package com.example.voicefinance;

import android.Manifest;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Bundle;
import android.speech.RecognizerIntent;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
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
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity {

    private ActivityMainBinding binding;
    private AppDatabase db;

    private ActivityResultLauncher<String> requestPermissionLauncher;
    private ActivityResultLauncher<Intent> speechRecognizerLauncher;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        setSupportActionBar(binding.toolbar);

        db = AppDatabase.getDatabase(this);

        registerLaunchers();
        setupListeners();
        observeDashboard();
        observeBudget();
    }

    // -------------------------------- MENU --------------------------------

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

    // ----------------------------- DASHBOARD ------------------------------

    private void observeDashboard() {
        db.transactionDao().getAllTransactionsOrdered()
                .observe(this, transactions -> {

                    double balance = 0;
                    double income = 0;
                    double expense = 0;

                    for (Transaction t : transactions) {
                        balance += t.amount;
                        if (t.amount > 0) income += t.amount;
                        else expense += t.amount;
                    }

                    binding.balanceAmount.setText(CurrencyUtils.getCurrencyInstance().format(balance));
                    binding.incomeAmount.setText(CurrencyUtils.getCurrencyInstance().format(income));
                    binding.expenseAmount.setText(CurrencyUtils.getCurrencyInstance().format(expense));
                });
    }

    // ----------------------------- VOICE ----------------------------------

    private void registerLaunchers() {

        requestPermissionLauncher = 
                registerForActivityResult(new ActivityResultContracts.RequestPermission(),
                        granted -> {
                            if (granted) startSpeech();
                            else toast("Microphone permission required");
                        });

        speechRecognizerLauncher = 
                registerForActivityResult(new ActivityResultContracts.StartActivityForResult(),
                        result -> {
                            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                                ArrayList<String> results =
                                        result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                                if (results != null && !results.isEmpty()) {
                                    parseSpeech(results.get(0));
                                }
                            }
                        });
    }

    private void startSpeech() {
        Intent i = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        i.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        speechRecognizerLauncher.launch(i);
    }

    private void parseSpeech(String text) {

        text = text.toLowerCase(Locale.getDefault());
        boolean isExpense = !(text.contains("income") || text.contains("got"));

        Matcher m = Pattern.compile("(\\d+(\\.\\d+)?)").matcher(text);
        if (!m.find()) {
            toast("Amount not found");
            return;
        }

        double amount = Double.parseDouble(m.group(1));
        if (isExpense) amount = -amount;

        String label = text.replace(m.group(1), "").replaceAll("[^a-z ]", "").trim();
        if (label.isEmpty()) label = isExpense ? "Expense" : "Income";

        save(new Transaction(label, amount, System.currentTimeMillis(), label,
                isExpense ? TransactionType.EXPENSE : TransactionType.INCOME));
    }

    // ---------------------------- MANUAL ----------------------------------

    private void setupListeners() {

        binding.addIncomeButton.setOnClickListener(v -> handleManual(false));
        binding.addExpenseButton.setOnClickListener(v -> handleManual(true));

        binding.micButton.setOnClickListener(v -> {
            animateMic();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO)
                    == PackageManager.PERMISSION_GRANTED)
                startSpeech();
            else requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
        });

        binding.statsButton.setOnClickListener(v -> 
                startActivity(new Intent(this, StatisticsActivity.class)));

        binding.adviceButton.setOnClickListener(v -> showAdviceDialog());

        binding.modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            binding.manualEntryCard.setVisibility(isChecked ? View.VISIBLE : View.GONE);
            binding.micButton.setVisibility(isChecked ? View.GONE : View.VISIBLE);
        });
    }

    private void handleManual(boolean isExpense) {

        String a = binding.editTextAmount.getText().toString();
        String l = binding.editTextLabel.getText().toString();

        if (TextUtils.isEmpty(a)) {
            toast("Enter amount");
            return;
        }

        double amount = Double.parseDouble(a);
        if (isExpense) amount = -amount;

        if (TextUtils.isEmpty(l)) l = isExpense ? "Expense" : "Income";

        save(new Transaction(l, amount, System.currentTimeMillis(), l,
                isExpense ? TransactionType.EXPENSE : TransactionType.INCOME));

        binding.editTextAmount.setText("");
        binding.editTextLabel.setText("");
    }

    private void save(Transaction t) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.transactionDao().insert(t);
            runOnUiThread(() -> toast("Saved"));
        });
    }

    // ----------------------------- BUDGET ---------------------------------

    private void observeBudget() {

        double budget = BudgetHelper.getMonthlyBudget(this);
        if (budget <= 0) {
            binding.budgetStatusText.setVisibility(View.GONE);
            return;
        }

        db.transactionDao().getCurrentMonthExpense()
                .observe(this, expense -> {
                    if (expense == null) return;
                    double used = Math.abs(expense);
                    double percent = (used / budget) * 100;

                    binding.budgetStatusText.setVisibility(View.VISIBLE);
                    if (percent >= 100)
                        binding.budgetStatusText.setText("⚠ Budget exceeded");
                    else
                        binding.budgetStatusText.setText(String.format(Locale.getDefault(),
                                "Budget usage: %.0f%%", percent));
                });
    }

    // ------------------------------ UI -----------------------------------

    private void animateMic() {
        ScaleAnimation a = new ScaleAnimation(1f, 1.2f, 1f, 1.2f,
                Animation.RELATIVE_TO_SELF, .5f,
                Animation.RELATIVE_TO_SELF, .5f);
        a.setDuration(120);
        a.setRepeatMode(Animation.REVERSE);
        a.setRepeatCount(1);
        binding.micButton.startAnimation(a);
    }

    private void showThemeChoiceDialog() {
        String[] themes = {"Light", "Dark", "System"};
        new AlertDialog.Builder(this)
                .setTitle("Choose Theme")
                .setSingleChoiceItems(themes, ThemeHelper.getSavedTheme(this),
                        (d, i) -> {
                            ThemeHelper.setTheme(this, i);
                            recreate();
                        }).show();
    }

    private void showSetBudgetDialog() {
        EditText e = new EditText(this);
        new AlertDialog.Builder(this)
                .setTitle("Set Budget")
                .setView(e)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        BudgetHelper.setMonthlyBudget(this,
                                Double.parseDouble(e.getText().toString()));
                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private void showAdviceDialog() {
        String[] advice = getResources().getStringArray(R.array.financial_advice);
        String randomAdvice = advice[new Random().nextInt(advice.length)];

        new AlertDialog.Builder(this)
                .setTitle("Financial Advice")
                .setMessage(randomAdvice)
                .setPositiveButton("OK", null)
                .show();
    }

    private void toast(String s) {
        Toast.makeText(this, s, Toast.LENGTH_SHORT).show();
    }
}
