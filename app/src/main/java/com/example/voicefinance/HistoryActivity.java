package com.example.voicefinance;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.RadioGroup;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.example.voicefinance.databinding.ActivityHistoryBinding;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class HistoryActivity extends AppCompatActivity {

    private ActivityHistoryBinding binding;
    private AppDatabase db;
    private int selectedWeek;
    private HistoryFilterType currentFilter = HistoryFilterType.DAY;
    private List<Transaction> cachedTransactions;
    private List<String> weekList;

    private int selectedDay;
    private int selectedMonth;
    private int selectedYear;

    private boolean spinnersReady = false;
    private String searchQuery = "";

    private List<String> yearList;

    // Custom interface to replace java.util.function.IntConsumer
    interface IntCallback {
        void accept(int value);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        ThemeHelper.applyTheme(this);
        super.onCreate(savedInstanceState);

        binding = ActivityHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // Handle status bar overlap
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, insets) -> {
            int topInset = insets.getInsets(WindowInsetsCompat.Type.systemBars()).top;
            v.setPadding(v.getPaddingLeft(), topInset, v.getPaddingRight(), v.getPaddingBottom());
            return insets;
        });

        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }
        binding.toolbar.setNavigationOnClickListener(v -> finish());

        db = AppDatabase.getDatabase(this);

        binding.recyclerView.setLayoutManager(
                new LinearLayoutManager(this)
        );

        // ---- TODAY DEFAULTS ----
        Calendar today = Calendar.getInstance();
        selectedDay = today.get(Calendar.DAY_OF_MONTH);
        selectedMonth = today.get(Calendar.MONTH);
        selectedYear = today.get(Calendar.YEAR);
        selectedWeek = HistoryUtils.getISOWeekNumber(today);

        setupDayButton();
        setupMonthYearSpinners();
        setupWeekSpinner();
        setupFilterGroup();

        // ---- SINGLE DB OBSERVER ----
        db.transactionDao()
                .getAllTransactionsOrdered()
                .observe(this, transactions -> {
                    cachedTransactions = transactions;
                    spinnersReady = true;
                    refreshList();
                });
    }

    // -----------------------------
    // FILTER RADIO GROUP
    // -----------------------------
    private void setupFilterGroup() {
        binding.filterGroup.setOnCheckedChangeListener(
                (RadioGroup group, int checkedId) -> {
                    if (checkedId == R.id.filter_year) {
                        currentFilter = HistoryFilterType.YEAR;
                        binding.dayButton.setVisibility(View.GONE);
                        binding.weekSpinner.setVisibility(View.GONE);
                        binding.monthSpinner.setVisibility(View.GONE);
                        binding.yearSpinner.setVisibility(View.VISIBLE);
                    } else if (checkedId == R.id.filter_month) {
                        currentFilter = HistoryFilterType.MONTH;
                        binding.dayButton.setVisibility(View.GONE);
                        binding.weekSpinner.setVisibility(View.GONE);
                        binding.monthSpinner.setVisibility(View.VISIBLE);
                        binding.yearSpinner.setVisibility(View.VISIBLE);
                    } else if (checkedId == R.id.filter_week) {
                        currentFilter = HistoryFilterType.WEEK;
                        binding.dayButton.setVisibility(View.GONE);
                        binding.weekSpinner.setVisibility(View.VISIBLE);
                        binding.monthSpinner.setVisibility(View.GONE);
                        binding.yearSpinner.setVisibility(View.VISIBLE);
                    } else { // DAY
                        currentFilter = HistoryFilterType.DAY;
                        binding.dayButton.setVisibility(View.VISIBLE);
                        binding.weekSpinner.setVisibility(View.GONE);
                        binding.monthSpinner.setVisibility(View.GONE);
                        binding.yearSpinner.setVisibility(View.GONE);
                    }
                    refreshList();
                }
        );
        // Set initial state
        binding.filterGroup.check(R.id.filter_day);
    }

    // -----------------------------
    // DAY PICKER
    // -----------------------------
    private void setupDayButton() {
        updateDayButtonText();
        Calendar cal = Calendar.getInstance();

        binding.dayButton.setOnClickListener(v -> {
            new DatePickerDialog(
                    this,
                    (view, year, month, day) -> {
                        selectedDay = day;
                        selectedMonth = month;
                        selectedYear = year;
                        updateDayButtonText();
                        refreshList();
                    },
                    cal.get(Calendar.YEAR),
                    cal.get(Calendar.MONTH),
                    cal.get(Calendar.DAY_OF_MONTH)
            ).show();
        });
    }

    private void updateDayButtonText() {
        Calendar cal = Calendar.getInstance();
        cal.set(selectedYear, selectedMonth, selectedDay);
        binding.dayButton.setText(
                new SimpleDateFormat("MMM d, yyyy", Locale.getDefault())
                        .format(cal.getTime())
        );
    }

    // -----------------------------
    // MONTH & YEAR SPINNERS
    // -----------------------------
    private void setupMonthYearSpinners() {

        // Months
        String[] months = new SimpleDateFormat(
                "MMMM", Locale.getDefault()
        ).getDateFormatSymbols().getMonths();

        binding.monthSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        months
                )
        );
        binding.monthSpinner.setSelection(selectedMonth);

        // Years
        yearList = new ArrayList<>();
        for (int y = selectedYear - 5; y <= selectedYear + 1; y++) {
            yearList.add(String.valueOf(y));
        }

        binding.yearSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        yearList
                )
        );
        binding.yearSpinner.setSelection(
                yearList.indexOf(String.valueOf(selectedYear))
        );

        binding.monthSpinner.setOnItemSelectedListener(
                new SimpleSelectionListener(pos -> {
                    selectedMonth = pos;
                    refreshList();
                })
        );

        binding.yearSpinner.setOnItemSelectedListener(
                new SimpleSelectionListener(pos -> {
                    int newYear = Integer.parseInt(yearList.get(pos));
                    if (newYear != selectedYear) {
                        selectedYear = newYear;
                        if (currentFilter == HistoryFilterType.WEEK) {
                            updateWeekSpinner();
                        }
                        refreshList();
                    }
                })
        );
    }

    // -----------------------------
    // WEEK SPINNER
    // -----------------------------
    private void setupWeekSpinner() {
        updateWeekSpinner();

        binding.weekSpinner.setOnItemSelectedListener(
                new SimpleSelectionListener(pos -> {
                    selectedWeek = pos + 1; // Week numbers are 1-based
                    refreshList();
                })
        );
    }
    private void updateWeekSpinner() {
        int maxWeeks = HistoryUtils.getMaxWeeksInYear(selectedYear);
        weekList = new ArrayList<>();

        SimpleDateFormat dateFormat = new SimpleDateFormat("MMM d", Locale.getDefault());

        for (int week = 1; week <= maxWeeks; week++) {
            Calendar weekStart = HistoryUtils.getWeekStart(week, selectedYear);
            Calendar weekEnd = (Calendar) weekStart.clone();
            weekEnd.add(Calendar.DAY_OF_YEAR, 6);

            String startStr = dateFormat.format(weekStart.getTime());
            String endStr = dateFormat.format(weekEnd.getTime());

            weekList.add("Week " + week + " (" + startStr + "–" + endStr + ")");
        }

        binding.weekSpinner.setAdapter(
                new ArrayAdapter<>(
                        this,
                        android.R.layout.simple_spinner_dropdown_item,
                        weekList
                )
        );

        // Set selection to current week, or closest available
        int selection = Math.min(selectedWeek - 1, maxWeeks - 1);
        binding.weekSpinner.setSelection(selection);
        selectedWeek = selection + 1;
    }

    // -----------------------------
    // REFRESH LIST
    // -----------------------------
    private void refreshList() {
        if (!spinnersReady || cachedTransactions == null) return;

        List<Transaction> source;

        if (currentFilter == HistoryFilterType.DAY) {
            source = HistoryUtils.filterByDay(
                    cachedTransactions,
                    selectedDay,
                    selectedMonth,
                    selectedYear
            );

        } else if (currentFilter == HistoryFilterType.WEEK) {
            source = HistoryUtils.filterByWeek(
                    cachedTransactions,
                    selectedWeek,
                    selectedYear
            );
        }else if (currentFilter == HistoryFilterType.MONTH) {
            source = HistoryUtils.filterByMonthYear(
                    cachedTransactions,
                    selectedMonth,
                    selectedYear
            );
        } else {
            source = HistoryUtils.filterByYear(
                    cachedTransactions,
                    selectedYear
            );
        }

        // Apply search query
        List<Transaction> filteredSource = new ArrayList<>();
        if (searchQuery.isEmpty()) {
            filteredSource.addAll(source);
        } else {
            for (Transaction t : source) {
                if (t.label.toLowerCase().contains(searchQuery) ||
                    t.category.toLowerCase().contains(searchQuery)) {
                    filteredSource.add(t);
                }
            }
        }

        binding.recyclerView.setAdapter(
                new HistoryAdapter(
                        HistoryUtils.buildHistoryItems(
                                filteredSource,
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
                            if (which == 0) showEditDialog(transaction);
                            else confirmDelete(transaction);
                        }
                )
                .show();
    }

    // -----------------------------
    // EDIT / DELETE (UNCHANGED)
    // -----------------------------
    private void showEditDialog(Transaction transaction) { /* unchanged */ }
    private void confirmDelete(Transaction transaction) { /* unchanged */ }

    // -----------------------------
    // SEARCH
    // -----------------------------
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_history, menu);

        SearchView searchView =
                (SearchView) menu.findItem(R.id.action_search).getActionView();

        searchView.setQueryHint("Search transactions");

        searchView.setOnQueryTextListener(
                new SearchView.OnQueryTextListener() {
                    @Override public boolean onQueryTextSubmit(String q) { return false; }
                    @Override public boolean onQueryTextChange(String t) {
                        searchQuery = t.toLowerCase();
                        refreshList();
                        return true;
                    }
                }
        );
        return true;
    }

    // -----------------------------
    // SIMPLE LISTENER
    // -----------------------------
    private static class SimpleSelectionListener
            implements android.widget.AdapterView.OnItemSelectedListener {

        private final IntCallback callback;
        SimpleSelectionListener(IntCallback c) {
            callback = c;
        }

        @Override
        public void onItemSelected(
                android.widget.AdapterView<?> parent,
                android.view.View view,
                int position,
                long id) {
            callback.accept(position);
        }

        @Override public void onNothingSelected(
                android.widget.AdapterView<?> parent) {}
    }
}
