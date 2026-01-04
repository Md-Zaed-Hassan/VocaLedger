package com.example.voicefinance;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.text.InputType;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.RadioGroup;
import android.widget.Spinner;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.voicefinance.databinding.ActivityHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private AppDatabase db;

    private HistoryFilterType currentFilter = HistoryFilterType.DAY;
    private List<Transaction> cachedTransactions;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = AppDatabase.getDatabase(this);

        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // Observe data ONCE
        db.transactionDao()
                .getAllTransactionsOrdered()
                .observe(this, transactions -> {
                    cachedTransactions = transactions;
                    refreshList();
                });

        // RadioGroup filter handling
        binding.filterGroup.setOnCheckedChangeListener(
                (RadioGroup group, int checkedId) -> {

                    if (checkedId == R.id.filter_year) {
                        currentFilter = HistoryFilterType.YEAR;
                    } else if (checkedId == R.id.filter_month) {
                        currentFilter = HistoryFilterType.MONTH;
                    } else {
                        currentFilter = HistoryFilterType.DAY;
                    }

                    refreshList();
                }
        );
    }

    // -----------------------------
    // REFRESH LIST
    // -----------------------------
    private void refreshList() {
        if (cachedTransactions == null) return;

        binding.recyclerView.setAdapter(
                new HistoryAdapter(
                        HistoryUtils.buildHistoryItems(
                                cachedTransactions,
                                currentFilter
                        ),
                        this::showTransactionOptions
                )
        );
    }

    // -----------------------------
    // LONG PRESS MENU
    // -----------------------------
    private void showTransactionOptions(Transaction transaction) {

        new AlertDialog.Builder(this)
                .setTitle("Transaction Options")
                .setItems(
                        new String[]{"Edit", "Delete"},
                        (dialog, which) -> {
                            if (which == 0) {
                                showEditDialog(transaction);
                            } else {
                                confirmDelete(transaction);
                            }
                        }
                )
                .show();
    }

    // -----------------------------
    // EDIT TRANSACTION
    // -----------------------------
    private void showEditDialog(Transaction transaction) {

        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        // Label
        EditText labelInput = new EditText(this);
        labelInput.setHint("Label");
        labelInput.setText(transaction.label);
        layout.addView(labelInput);

        // Amount
        EditText amountInput = new EditText(this);
        amountInput.setInputType(
                InputType.TYPE_CLASS_NUMBER |
                        InputType.TYPE_NUMBER_FLAG_DECIMAL
        );
        amountInput.setHint("Amount");
        amountInput.setText(
                String.valueOf(Math.abs(transaction.amount))
        );
        layout.addView(amountInput);

        // Category
        Spinner categorySpinner = new Spinner(this);
        String[] categories = {
                "Food", "Transportation", "Snacks",
                "Bills", "Shopping", "Other"
        };

        ArrayAdapter<String> adapter =
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        categories
                );
        categorySpinner.setAdapter(adapter);

        for (int i = 0; i < categories.length; i++) {
            if (categories[i].equals(transaction.category)) {
                categorySpinner.setSelection(i);
                break;
            }
        }
        layout.addView(categorySpinner);

        // Date picker
        Button dateButton = new Button(this);
        dateButton.setText(getFormattedDate(transaction.timestamp));
        layout.addView(dateButton);

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(transaction.timestamp);

        dateButton.setOnClickListener(v ->
                new DatePickerDialog(
                        this,
                        (view, year, month, day) -> {
                            calendar.set(year, month, day);
                            dateButton.setText(
                                    getFormattedDate(calendar.getTimeInMillis())
                            );
                        },
                        calendar.get(Calendar.YEAR),
                        calendar.get(Calendar.MONTH),
                        calendar.get(Calendar.DAY_OF_MONTH)
                ).show()
        );

        new AlertDialog.Builder(this)
                .setTitle("Edit Transaction")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        double value =
                                Double.parseDouble(
                                        amountInput.getText().toString()
                                );

                        transaction.label =
                                labelInput.getText().toString().trim();

                        transaction.category =
                                categorySpinner.getSelectedItem().toString();

                        transaction.amount =
                                transaction.amount < 0 ? -value : value;

                        transaction.timestamp =
                                calendar.getTimeInMillis();

                        transaction.updatedAt =
                                System.currentTimeMillis();

                        AppDatabase.databaseWriteExecutor.execute(() ->
                                db.transactionDao().update(transaction)
                        );

                    } catch (Exception ignored) {}
                })
                .setNegativeButton("Cancel", null)
                .show();
    }

    private String getFormattedDate(long millis) {
        return new SimpleDateFormat(
                "MMM d, yyyy",
                Locale.getDefault()
        ).format(new Date(millis));
    }

    // -----------------------------
    // DELETE CONFIRMATION
    // -----------------------------
    private void confirmDelete(Transaction transaction) {

        new AlertDialog.Builder(this)
                .setTitle("Delete Transaction")
                .setMessage("Are you sure?")
                .setPositiveButton("Delete", (d, w) ->
                        AppDatabase.databaseWriteExecutor.execute(() ->
                                db.transactionDao().delete(transaction)
                        )
                )
                .setNegativeButton("Cancel", null)
                .show();
    }
}
