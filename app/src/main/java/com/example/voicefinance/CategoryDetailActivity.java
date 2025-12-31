package com.example.voicefinance;

import android.os.Bundle;

import androidx.appcompat.app.AppCompatActivity;

import com.example.voicefinance.databinding.ActivityCategoryDetailBinding;

public class CategoryDetailActivity extends AppCompatActivity {

    private ActivityCategoryDetailBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityCategoryDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        db = AppDatabase.getDatabase(this);

        String category = getIntent().getStringExtra("category");
        String year = getIntent().getStringExtra("year");
        String month = getIntent().getStringExtra("month");

        binding.categoryTitle.setText(category);

        db.transactionDao()
                .getTransactionsByCategoryAndMonth(category, year, month)
                .observe(this, transactions -> {
                    binding.recyclerView.setAdapter(
                            new TransactionAdapter(transactions)
                    );
                });
    }

    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }
}
