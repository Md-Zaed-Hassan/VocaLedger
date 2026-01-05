package com.example.voicefinance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryUtils {

    private static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("MMM d · EEEE", Locale.getDefault());

    private static final SimpleDateFormat MONTH_FORMAT =
            new SimpleDateFormat("MMMM yyyy", Locale.getDefault());

    private static final SimpleDateFormat YEAR_FORMAT =
            new SimpleDateFormat("yyyy", Locale.getDefault());

    // ------------------------------------------------
    // BUILD HISTORY LIST (GROUPED)
    // ------------------------------------------------
    public static List<HistoryListItem> buildHistoryItems(
            List<Transaction> transactions,
            HistoryFilterType filterType
    ) {

        List<HistoryListItem> result = new ArrayList<>();
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();

        for (Transaction t : transactions) {

            Date date = new Date(t.timestamp);
            String key;

            if (filterType == HistoryFilterType.YEAR) {
                key = YEAR_FORMAT.format(date);
            } else if (filterType == HistoryFilterType.MONTH) {
                key = MONTH_FORMAT.format(date);
            } else {
                key = DAY_FORMAT.format(date);
            }

            grouped.computeIfAbsent(key, k -> new ArrayList<>())
                    .add(t);
        }

        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {

            double totalExpense = 0;
            for (Transaction t : entry.getValue()) {
                if (t.amount < 0) {
                    totalExpense += t.amount;
                }
            }

            result.add(
                    new HistoryListItem.DateHeader(
                            entry.getKey(),
                            Math.abs(totalExpense)
                    )
            );

            for (Transaction t : entry.getValue()) {
                result.add(new HistoryListItem.TransactionItem(t));
            }
        }

        return result;
    }

    // ------------------------------------------------
    // FILTER: DAY
    // ------------------------------------------------
    public static List<Transaction> filterByDay(
            List<Transaction> transactions,
            int day,
            int month,
            int year
    ) {

        List<Transaction> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (Transaction t : transactions) {
            cal.setTimeInMillis(t.timestamp);

            if (cal.get(Calendar.DAY_OF_MONTH) == day &&
                    cal.get(Calendar.MONTH) == month &&
                    cal.get(Calendar.YEAR) == year) {
                result.add(t);
            }
        }
        return result;
    }

    // ------------------------------------------------
    // FILTER: MONTH + YEAR
    // ------------------------------------------------
    public static List<Transaction> filterByMonthYear(
            List<Transaction> transactions,
            int month,
            int year
    ) {

        List<Transaction> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (Transaction t : transactions) {
            cal.setTimeInMillis(t.timestamp);

            if (cal.get(Calendar.MONTH) == month &&
                    cal.get(Calendar.YEAR) == year) {
                result.add(t);
            }
        }
        return result;
    }

    // ------------------------------------------------
    // FILTER: YEAR
    // ------------------------------------------------
    public static List<Transaction> filterByYear(
            List<Transaction> transactions,
            int year
    ) {

        List<Transaction> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();

        for (Transaction t : transactions) {
            cal.setTimeInMillis(t.timestamp);

            if (cal.get(Calendar.YEAR) == year) {
                result.add(t);
            }
        }
        return result;
    }
}
