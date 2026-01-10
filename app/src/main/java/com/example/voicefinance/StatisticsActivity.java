package com.example.voicefinance;

import android.content.Intent;
import android.content.res.Configuration;
import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.text.SpannableString;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.view.View;

import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.LiveData;

import com.example.voicefinance.databinding.ActivityStatisticsBinding;
import com.github.mikephil.charting.animation.Easing;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.components.XAxis;
import com.github.mikephil.charting.components.YAxis;
import com.github.mikephil.charting.data.Entry;
import com.github.mikephil.charting.data.LineData;
import com.github.mikephil.charting.data.LineDataSet;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.github.mikephil.charting.highlight.Highlight;
import com.github.mikephil.charting.listener.OnChartValueSelectedListener;
import com.github.mikephil.charting.utils.ColorTemplate;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class StatisticsActivity extends AppCompatActivity {

    private ActivityStatisticsBinding binding;
    private AppDatabase db;

    private LiveData<List<TransactionDao.TrendPoint>> activeTrendLiveData;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityStatisticsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = AppDatabase.getDatabase(this);

        setupPieChart();
        setupPieClick();
        setupTrendChart();
        setupPeriodSelector();
    }

    /* ---------------- PIE CHART ---------------- */

    private void setupPieChart() {

        boolean isDark =
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        binding.pieChart.setUsePercentValues(true);
        binding.pieChart.getDescription().setEnabled(false);
        binding.pieChart.setDrawHoleEnabled(true);
        binding.pieChart.setHoleRadius(60f);
        binding.pieChart.setTransparentCircleRadius(63f);
        binding.pieChart.setDrawCenterText(true);
        binding.pieChart.setDrawEntryLabels(false);
        binding.pieChart.setRotationEnabled(true);

        binding.pieChart.setHoleColor(isDark ? Color.parseColor("#1E1E1E") : Color.WHITE);

        Legend legend = binding.pieChart.getLegend();
        legend.setEnabled(true);
        legend.setTextColor(isDark ? Color.WHITE : Color.BLACK);
        legend.setTextSize(12f);

        binding.pieChart.animateY(1000, Easing.EaseInOutQuad);
    }

    private void setupPieClick() {
        binding.pieChart.setOnChartValueSelectedListener(new OnChartValueSelectedListener() {
            @Override
            public void onValueSelected(com.github.mikephil.charting.data.Entry e, Highlight h) {
                if (!(e instanceof PieEntry)) return;
                String category = ((PieEntry) e).getLabel();
                Intent i = new Intent(StatisticsActivity.this, CategoryDetailActivity.class);
                i.putExtra("category", category);
                startActivity(i);
            }
            @Override public void onNothingSelected() {}
        });
    }

    /* ---------------- PERIOD SWITCH ---------------- */

    private void setupPeriodSelector() {
        binding.toggleGroup.setOnCheckedChangeListener((g, id) -> loadForPeriod(id));
        loadForPeriod(binding.toggleGroup.getCheckedRadioButtonId());
    }

    private void loadForPeriod(int id) {
        // Pie chart gets data for the current period
        long pieChartSince = getPieChartStartTime(id);
        db.transactionDao()
                .getExpenseTotalsByCategorySince(pieChartSince)
                .observe(this, this::loadPie);

        if (activeTrendLiveData != null) {
            activeTrendLiveData.removeObservers(this);
        }

        // Line chart gets data for a trend window
        long lineChartSince = getLineChartStartTime(id);
        if (id == R.id.radio_daily) {
            activeTrendLiveData = db.transactionDao().getDailyTrends(lineChartSince);
        } else if (id == R.id.radio_yearly) {
            activeTrendLiveData = db.transactionDao().getYearlyTrends(lineChartSince);
        } else { // R.id.radio_monthly
            activeTrendLiveData = db.transactionDao().getMonthlyTrends(lineChartSince);
        }
        activeTrendLiveData.observe(this, this::drawTrends);
    }

    /* ---------------- PIE DATA ---------------- */

    private void loadPie(List<TransactionDao.CategoryTotal> totals) {

        boolean isDark =
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;

        List<PieEntry> entries = new ArrayList<>();
        double total = 0;

        for (TransactionDao.CategoryTotal t : totals) {
            entries.add(new PieEntry((float) Math.abs(t.total), t.category));
            total += t.total;
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextSize(14f); // Increase percentage text size
        set.setSelectionShift(25f); // Increase selected slice size

        PieData data = new PieData(set);
        data.setValueFormatter(new PercentFormatter(binding.pieChart));
        data.setValueTextColor(isDark ? Color.WHITE : Color.BLACK);

        binding.pieChart.setData(data);
        binding.pieChart.setCenterText(makeCenterText(total));
        binding.pieChart.setCenterTextColor(isDark ? Color.WHITE : Color.BLACK);
        binding.pieChart.invalidate();
    }

    private SpannableString makeCenterText(double total) {
        String amount = CurrencyUtils.getCurrencyInstance().format(Math.abs(total));
        SpannableString s = new SpannableString(amount + "\nTotal Expenses");
        s.setSpan(new RelativeSizeSpan(1.6f), 0, amount.length(), 0);
        s.setSpan(new StyleSpan(Typeface.BOLD), 0, amount.length(), 0);
        return s;
    }

    /* ---------------- LINE CHART ---------------- */

    private void setupTrendChart() {
        binding.trendsChart.getDescription().setEnabled(false);
        binding.trendsChart.getAxisRight().setEnabled(false);

        boolean isDark =
                (getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK)
                        == Configuration.UI_MODE_NIGHT_YES;
        int textColor = isDark ? Color.WHITE : Color.BLACK;

        XAxis xAxis = binding.trendsChart.getXAxis();
        xAxis.setPosition(XAxis.XAxisPosition.BOTTOM);
        xAxis.setTextColor(textColor);
        xAxis.setLabelRotationAngle(-45);


        YAxis yAxis = binding.trendsChart.getAxisLeft();
        yAxis.setTextColor(textColor);

        binding.trendsChart.getLegend().setTextColor(textColor);
    }

    private void drawTrends(List<TransactionDao.TrendPoint> rows) {
        if (rows == null) {
            rows = Collections.emptyList();
        }

        final List<String> periods = generatePeriods();

        // This ensures the chart is always visible with a timeline, even if there is no data.
        binding.trendsChart.setVisibility(View.VISIBLE);
        binding.trendsChartTitle.setVisibility(View.VISIBLE);

        Map<String, float[]> dataMap = new LinkedHashMap<>();
        for (TransactionDao.TrendPoint r : rows) {
            int x = periods.indexOf(r.period);
            if (x == -1) continue;

            float[] series = dataMap.get(r.category);
            if (series == null) {
                series = new float[periods.size()];
                dataMap.put(r.category, series);
            }
            series[x] = (float) Math.abs(r.total);
        }

        LineData data = new LineData();
        int[] colors = ColorTemplate.MATERIAL_COLORS;
        int i = 0;

        if (dataMap.isEmpty()) {
            binding.trendsChart.clear();
        } else {
            for (Map.Entry<String, float[]> e : dataMap.entrySet()) {
                List<Entry> entries = new ArrayList<>();
                float[] yValues = e.getValue();
                for (int x = 0; x < yValues.length; x++) {
                    entries.add(new Entry(x, yValues[x]));
                }

                LineDataSet set = new LineDataSet(entries, e.getKey());
                int c = colors[i++ % colors.length];
                set.setColor(c);
                set.setCircleColor(c);
                set.setLineWidth(2f);
                set.setDrawValues(false);
                data.addDataSet(set);
            }
        }

        XAxis xAxis = binding.trendsChart.getXAxis();
        xAxis.setGranularity(1f);
        xAxis.setLabelCount(periods.size(), false);
        xAxis.setValueFormatter(new ValueFormatter() {
            @Override
            public String getFormattedValue(float v) {
                int i = (int) v;
                if (i < 0 || i >= periods.size()) return "";

                String period = periods.get(i);
                int checkedId = binding.toggleGroup.getCheckedRadioButtonId();

                if ((checkedId == R.id.radio_daily || checkedId == R.id.radio_monthly) && period.length() > 5) {
                    return period.substring(5);
                }
                return period;
            }
        });

        binding.trendsChart.setData(data);
        binding.trendsChart.invalidate();
    }

    private List<String> generatePeriods() {
        List<String> periods = new ArrayList<>();
        int checkedId = binding.toggleGroup.getCheckedRadioButtonId();
        long since = getLineChartStartTime(checkedId);

        Calendar cal = Calendar.getInstance();
        cal.setTimeInMillis(since);

        Calendar endCal = Calendar.getInstance();

        SimpleDateFormat fmt;
        int calendarField;

        if (checkedId == R.id.radio_daily) {
            fmt = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
            calendarField = Calendar.DAY_OF_YEAR;
        } else if (checkedId == R.id.radio_monthly) {
            fmt = new SimpleDateFormat("yyyy-MM", Locale.US);
            calendarField = Calendar.MONTH;
            cal.set(Calendar.DAY_OF_MONTH, 1); // Normalize to start of month
        } else { // R.id.radio_yearly
            fmt = new SimpleDateFormat("yyyy", Locale.US);
            calendarField = Calendar.YEAR;
            cal.set(Calendar.DAY_OF_YEAR, 1); // Normalize to start of year
        }

        while (cal.before(endCal) || cal.equals(endCal)) {
            periods.add(fmt.format(cal.getTime()));
            cal.add(calendarField, 1);
        }

        return periods;
    }

    private long getPieChartStartTime(int id) {
        Calendar c = Calendar.getInstance();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        c.set(Calendar.MILLISECOND, 0);

        if (id == R.id.radio_monthly) {
            c.set(Calendar.DAY_OF_MONTH, 1);
        } else if (id == R.id.radio_yearly) {
            c.set(Calendar.DAY_OF_YEAR, 1);
        }
        return c.getTimeInMillis();
    }

    private long getLineChartStartTime(int id) {
        Calendar c = Calendar.getInstance();
        if (id == R.id.radio_daily) c.add(Calendar.DAY_OF_YEAR, -29); // 30 days
        else if (id == R.id.radio_yearly) c.add(Calendar.YEAR, -4); // 5 years
        else c.add(Calendar.MONTH, -11); // 12 months
        return c.getTimeInMillis();
    }
}
