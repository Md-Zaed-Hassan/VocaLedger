package com.example.voicefinance;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import android.view.animation.Animation;
import android.view.animation.ScaleAnimation;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.voicefinance.databinding.ActivityMainBinding;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppDatabase db;

    private String currentYear;
    private String currentMonth;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    private static final String PREFS_NAME = "FinancialAdvicePrefs";
    private static final String LAST_ADVICE_TIMESTAMP = "last_advice_timestamp";
    private static final String LAST_ADVICE_INDEX = "last_advice_index";
    private static final long ADVICE_INTERVAL = 3600 * 1000; // 1 hour

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        db = AppDatabase.getDatabase(this);

        Calendar cal = Calendar.getInstance();
        currentYear = String.valueOf(cal.get(Calendar.YEAR));
        currentMonth = String.format(Locale.getDefault(), "%02d", cal.get(Calendar.MONTH) + 1);

        observeMonthlySummary();
        registerLaunchers();
        setupListeners();
        observeData();
        observeMonthlyBudget();
    }

    // --------------------------------------------------
    // MENU
    // --------------------------------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {

        if (item.getItemId() == R.id.action_history) {
            startActivity(new Intent(this, HistoryActivity.class));
            return true;
        }

        if (item.getItemId() == R.id.action_change_theme) {
            showThemeChoiceDialog();
            return true;
        }

        if (item.getItemId() == R.id.action_set_budget) {
            showSetBudgetDialog();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    // --------------------------------------------------
    // DASHBOARD
    // --------------------------------------------------
    private void observeData() {
        db.transactionDao()
                .getAllTransactionsOrdered()
                .observe(this, this::updateDashboard);
    }

    private void updateDashboard(List<Transaction> transactions) {

        double totalBalance = 0;
        double totalIncome = 0;
        double totalExpense = 0;

        for (Transaction t : transactions) {
            totalBalance += t.amount;
            if (t.amount > 0) totalIncome += t.amount;
            else totalExpense += t.amount;
        }

        binding.balanceAmount.setText(CurrencyUtils.getCurrencyInstance().format(totalBalance));
        binding.incomeAmount.setText(CurrencyUtils.getCurrencyInstance().format(totalIncome));
        binding.expenseAmount.setText(CurrencyUtils.getCurrencyInstance().format(totalExpense));
    }

    // --------------------------------------------------
    // THEME
    // --------------------------------------------------
    private void showThemeChoiceDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Choose Theme");

        int currentTheme = ThemeHelper.getSavedTheme(this);
        String[] themes = {"Light", "Dark (Neon)", "System Default"};

        builder.setSingleChoiceItems(themes, currentTheme, (dialog, which) -> {
            ThemeHelper.setTheme(this, which);
            dialog.dismiss();
            recreate();
        });

        builder.show();
    }

    // --------------------------------------------------
    // VOICE INPUT
    // --------------------------------------------------
    private void registerLaunchers() {

        requestPermissionLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.RequestPermission(),
                        granted -> {
                            if (granted) startSpeechToText();
                            else showToast("Microphone permission is required.");
                        });

        speechRecognizerLauncher =
                registerForActivityResult(
                        new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                ArrayList<String> results =
                                        result.getData().getStringArrayListExtra(
                                                RecognizerIntent.EXTRA_RESULTS);
                                if (results != null && !results.isEmpty()) {
                                    parseSpeechAndSave(results.get(0));
                                }
                            }
                        });
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE_MODEL,
                RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(
                RecognizerIntent.EXTRA_LANGUAGE,
                Locale.getDefault());
        intent.putExtra(
                RecognizerIntent.EXTRA_PROMPT,
                "Speak your transaction");

        speechRecognizerLauncher.launch(intent);
    }

    private void parseSpeechAndSave(String text) {

        text = text.toLowerCase(Locale.getDefault());
        boolean isExpense = !(text.contains("income") || text.contains("got"));

        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(text);

        if (!matcher.find()) {
            showToast("Amount not detected");
            return;
        }

        double amount = Double.parseDouble(matcher.group(1));
        if (isExpense) amount = -amount;

        String label = text.replaceAll(matcher.group(1), "")
                .replaceAll("[^a-z ]", "")
                .trim();

        if (label.isEmpty()) {
            label = isExpense ? "Expense" : "Income";
        } else {
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
        }

        saveTransaction(new Transaction(
                label,
                amount,
                System.currentTimeMillis(),
                label,
                isExpense ? TransactionType.EXPENSE : TransactionType.INCOME
        ));
    }

    // --------------------------------------------------
    // MANUAL INPUT
    // --------------------------------------------------
    private void setupListeners() {

        binding.addIncomeButton.setOnClickListener(v ->
                handleManualTransaction(false));

        binding.addExpenseButton.setOnClickListener(v ->
                handleManualTransaction(true));

        binding.micButton.setOnClickListener(v -> {
            animateMicClick();
            if (ContextCompat.checkSelfPermission(
                    this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                requestPermissionLauncher.launch(
                        Manifest.permission.RECORD_AUDIO);
            }
        });

        binding.statsButton.setOnClickListener(v ->
                startActivity(new Intent(this, StatisticsActivity.class)));
    }

    private void handleManualTransaction(boolean isExpense) {

        String amountStr = binding.editTextAmount.getText().toString();
        String label = binding.editTextLabel.getText().toString();

        if (TextUtils.isEmpty(amountStr)) {
            showToast("Enter amount");
            return;
        }

        double amount = Double.parseDouble(amountStr);
        if (isExpense) amount = -amount;

        if (TextUtils.isEmpty(label)) {
            label = isExpense ? "Expense" : "Income";
        }

        saveTransaction(new Transaction(
                label,
                amount,
                System.currentTimeMillis(),
                label,
                isExpense ? TransactionType.EXPENSE : TransactionType.INCOME
        ));

        binding.editTextAmount.setText("");
        binding.editTextLabel.setText("");
    }

    private void saveTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.transactionDao().insert(transaction);
            showToast("Saved");
        });
    }

    // --------------------------------------------------
    // MONTHLY SUMMARY
    // --------------------------------------------------
    private void observeMonthlySummary() {
        db.transactionDao()
                .getMonthlyIncome(currentYear, currentMonth)
                .observe(this, i -> updateMonthlyDashboard());

        db.transactionDao()
                .getMonthlyExpense(currentYear, currentMonth)
                .observe(this, e -> updateMonthlyDashboard());
    }

    private void updateMonthlyDashboard() {

        db.transactionDao()
                .getMonthlyIncome(currentYear, currentMonth)
                .observe(this, income -> {

                    db.transactionDao()
                            .getMonthlyExpense(currentYear, currentMonth)
                            .observe(this, expense -> {

                                double in = income != null ? income : 0;
                                double out = expense != null ? expense : 0;
                                double bal = in + out;

                                binding.incomeAmount.setText(
                                        CurrencyUtils.getCurrencyInstance().format(in));
                                binding.expenseAmount.setText(
                                        CurrencyUtils.getCurrencyInstance().format(out));
                                binding.balanceAmount.setText(
                                        CurrencyUtils.getCurrencyInstance().format(bal));
                            });
                });
    }

    // --------------------------------------------------
    // BUDGET
    // --------------------------------------------------
    private void observeMonthlyBudget() {

        double budget = BudgetHelper.getMonthlyBudget(this);
        if (budget <= 0) return;

        db.transactionDao()
                .getCurrentMonthExpense()
                .observe(this, expense -> {

                    if (expense == null) return;

                    double used = Math.abs(expense);
                    double percent = (used / budget) * 100;

                    if (percent >= 100)
                        binding.budgetStatusText.setText("⚠ Budget exceeded");
                    else
                        binding.budgetStatusText.setText(
                                String.format(
                                        Locale.getDefault(),
                                        "Budget usage: %.0f%%",
                                        percent));
                });
    }

    // --------------------------------------------------
    // UI HELPERS
    // --------------------------------------------------
    private void animateMicClick() {

        ScaleAnimation anim =
                new ScaleAnimation(
                        1f, 1.2f, 1f, 1.2f,
                        Animation.RELATIVE_TO_SELF, 0.5f,
                        Animation.RELATIVE_TO_SELF, 0.5f);

        anim.setDuration(120);
        anim.setRepeatMode(Animation.REVERSE);
        anim.setRepeatCount(1);
        binding.micButton.startAnimation(anim);
    }

    private void showSetBudgetDialog() {

        EditText input = new EditText(this);
        input.setHint("Monthly budget");

        new AlertDialog.Builder(this)
                .setTitle("Set Budget")
                .setView(input)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        double v = Double.parseDouble(input.getText().toString());
                        BudgetHelper.setMonthlyBudget(this, v);
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showToast(String msg) {
        runOnUiThread(() ->
                Toast.makeText(this, msg, Toast.LENGTH_SHORT).show());
    }
}
