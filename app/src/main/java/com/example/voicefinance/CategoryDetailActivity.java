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
        if (category == null || year == null || month == null) {
            finish();
            return;
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

        observeCategoryData(category, year, month);
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
    // Observe category transactions
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
}
