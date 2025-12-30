package com.example.voicefinance;

import android.graphics.Color;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.TypedValue;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;
//import androidx.lifecycle.Observer;

import com.example.voicefinance.databinding.ActivityStatisticsBinding;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class StatisticsActivity extends AppCompatActivity {

    private ActivityStatisticsBinding binding;
    private AppDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // --- APPLY SAVED THEME ---
        ThemeHelper.applyTheme(this);

        super.onCreate(savedInstanceState);
        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // --- SET UP THE TOOLBAR ---
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        // Initialize database
        db = AppDatabase.getDatabase(this);

        // Set up the Pie Chart's basic style
        setupPieChart();

        // Set up listener for Daily/Monthly/Yearly selection
        setupTimePeriodSelector();
    }

    // This method handles the click of the "Up" button (the cross icon)
    @Override
    public boolean onSupportNavigateUp() {
        onBackPressed(); // This is the standard action for the Up button
        return true;
    }

    private void setupPieChart() {
        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setExtraOffsets(5, 10, 5, 5);

        binding.pieChart.setDragDecelerationFrictionCoef(0.95f);

        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleColor(Color.TRANSPARENT);

        // A larger hole for a cleaner look
        binding.pieChart.setHoleRadius(58f);
        binding.pieChart.setTransparentCircleRadius(61f);

        binding.pieChart.setDrawCenterText(true);

        binding.pieChart.setRotationAngle(0);
        binding.pieChart.setRotationEnabled(true);
        binding.pieChart.setHighlightPerTapEnabled(true);

        // Disable the default labels and legend
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.getLegend().setEnabled(false);

        // --- SET UP THE MARKER ---
        CustomMarkerView markerView = new CustomMarkerView(this, R.layout.marker_view);
        binding.pieChart.setMarker(markerView);

        // Add an animation
        binding.pieChart.animateY(1400, Easing.EaseInOutQuad);
    }

    private void setupTimePeriodSelector() {
        binding.toggleGroup.setOnCheckedChangeListener((group, checkedId) -> observeDataForPeriod(checkedId));

        // Load data for the initial selection
        observeDataForPeriod(binding.toggleGroup.getCheckedRadioButtonId());
    }

    private void observeDataForPeriod(int checkedId) {
        long startTime = getStartTimeForPeriod(checkedId);
        //long endTime = System.currentTimeMillis();
        LiveData<List<TransactionDao.CategoryTotal>> liveData = db.transactionDao().getExpenseTotalsByCategorySince(startTime);

        String title;
        if (checkedId == R.id.radio_daily) {
            title = "Daily Expense Breakdown";
        } else if (checkedId == R.id.radio_yearly) {
            title = "Yearly Expense Breakdown";
        } else { // Default to monthly
            title = "Monthly Expense Breakdown";
        }
        binding.chartTitle.setText(title);

        liveData.observe(this, categoryTotals -> {
            if (categoryTotals != null && !categoryTotals.isEmpty()) {
                loadPieChartData(categoryTotals);
            } else {
                binding.pieChart.setData(new PieData(new PieDataSet(new ArrayList<>(), "")));
                binding.pieChart.setCenterText(generateCenterSpannableText(0.0));

                // --- THEME-AWARE TEXT COLOR ---
                TypedValue typedValue = new TypedValue();
                getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
                int textColor = typedValue.data;
                binding.pieChart.setCenterTextColor(textColor);

                binding.pieChart.invalidate();
                binding.totalExpensesText.setText(NumberFormat.getCurrencyInstance().format(0.0));
            }
        });
    }

    private long getStartTimeForPeriod(int checkedId) {
        Calendar cal = Calendar.getInstance();
        if (checkedId == R.id.radio_daily) {
            cal.add(Calendar.DAY_OF_YEAR, -1);
        } else if (checkedId == R.id.radio_yearly) {
            cal.add(Calendar.YEAR, -1);
        } else { // Default to monthly
            cal.add(Calendar.MONTH, -1);
        }
        return cal.getTimeInMillis();
    }

    private void loadPieChartData(List<TransactionDao.CategoryTotal> categoryTotals) {
        ArrayList<PieEntry> entries = new ArrayList<>();
        double totalExpense = 0.0;

        for (TransactionDao.CategoryTotal total : categoryTotals) {
            entries.add(new PieEntry((float) Math.abs(total.total), total.category));
            totalExpense += total.total;
        }

        // Use a more professional color palette
        ArrayList<Integer> colors = new ArrayList<>();
        for (int c : ColorTemplate.MATERIAL_COLORS) {
            colors.add(c);
        }
        for (int c : ColorTemplate.PASTEL_COLORS) {
            colors.add(c);
        }

        PieDataSet dataSet = new PieDataSet(entries, "Expense Categories");
        dataSet.setSliceSpace(3f);
        dataSet.setColors(colors);
        dataSet.setDrawValues(true);

        PieData data = new PieData(dataSet);
        data.setValueFormatter(new PercentFormatter(binding.pieChart));
        data.setValueTextSize(12f);

        // --- THEME-AWARE TEXT COLOR ---
        TypedValue typedValue = new TypedValue();
        getTheme().resolveAttribute(com.google.android.material.R.attr.colorOnSurface, typedValue, true);
        int textColor = typedValue.data;
        data.setValueTextColor(textColor);
        binding.pieChart.setCenterTextColor(textColor);

        binding.pieChart.setData(data);
        binding.pieChart.invalidate();

        // Set the text in the center of the donut chart
        binding.pieChart.setCenterText(generateCenterSpannableText(totalExpense));
        binding.totalExpensesText.setVisibility(View.GONE);
    }

    private SpannableString generateCenterSpannableText(double totalExpense) {
        if (totalExpense == 0.0) {
            return new SpannableString("0 Expense!");
        }
        String totalAmount = NumberFormat.getCurrencyInstance().format(Math.abs(totalExpense));
        String text = totalAmount + "\nTotal Expenses";
        SpannableString s = new SpannableString(text);
        s.setSpan(new RelativeSizeSpan(1.7f), 0, totalAmount.length(), 0);
        s.setSpan(new StyleSpan(android.graphics.Typeface.BOLD), 0, totalAmount.length(), 0);
        return s;
    }
}
