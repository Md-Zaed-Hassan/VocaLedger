package com.example.voicefinance;

import java.text.SimpleDateFormat;
import java.util.*;

public class HistoryUtils {

    private static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("MMM d · EEEE", Locale.getDefault());

    public static List<HistoryListItem> buildHistoryItems(
            List<Transaction> transactions) {

        List<HistoryListItem> result = new ArrayList<>();
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();

        // Group by date string
        for (Transaction t : transactions) {
            String key = DAY_FORMAT.format(new Date(t.timestamp));
            grouped.computeIfAbsent(key, k -> new ArrayList<>()).add(t);
        }

        // Build list with headers
        for (String date : grouped.keySet()) {
            double dailyTotal = 0;

            for (Transaction t : grouped.get(date)) {
                if (t.amount < 0) {
                    dailyTotal += t.amount;
                }
            }

            result.add(
                    new HistoryListItem.DateHeader(
                            date,
                            Math.abs(dailyTotal)
                    )
            );

            for (Transaction t : grouped.get(date)) {
                result.add(
                        new HistoryListItem.TransactionItem(t)
                );
            }
        }

        return result;
    }
}
