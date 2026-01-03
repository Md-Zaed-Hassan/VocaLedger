package com.example.voicefinance;
import android.app.DatePickerDialog;
import android.widget.Button;
import android.widget.LinearLayout;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import android.os.Bundle;
import android.text.InputType;
import android.widget.EditText;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.voicefinance.databinding.ActivityHistoryBinding;

import java.util.List;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);

        // ✅ SAFE navigation handling
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = AppDatabase.getDatabase(this);

        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        db.transactionDao()
                .getAllTransactions()
                .observe(this, transactions -> {

                    List<HistoryListItem> items =
                            HistoryUtils.buildHistoryItems(transactions);

                    binding.recyclerView.setAdapter(
                            new HistoryAdapter(
                                    items,
                                    this::showTransactionOptions
                            )
                    );
                });

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

        // Container layout
        LinearLayout layout = new LinearLayout(this);
        layout.setOrientation(LinearLayout.VERTICAL);
        layout.setPadding(40, 20, 40, 10);

        // -------------------------
        // Amount input
        // -------------------------
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

        // -------------------------
        // Date selector button
        // -------------------------
        Button dateButton = new Button(this);
        dateButton.setText(getFormattedDate(transaction.timestamp));
        layout.addView(dateButton);

        final Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(transaction.timestamp);

        dateButton.setOnClickListener(v -> {
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
            ).show();
        });

        // -------------------------
        // Dialog
        // -------------------------
        new AlertDialog.Builder(this)
                .setTitle("Edit Transaction")
                .setView(layout)
                .setPositiveButton("Save", (d, w) -> {
                    try {
                        double value =
                                Double.parseDouble(
                                        amountInput.getText().toString()
                                );

                        // Preserve income / expense sign
                        transaction.amount =
                                transaction.amount < 0 ? -value : value;

                        // Update date
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
    // DELETE (CONFIRMED)
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
