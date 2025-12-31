package com.example.voicefinance;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

@Dao
public interface TransactionDao {

    /* ---------------------------------------------------
     * BASIC OPERATIONS
     * --------------------------------------------------- */

    // Insert a new transaction (used by MainActivity)
    @Insert
    void insert(Transaction transaction);

    // Fetch all transactions ordered by creation time
    // Used for dashboard balance calculation
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    LiveData<List<Transaction>> getAllTransactions();


    /* ---------------------------------------------------
     * LEGACY ANALYTICS (RELATIVE TIME)
     * --------------------------------------------------- */

    // Existing method used by your StatisticsActivity
    // This supports the old "last N days/months/year" logic
    // We KEEP this for backward compatibility
    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 AND timestamp >= :since " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getExpenseTotalsByCategorySince(long since);


    /* ---------------------------------------------------
     * CALENDAR-CORRECT ANALYTICS (NEW – PHASE 2)
     * --------------------------------------------------- */

    // YEARLY expense breakdown by category
    // Used when user selects "Yearly" + a specific year
    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND strftime('%Y', timestamp / 1000, 'unixepoch') = :year " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getYearlyExpenseByCategory(String year);


    // MONTHLY expense breakdown by category (under a selected year)
    // Used when user selects "Monthly" + a year
    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND strftime('%Y', timestamp / 1000, 'unixepoch') = :year " +
                    "AND strftime('%m', timestamp / 1000, 'unixepoch') = :month " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getMonthlyExpenseByCategory(
            String year,
            String month
    );


    // DAILY expense breakdown by category
    // Always uses the current day (no year/month needed)
    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND date(timestamp / 1000, 'unixepoch') = date('now') " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getDailyExpenseByCategory();


    /* ---------------------------------------------------
     * RESULT HOLDER (USED BY MPAndroidChart)
     * --------------------------------------------------- */

    // Simple POJO for chart aggregation
    class CategoryTotal {
        public String category;
        public double total;
    }

    // Total income for a given month
    @Query(
            "SELECT IFNULL(SUM(amount), 0) FROM transactions " +
                    "WHERE amount > 0 " +
                    "AND strftime('%Y', timestamp / 1000, 'unixepoch') = :year " +
                    "AND strftime('%m', timestamp / 1000, 'unixepoch') = :month"
    )
    LiveData<Double> getMonthlyIncome(String year, String month);


    // Total expense for a given month
    @Query(
            "SELECT IFNULL(SUM(amount), 0) FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND strftime('%Y', timestamp / 1000, 'unixepoch') = :year " +
                    "AND strftime('%m', timestamp / 1000, 'unixepoch') = :month"
    )
    LiveData<Double> getMonthlyExpense(String year, String month);

    // Fetch transactions for a specific category within a month/year
    @Query(
            "SELECT * FROM transactions " +
                    "WHERE category = :category " +
                    "AND amount < 0 " +
                    "AND strftime('%Y', timestamp / 1000, 'unixepoch') = :year " +
                    "AND strftime('%m', timestamp / 1000, 'unixepoch') = :month " +
                    "ORDER BY timestamp DESC"
    )
    LiveData<List<Transaction>> getTransactionsByCategoryAndMonth(
            String category,
            String year,
            String month
    );

}
