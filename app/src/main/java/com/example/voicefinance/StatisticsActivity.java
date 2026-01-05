package com.example.voicefinance;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.example.voicefinance.databinding.ActivityStatisticsBinding;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    private ActivityStatisticsBinding binding;
    private AppDatabase db;

    private String selectedYear;
    private String selectedMonth;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        db = AppDatabase.getDatabase(this);

        updateSelectedMonthYear();

        setupPieChart();
        setupTimePeriodSelector();
        setupChartClick();
    }

    // --------------------------------------------------
    // Navigation
    // --------------------------------------------------
    @Override
    public boolean onSupportNavigateUp() {
        finish();
        return true;
    }

    // --------------------------------------------------
    // Pie Chart Setup
    // --------------------------------------------------
    private void setupPieChart() {

        boolean isDark =
                (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setExtraOffsets(5, 10, 5, 5);
        binding.pieChart.setDragDecelerationFrictionCoef(0.95f);

        binding.pieChart.setDrawHoleEnabled(true);

        // ✅ FIX: Theme-aware hole color (THIS solves your issue)
        binding.pieChart.setHoleColor(
                isDark ? Color.parseColor("#1E1E1E") : Color.parseColor("#F2F2F2")
        );

        binding.pieChart.setHoleRadius(58f);
        binding.pieChart.setTransparentCircleRadius(61f);
        binding.pieChart.setDrawCenterText(true);

        binding.pieChart.setRotationEnabled(true);
        binding.pieChart.setHighlightPerTapEnabled(true);
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.getLegend().setEnabled(false);

        binding.pieChart.animateY(1200, Easing.EaseInOutQuad);
    }

    // --------------------------------------------------
    // Chart Click → Category Details
    // --------------------------------------------------
    private void setupChartClick() {

        binding.pieChart.setOnChartValueSelectedListener(
                new OnChartValueSelectedListener() {

                    @Override
                    public void onValueSelected(Entry e, Highlight h) {
                        if (!(e instanceof PieEntry)) return;

                        Intent intent = new Intent(
                                StatisticsActivity.this,
                                CategoryDetailActivity.class
                        );
                        intent.putExtra("category", ((PieEntry) e).getLabel());
                        intent.putExtra("year", selectedYear);
                        intent.putExtra("month", selectedMonth);
                        startActivity(intent);
                    }

                    @Override
                    public void onNothingSelected() {}
                }
        );
    }

    // --------------------------------------------------
    // Period Selector
    // --------------------------------------------------
    private void setupTimePeriodSelector() {

        binding.toggleGroup.setOnCheckedChangeListener(
                (group, checkedId) -> observeDataForPeriod(checkedId)
        );

        observeDataForPeriod(
                binding.toggleGroup.getCheckedRadioButtonId()
        );
    }

    private void observeDataForPeriod(int checkedId) {

        long startTime = getStartTimeForPeriod(checkedId);

        LiveData<List<TransactionDao.CategoryTotal>> liveData =
                db.transactionDao()
                        .getExpenseTotalsByCategorySince(startTime);

        if (checkedId == R.id.radio_daily) {
            binding.chartTitle.setText("Daily Expense Breakdown");
        } else if (checkedId == R.id.radio_yearly) {
            binding.chartTitle.setText("Yearly Expense Breakdown");
        } else {
            binding.chartTitle.setText("Monthly Expense Breakdown");
        }

        liveData.observe(this, totals -> {
            if (totals == null || totals.isEmpty()) {
                showEmptyChart();
            } else {
                loadPieChartData(totals);
            }
        });
    }

    // --------------------------------------------------
    // Empty Chart (Readable in both themes)
    // --------------------------------------------------
    private void showEmptyChart() {

        boolean isDark =
                (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        PieDataSet set = new PieDataSet(new ArrayList<>(), "");
        set.setDrawValues(false);
        set.setColor(Color.GRAY);

        binding.pieChart.setData(new PieData(set));
        binding.pieChart.setCenterText("No expense data");
        binding.pieChart.setCenterTextSize(14f);
        binding.pieChart.setCenterTextTypeface(Typeface.DEFAULT_BOLD);
        binding.pieChart.setCenterTextColor(
                isDark ? Color.WHITE : Color.BLACK
        );

        binding.pieChart.invalidate();
    }

    // --------------------------------------------------
    // Data Loader
    // --------------------------------------------------
    private void loadPieChartData(List<TransactionDao.CategoryTotal> totals) {

        boolean isDark =
                (getResources().getConfiguration().uiMode
                        & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        ArrayList<PieEntry> entries = new ArrayList<>();
        double totalExpense = 0;

        for (TransactionDao.CategoryTotal t : totals) {
            entries.add(
                    new PieEntry((float) Math.abs(t.total), t.category)
            );
            totalExpense += t.total;
        }

        PieDataSet dataSet = new PieDataSet(entries, "");
        dataSet.setSliceSpace(3f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChart));
        data.setValueTextSize(12f);
        data.setValueTextColor(isDark ? Color.WHITE : Color.BLACK);

        binding.pieChart.setData(data);
        binding.pieChart.setCenterText(
                generateCenterSpannableText(totalExpense)
        );
        binding.pieChart.setCenterTextColor(
                isDark ? Color.WHITE : Color.BLACK
        );
        binding.pieChart.invalidate();
    }

    // --------------------------------------------------
    // Helpers
    // --------------------------------------------------
    private long getStartTimeForPeriod(int checkedId) {

        Calendar cal = Calendar.getInstance();

        if (checkedId == R.id.radio_daily) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        } else if (checkedId == R.id.radio_yearly) {
            cal.add(Calendar.YEAR, -1);
        } else {
            cal.add(Calendar.MONTH, -1);
        }

        return cal.getTimeInMillis();
    }

    private void updateSelectedMonthYear() {

        Calendar cal = Calendar.getInstance();
        selectedYear = String.valueOf(cal.get(Calendar.YEAR));
        selectedMonth = String.format("%02d", cal.get(Calendar.MONTH) + 1);
    }

    private SpannableString generateCenterSpannableText(double totalExpense) {

        String amount =
                CurrencyUtils.getCurrencyInstance()
                        .format(Math.abs(totalExpense));

        SpannableString s =
                new SpannableString(amount + "\nTotal Expenses");

        s.setSpan(
                new RelativeSizeSpan(1.7f),
                0,
                amount.length(),
                0
        );
        s.setSpan(
                new StyleSpan(Typeface.BOLD),
                0,
                amount.length(),
                0
        );
        return s;
    }
}
