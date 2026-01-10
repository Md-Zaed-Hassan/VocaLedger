package com.example.voicefinance;

import android.os.Bundle;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.google.android.material.appbar.MaterialToolbar;

public class CategoryDetailActivity extends AppCompatActivity {

    private AppDatabase db;
    private RecyclerView recyclerView;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {

        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_category_detail);

        // -------------------------------
        // Toolbar setup (REQUIRED)
        // -------------------------------
        MaterialToolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // -------------------------------
        // Read intent data safely
        // -------------------------------
        String category = getIntent().getStringExtra("category");
        String year = getIntent().getStringExtra("year");
        String month = getIntent().getStringExtra("month");

        // Defensive fallback (prevents crash)
        if (category == null) {
            finish();
            return;
        }

        // Update toolbar title
        if (getSupportActionBar() != null) {
            getSupportActionBar().setTitle(category);
        }

        // -------------------------------
        // RecyclerView setup
        // -------------------------------
        recyclerView = findViewById(R.id.recyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        // -------------------------------
        // Database
        // -------------------------------
        db = AppDatabase.getDatabase(this);

        // If year and month are provided, show only that month
        // Otherwise, show full category history
        if (year != null && month != null) {
            observeCategoryData(category, year, month);
        } else {
            observeFullCategoryHistory(category);
        }
    }

    // -------------------------------
    // Back navigation
    // -------------------------------
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // -------------------------------
    // Observe category transactions (single month)
    // -------------------------------
    private void observeCategoryData(String category, String year, String month) {

        db.transactionDao()
                .getTransactionsByCategoryAndMonth(category, year, month)
                .observe(this, transactions -> {

                    // 🔒 CRASH PREVENTION
                    if (transactions == null) return;

                    TransactionAdapter adapter =
                            new TransactionAdapter(transactions, null);
                    recyclerView.setAdapter(adapter);
                });
    }

    // -------------------------------
    // Observe full category history
    // -------------------------------
    private void observeFullCategoryHistory(String category) {

        db.transactionDao()
                .getTransactionsByCategory(category)
                .observe(this, transactions -> {

                    // 🔒 CRASH PREVENTION
                    if (transactions == null) return;

                    TransactionAdapter adapter =
                            new TransactionAdapter(transactions, null);
                    recyclerView.setAdapter(adapter);
                });
    }
}
