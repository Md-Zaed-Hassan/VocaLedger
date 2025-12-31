package com.example.voicefinance;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;

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

    // ✅ REQUIRED FOR PHASE E (Category Drill-Down)
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

        // ✅ Initialize current month/year safely
        updateSelectedMonthYear();

        setupPieChart();
        setupTimePeriodSelector();
        setupChartClick();
    }

    // -------------------------------
    // Navigation
    // -------------------------------
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // Deprecated but safe here
        return true;
    }

    // -------------------------------
    // Pie Chart Setup
    // -------------------------------
    private void setupPieChart() {

        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setExtraOffsets(5, 10, 5, 5);
        binding.pieChart.setDragDecelerationFrictionCoef(0.95f);

        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);
        binding.pieChart.setHoleRadius(58f);
        binding.pieChart.setTransparentCircleRadius(61f);
        binding.pieChart.setDrawCenterText(true);

        binding.pieChart.setRotationEnabled(true);
        binding.pieChart.setHighlightPerTapEnabled(true);
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.getLegend().setEnabled(false);

        CustomMarkerView markerView =
                new CustomMarkerView(this, R.layout.marker_view);
        binding.pieChart.setMarker(markerView);

        binding.pieChart.animateY(1400, Easing.EaseInOutQuad);
    }

    // -------------------------------
    // Tap → Category Drill-Down
    // -------------------------------
    private void setupChartClick() {

        binding.pieChart.setOnChartValueSelectedListener(
                new OnChartValueSelectedListener() {

                    @Override
                    public void onValueSelected(Entry e, Highlight h) {

                        if (!(e instanceof PieEntry)) return;

                        String category = ((PieEntry) e).getLabel();

                        Intent intent = new Intent(
                                StatisticsActivity.this,
                                CategoryDetailActivity.class
                        );
                        intent.putExtra("category", category);
                        intent.putExtra("year", selectedYear);
                        intent.putExtra("month", selectedMonth);

                        startActivity(intent);
                    }

                    @Override
                    public void onNothingSelected() {}
                }
        );
    }

    // -------------------------------
    // Period Selector (Daily / Monthly / Yearly)
    // -------------------------------
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

        liveData.observe(this, categoryTotals -> {

            if (categoryTotals != null && !categoryTotals.isEmpty()) {
                loadPieChartData(categoryTotals);
            } else {
                binding.pieChart.clear();
                binding.pieChart.setCenterText(
                        generateCenterSpannableText(0)
                );
                binding.pieChart.invalidate();
            }
        });
    }

    // -------------------------------
    // Helpers
    // -------------------------------
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
        selectedMonth = String.format(
                "%02d",
                cal.get(Calendar.MONTH) + 1
        );
    }

    private void loadPieChartData(
            List<TransactionDao.CategoryTotal> categoryTotals) {

        ArrayList<PieEntry> entries = new ArrayList<>();
        double totalExpense = 0;

        for (TransactionDao.CategoryTotal total : categoryTotals) {
            entries.add(
                    new PieEntry(
                            (float) Math.abs(total.total),
                            total.category
                    )
            );
            totalExpense += total.total;
        }

        PieDataSet dataSet =
                new PieDataSet(entries, "Expense Categories");

        dataSet.setSliceSpace(3f);
        dataSet.setColors(ColorTemplate.MATERIAL_COLORS);
        dataSet.setDrawValues(true);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(
                new PercentFormatter(binding.pieChart)
        );
        data.setValueTextSize(12f);

        TypedValue tv = new TypedValue();
        getTheme().resolveAttribute(
                com.google.android.material.R.attr.colorOnSurface,
                tv,
                true
        );

        data.setValueTextColor(tv.data);
        binding.pieChart.setCenterTextColor(tv.data);
        binding.pieChart.setData(data);
        binding.pieChart.invalidate();

        binding.pieChart.setCenterText(
                generateCenterSpannableText(totalExpense)
        );
    }

    private SpannableString generateCenterSpannableText(
            double totalExpense) {

        if (totalExpense == 0) {
            return new SpannableString("No Expenses");
        }

        String amount =
                NumberFormat.getCurrencyInstance()
                        .format(Math.abs(totalExpense));

        String text = amount + "\nTotal Expenses";
        SpannableString s = new SpannableString(text);

        s.setSpan(
                new RelativeSizeSpan(1.7f),
                0,
                amount.length(),
                0
        );
        s.setSpan(
                new StyleSpan(android.graphics.Typeface.BOLD),
                0,
                amount.length(),
                0
        );
        return s;
    }
}
