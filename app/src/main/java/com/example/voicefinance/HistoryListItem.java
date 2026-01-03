package com.example.voicefinance;

public abstract class HistoryListItem {

    public static class DateHeader extends HistoryListItem {
        public String dateText;   // "Dec 7 · Sunday"
        public double total;

        public DateHeader(String dateText, double total) {
            this.dateText = dateText;
            this.total = total;
        }
    }

    public static class TransactionItem extends HistoryListItem {
        public Transaction transaction;

        public TransactionItem(Transaction transaction) {
            this.transaction = transaction;
        }
    }
}
