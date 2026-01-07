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

    private static final SimpleDateFormat WEEK_START_FORMAT =
            new SimpleDateFormat("MMM d", Locale.getDefault());

    private static final SimpleDateFormat WEEK_END_FORMAT =
            new SimpleDateFormat("MMM d, yyyy", Locale.getDefault());
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
            }else if (filterType == HistoryFilterType.WEEK) {
                key = getWeekKey(date);
            }else {
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

    // ------------------------------------------------
    // FILTER: WEEK (ISO Week)
    // ------------------------------------------------
    public static List<Transaction> filterByWeek(
            List<Transaction> transactions,
            int weekNumber,
            int year
    ) {

        List<Transaction> result = new ArrayList<>();
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);

        // Get the start and end of the specified ISO week
        Calendar weekStart = getWeekStart(weekNumber, year);
        Calendar weekEnd = (Calendar) weekStart.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);

        long weekStartMillis = weekStart.getTimeInMillis();
        long weekEndMillis = weekEnd.getTimeInMillis();

        for (Transaction t : transactions) {
            if (t.timestamp >= weekStartMillis && t.timestamp <= weekEndMillis) {
                result.add(t);
            }
        }
        return result;
    }

    // ------------------------------------------------
    // WEEK KEY FOR GROUPING (ISO Week)
    // ------------------------------------------------
    private static String getWeekKey(Date date) {
        Calendar cal = Calendar.getInstance();
        cal.setTime(date);
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);

        // Get the start of the week (Monday)
        int dayOfWeek = cal.get(Calendar.DAY_OF_WEEK);
        int daysFromMonday = (dayOfWeek == Calendar.SUNDAY) ? 6 : dayOfWeek - Calendar.MONDAY;
        cal.add(Calendar.DAY_OF_YEAR, -daysFromMonday);

        Date weekStart = cal.getTime();
        Calendar weekEnd = (Calendar) cal.clone();
        weekEnd.add(Calendar.DAY_OF_YEAR, 6);
        Date weekEndDate = weekEnd.getTime();

        String startStr = WEEK_START_FORMAT.format(weekStart);
        String endStr = WEEK_END_FORMAT.format(weekEndDate);

        // Extract year from end date (handles year boundaries)
        Calendar endCal = Calendar.getInstance();
        endCal.setTime(weekEndDate);
        int year = endCal.get(Calendar.YEAR);

        return startStr + "–" + endStr;
    }

    // ------------------------------------------------
    // GET WEEK START (ISO Week)
    // ------------------------------------------------
    public static Calendar getWeekStart(int weekNumber, int year) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        cal.clear();
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.WEEK_OF_YEAR, weekNumber);
        cal.set(Calendar.DAY_OF_WEEK, Calendar.MONDAY);
        cal.set(Calendar.HOUR_OF_DAY, 0);
        cal.set(Calendar.MINUTE, 0);
        cal.set(Calendar.SECOND, 0);
        cal.set(Calendar.MILLISECOND, 0);
        return cal;
    }

    // ------------------------------------------------
    // GET ISO WEEK NUMBER
    // ------------------------------------------------
    public static int getISOWeekNumber(Calendar cal) {
        Calendar isoCal = (Calendar) cal.clone();
        isoCal.setFirstDayOfWeek(Calendar.MONDAY);
        isoCal.setMinimalDaysInFirstWeek(4);
        return isoCal.get(Calendar.WEEK_OF_YEAR);
    }

    // ------------------------------------------------
    // GET MAX WEEKS IN YEAR (ISO)
    // ------------------------------------------------
    public static int getMaxWeeksInYear(int year) {
        Calendar cal = Calendar.getInstance();
        cal.setFirstDayOfWeek(Calendar.MONDAY);
        cal.setMinimalDaysInFirstWeek(4);
        cal.set(Calendar.YEAR, year);
        cal.set(Calendar.MONTH, Calendar.DECEMBER);
        cal.set(Calendar.DAY_OF_MONTH, 31);
        return cal.get(Calendar.WEEK_OF_YEAR);
    }
}
