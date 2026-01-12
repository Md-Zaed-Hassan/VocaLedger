package com.example.voicefinance;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

@Dao
public interface TransactionDao {

    /* ---------------------------------------------------
     * BASIC CRUD
     * --------------------------------------------------- */

    @Insert
    void insert(Transaction transaction);

    @Update
    void update(Transaction transaction);

    @Delete
    void delete(Transaction transaction);

    /* ---------------------------------------------------
     * CANONICAL TRANSACTION LIST
     * --------------------------------------------------- */

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    LiveData<List<Transaction>> getAllTransactionsOrdered();

    /* ---------------------------------------------------
     * RELATIVE ANALYTICS (PIE CHART)
     * --------------------------------------------------- */

    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 AND timestamp >= :since " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getExpenseTotalsByCategorySince(long since);

    /* ---------------------------------------------------
     * MONTHLY SUMMARY
     * --------------------------------------------------- */

    @Query(
            "SELECT IFNULL(SUM(amount),0) FROM transactions " +
                    "WHERE amount > 0 " +
                    "AND strftime('%Y', timestamp/1000, 'unixepoch', 'localtime') = :year " +
                    "AND strftime('%m', timestamp/1000, 'unixepoch', 'localtime') = :month"
    )
    LiveData<Double> getMonthlyIncome(String year, String month);

    @Query(
            "SELECT IFNULL(SUM(amount),0) FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND strftime('%Y', timestamp/1000, 'unixepoch', 'localtime') = :year " +
                    "AND strftime('%m', timestamp/1000, 'unixepoch', 'localtime') = :month"
    )
    LiveData<Double> getMonthlyExpense(String year, String month);

    @Query(
            "SELECT IFNULL(SUM(amount),0) FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND strftime('%Y-%m', timestamp/1000, 'unixepoch', 'localtime') = " +
                    "strftime('%Y-%m','now', 'localtime')"
    )
    LiveData<Double> getCurrentMonthExpense();

    /* ---------------------------------------------------
     * CATEGORY DRILL-DOWN
     * --------------------------------------------------- */

    @Query(
            "SELECT * FROM transactions " +
                    "WHERE category = :category AND amount < 0 " +
                    "ORDER BY timestamp DESC"
    )
    LiveData<List<Transaction>> getTransactionsByCategory(String category);

    @Query(
            "SELECT * FROM transactions " +
                    "WHERE category = :category AND amount < 0 " +
                    "AND strftime('%Y', timestamp/1000, 'unixepoch', 'localtime') = :year " +
                    "AND strftime('%m', timestamp/1000, 'unixepoch', 'localtime') = :month " +
                    "ORDER BY timestamp DESC"
    )
    LiveData<List<Transaction>> getTransactionsByCategoryAndMonth(
            String category, String year, String month
    );

    /* ---------------------------------------------------
     * SINGLE-CATEGORY TREND (LEGACY)
     * --------------------------------------------------- */

    @Query(
            "SELECT strftime('%m', timestamp/1000, 'unixepoch', 'localtime') AS period, " +
                    "SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE category = :category AND amount < 0 " +
                    "AND strftime('%Y', timestamp/1000, 'unixepoch', 'localtime') = :year " +
                    "GROUP BY period " +
                    "ORDER BY period"
    )
    LiveData<List<PeriodTotal>> getCategoryTrendsByYear(String category, String year);

    /* ---------------------------------------------------
     * MULTI-CATEGORY TIME-SERIES (FOR LINE CHART)
     * --------------------------------------------------- */

    // DAILY timeline
    @Query(
            "SELECT " +
                    "strftime('%Y-%m-%d', timestamp/1000, 'unixepoch', 'localtime') AS period, " +
                    "category, " +
                    "SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 AND timestamp >= :since " +
                    "GROUP BY period, category " +
                    "ORDER BY period ASC"
    )
    LiveData<List<TrendPoint>> getDailyTrends(long since);

    // MONTHLY timeline
    @Query(
            "SELECT " +
                    "strftime('%Y-%m', timestamp/1000, 'unixepoch', 'localtime') AS period, " +
                    "category, " +
                    "SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 AND timestamp >= :since " +
                    "GROUP BY period, category " +
                    "ORDER BY period ASC"
    )
    LiveData<List<TrendPoint>> getMonthlyTrends(long since);

    // YEARLY timeline
    @Query(
            "SELECT " +
                    "strftime('%Y', timestamp/1000, 'unixepoch', 'localtime') AS period, " +
                    "category, " +
                    "SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 AND timestamp >= :since " +
                    "GROUP BY period, category " +
                    "ORDER BY period ASC"
    )
    LiveData<List<TrendPoint>> getYearlyTrends(long since);

    @Query(
            "SELECT IFNULL(SUM(amount),0) FROM transactions " +
                    "WHERE amount < 0 AND timestamp BETWEEN :start AND :end"
    )
    LiveData<Double> getExpenseBetween(long start, long end);



    /* ---------------------------------------------------
     * RESULT HOLDERS
     * --------------------------------------------------- */

    class CategoryTotal {
        public String category;
        public double total;
    }

    class PeriodTotal {
        public String period;
        public double total;
    }

    class TrendPoint {
        public String period;   // 2025-01-05, 2025-01, or 2025
        public String category;
        public double total;
    }
}
