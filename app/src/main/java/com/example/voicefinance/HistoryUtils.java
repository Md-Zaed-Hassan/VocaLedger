package com.example.voicefinance;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class HistoryUtils {

    private static final SimpleDateFormat DAY_FORMAT =
            new SimpleDateFormat("MMM d · EEEE", Locale.getDefault());

    public static List<HistoryListItem> buildHistoryItems(
            List<Transaction> transactions,
            HistoryFilterType filterType
    ) {

        List<HistoryListItem> result = new ArrayList<>();
        Map<String, List<Transaction>> grouped = new LinkedHashMap<>();

        // -----------------------------
        // GROUP TRANSACTIONS
        // -----------------------------
        for (Transaction t : transactions) {

            String key = DAY_FORMAT.format(new Date(t.timestamp));

            List<Transaction> list = grouped.get(key);
            if (list == null) {
                list = new ArrayList<>();
                grouped.put(key, list);
            }

            list.add(t);
        }

        // -----------------------------
        // BUILD DISPLAY LIST
        // -----------------------------
        for (Map.Entry<String, List<Transaction>> entry : grouped.entrySet()) {

            String date = entry.getKey();
            List<Transaction> dayTransactions = entry.getValue();

            double dailyTotal = 0;
            for (Transaction t : dayTransactions) {
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

            for (Transaction t : dayTransactions) {
                result.add(
                        new HistoryListItem.TransactionItem(t)
                );
            }
        }

        return result;
    }
}
