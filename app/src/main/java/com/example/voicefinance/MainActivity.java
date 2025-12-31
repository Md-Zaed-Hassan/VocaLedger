package com.example.voicefinance;

import android.Manifest;
import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.content.Context;import android.content.Intent;
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
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;

import com.example.voicefinance.databinding.ActivityMainBinding;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Random;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Calendar;


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
        currentMonth = String.format("%02d", cal.get(Calendar.MONTH) + 1);
        observeMonthlySummary();
        registerLaunchers();
        setupListeners();
        observeData();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.action_change_theme) {
            showThemeChoiceDialog();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

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

        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void registerLaunchers() {
        requestPermissionLauncher = registerForActivityResult(new ActivityResultContracts.RequestPermission(), isGranted -> {
            if (isGranted) {
                startSpeechToText();
            } else {
                showToast("Microphone permission is required for voice entry.");
            }
        });

        speechRecognizerLauncher = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), result -> {
            if (result.getResultCode() == RESULT_OK && result.getData() != null) {
                ArrayList<String> results = result.getData().getStringArrayListExtra(RecognizerIntent.EXTRA_RESULTS);
                if (results != null && !results.isEmpty()) {
                    String spokenText = results.get(0);
                    parseSpeechAndSave(spokenText);
                }
            }
        });
    }

    private void setupListeners() {
        binding.modeSwitch.setOnCheckedChangeListener((buttonView, isChecked) -> {
            animateManualCard(isChecked);
        });

        binding.addIncomeButton.setOnClickListener(v -> handleManualTransaction(false));
        binding.addExpenseButton.setOnClickListener(v -> handleManualTransaction(true));

        binding.micButton.setOnClickListener(v -> {
            animateMicClick();
            if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECORD_AUDIO) == PackageManager.PERMISSION_GRANTED) {
                startSpeechToText();
            } else {
                requestPermissionLauncher.launch(Manifest.permission.RECORD_AUDIO);
            }
        });

        binding.adviceButton.setOnClickListener(v -> showAdviceDialog());

        binding.statsButton.setOnClickListener(v -> {
            Intent intent = new Intent(MainActivity.this, StatisticsActivity.class);
            startActivity(intent);
        });
    }

    private void animateMicClick() {
        ScaleAnimation scaleAnim = new ScaleAnimation(
                1.0f, 1.2f, 1.0f, 1.2f,
                Animation.RELATIVE_TO_SELF, 0.5f,
                Animation.RELATIVE_TO_SELF, 0.5f
        );
        scaleAnim.setDuration(100);
        scaleAnim.setInterpolator(new AccelerateDecelerateInterpolator());
        scaleAnim.setRepeatMode(Animation.REVERSE);
        scaleAnim.setRepeatCount(1);
        binding.micButton.startAnimation(scaleAnim);
    }

    private void animateManualCard(boolean isShowing) {
        if (isShowing) {
            binding.manualEntryCard.setVisibility(View.VISIBLE);
            binding.manualEntryCard.setAlpha(0f);
            binding.manualEntryCard.setTranslationY(-50f);

            binding.manualEntryCard.animate()
                    .alpha(1.0f)
                    .translationY(0)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(null);
            binding.modeSwitch.setText("Manual Mode");
        } else {
            binding.manualEntryCard.animate()
                    .alpha(0f)
                    .translationY(-50f)
                    .setDuration(300)
                    .setInterpolator(new AccelerateDecelerateInterpolator())
                    .setListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            binding.manualEntryCard.setVisibility(View.GONE);
                        }
                    });
            binding.modeSwitch.setText("Voice Mode");
        }
    }

    private void startSpeechToText() {
        Intent intent = new Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM);
        intent.putExtra(RecognizerIntent.EXTRA_LANGUAGE, Locale.getDefault());
        intent.putExtra(RecognizerIntent.EXTRA_PROMPT, "Speak your transaction...");

        try {
            speechRecognizerLauncher.launch(intent);
        } catch (Exception e) {
            showToast("Your device doesn\\'t support speech recognition.");
        }
    }

    private void parseSpeechAndSave(String text) {
        text = text.toLowerCase();
        boolean isExpense = true;

        if (text.contains("income") || text.contains("add") || text.contains("got")) {
            isExpense = false;
        } else if (text.contains("expense") || text.contains("spent") || text.contains("cost")) {
            isExpense = true;
        }

        Pattern pattern = Pattern.compile("(\\d+(\\.\\d+)?)");
        Matcher matcher = pattern.matcher(text);

        double amount = 0.0;
        String amountString = "";
        if (matcher.find()) {
            try {
                amountString = matcher.group(1);
                amount = Double.parseDouble(amountString);
            } catch (NumberFormatException e) {
                showToast("Could not understand the amount.");
                return;
            }
        }

        if (amount == 0.0) {
            showToast("I heard: '" + text + "'. Could not find an amount.");
            return;
        }

        String label = text.replaceAll("income", "")
                .replaceAll("expense", "")
                .replaceAll("add", "")
                .replaceAll("got", "")
                .replaceAll("spent", "")
                .replaceAll("cost", "")
                .replaceAll("dollars", "")
                .replaceAll("dollar", "")
                .replaceAll("on", "")
                .replaceAll("for", "")
                .replaceAll("at", "")
                .replaceAll(amountString, "")
                .replaceAll("[^a-zA-Z ]", "")
                .trim();

        if (label.isEmpty()) {
            label = isExpense ? "Expense" : "Income";
        } else {
            label = label.substring(0, 1).toUpperCase() + label.substring(1);
        }

        handleVoiceTransaction(label, amount, isExpense);
    }

    private void observeData() {
        db.transactionDao().getAllTransactions().observe(this, transactions -> {
            if (transactions != null) {
                updateDashboard(transactions);
            }
        });
    }

    private void updateDashboard(List<Transaction> transactions) {
        double totalBalance = 0.0;
        double totalIncome = 0.0;
        double totalExpense = 0.0;

        for (Transaction transaction : transactions) {
            totalBalance += transaction.amount;
            if (transaction.amount > 0) {
                totalIncome += transaction.amount;
            } else {
                totalExpense += transaction.amount;
            }
        }

        binding.balanceAmount.setText(formatCurrency(totalBalance));
        binding.incomeAmount.setText(formatCurrency(totalIncome));
        binding.expenseAmount.setText(formatCurrency(totalExpense));
    }

    private String formatCurrency(double amount) {
        return NumberFormat.getCurrencyInstance().format(amount);
    }

    private void showAdviceDialog() {
        SharedPreferences prefs = getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE);
        long lastTimestamp = prefs.getLong(LAST_ADVICE_TIMESTAMP, 0);
        int lastIndex = prefs.getInt(LAST_ADVICE_INDEX, -1);

        long currentTime = System.currentTimeMillis();
        String[] adviceArray = getResources().getStringArray(R.array.financial_advice);

        int adviceIndex;

        if (currentTime - lastTimestamp > ADVICE_INTERVAL) {
            Random random = new Random();
            adviceIndex = random.nextInt(adviceArray.length);

            if (adviceArray.length > 1 && adviceIndex == lastIndex) {
                adviceIndex = (adviceIndex + 1) % adviceArray.length;
            }

            SharedPreferences.Editor editor = prefs.edit();
            editor.putLong(LAST_ADVICE_TIMESTAMP, currentTime);
            editor.putInt(LAST_ADVICE_INDEX, adviceIndex);
            editor.apply();
        } else {
            adviceIndex = lastIndex;
            if (adviceIndex == -1) {
                Random random = new Random();
                adviceIndex = random.nextInt(adviceArray.length);
                SharedPreferences.Editor editor = prefs.edit();
                editor.putLong(LAST_ADVICE_TIMESTAMP, currentTime);
                editor.putInt(LAST_ADVICE_INDEX, adviceIndex);
                editor.apply();
            }
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("Financial Advice");
        builder.setMessage(adviceArray[adviceIndex]);
        builder.setPositiveButton("OK", (dialog, which) -> dialog.dismiss());
        AlertDialog dialog = builder.create();
        dialog.show();
    }

    private void handleManualTransaction(boolean isExpense) {
        String amountStr = binding.editTextAmount.getText().toString();
        String label = binding.editTextLabel.getText().toString();

        if (TextUtils.isEmpty(amountStr)) {
            showToast("Please enter an amount");
            return;
        }

        if (TextUtils.isEmpty(label)) {
            label = isExpense ? "Expense" : "Income";
        }

        double amount;
        try {
            amount = Double.parseDouble(amountStr);
        } catch (NumberFormatException e) {
            showToast("Please enter a valid number");
            return;
        }

        if (isExpense) {
            amount = amount * -1;
        }

        saveTransaction(new Transaction(label, amount, System.currentTimeMillis(), label, isExpense ? TransactionType.EXPENSE : TransactionType.INCOME));

        binding.editTextAmount.setText("");
        binding.editTextLabel.setText("");
    }

    private void handleVoiceTransaction(String label, double amount, boolean isExpense) {
        if (isExpense) {
            amount = amount * -1;
        }
        saveTransaction(new Transaction(label, amount, System.currentTimeMillis(), label, isExpense ? TransactionType.EXPENSE : TransactionType.INCOME));
    }

    private void saveTransaction(Transaction transaction) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            db.transactionDao().insert(transaction);

            String type = transaction.amount > 0 ? "Income" : "Expense";
            showToast(type + " saved!");
        });
    }

    private void showToast(String message) {
        runOnUiThread(() -> {
            Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
        });
    }
    private void observeMonthlySummary() {

        db.transactionDao()
                .getMonthlyIncome(currentYear, currentMonth)
                .observe(this, income -> updateMonthlyDashboard());

        db.transactionDao()
                .getMonthlyExpense(currentYear, currentMonth)
                .observe(this, expense -> updateMonthlyDashboard());
    }

    private void updateMonthlyDashboard() {

        db.transactionDao()
                .getMonthlyIncome(currentYear, currentMonth)
                .observe(this, income -> {

                    db.transactionDao()
                            .getMonthlyExpense(currentYear, currentMonth)
                            .observe(this, expense -> {

                                double totalIncome = income != null ? income : 0.0;
                                double totalExpense = expense != null ? expense : 0.0;
                                double balance = totalIncome + totalExpense;

                                binding.incomeAmount.setText(formatCurrency(totalIncome));
                                binding.expenseAmount.setText(formatCurrency(totalExpense));
                                binding.balanceAmount.setText(formatCurrency(balance));
                            });
                });
    }

}