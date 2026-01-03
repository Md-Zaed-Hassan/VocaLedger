package com.example.voicefinance;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Delete;
import androidx.room.Update; // ✅ REQUIRED for edit support

import java.util.List;

@Dao
public interface TransactionDao {

    /* ---------------------------------------------------
     * BASIC CRUD
     * --------------------------------------------------- */

    // Insert a new transaction (used by MainActivity)
    @Insert
    void insert(Transaction transaction);

    // ✅ REQUIRED: Update an existing transaction (Phase F – Edit)
    @Update
    void update(Transaction transaction);

    // Delete a transaction (Phase F – Delete)
    @Delete
    void delete(Transaction transaction);


    /* ---------------------------------------------------
     * DASHBOARD / HISTORY
     * --------------------------------------------------- */

    // Fetch all transactions ordered by creation time
    // Used by dashboard + history screen
    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    LiveData<List<Transaction>> getAllTransactions();


    /* ---------------------------------------------------
     * LEGACY ANALYTICS (RELATIVE TIME)
     * --------------------------------------------------- */

    // Backward-compatible analytics
    // Used by existing StatisticsActivity logic
    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 AND timestamp >= :since " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getExpenseTotalsByCategorySince(long since);


    /* ---------------------------------------------------
     * CALENDAR-CORRECT ANALYTICS (PHASE E / E2)
     * --------------------------------------------------- */

    // YEARLY expense breakdown by category
    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND strftime('%Y', timestamp / 1000, 'unixepoch') = :year " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getYearlyExpenseByCategory(String year);

    // MONTHLY expense breakdown by category
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

    // DAILY expense breakdown (current day)
    @Query(
            "SELECT category, SUM(amount) AS total " +
                    "FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND date(timestamp / 1000, 'unixepoch') = date('now') " +
                    "GROUP BY category"
    )
    LiveData<List<CategoryTotal>> getDailyExpenseByCategory();


    /* ---------------------------------------------------
     * MONTHLY SUMMARY (BUDGET + DASHBOARD)
     * --------------------------------------------------- */

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

    // Total expense for CURRENT month (budget warning)
    @Query(
            "SELECT IFNULL(SUM(amount), 0) " +
                    "FROM transactions " +
                    "WHERE amount < 0 " +
                    "AND strftime('%Y-%m', timestamp / 1000, 'unixepoch') = " +
                    "      strftime('%Y-%m', 'now')"
    )
    LiveData<Double> getCurrentMonthExpense();


    /* ---------------------------------------------------
     * CATEGORY DRILL-DOWN (PHASE E)
     * --------------------------------------------------- */

    // Transactions for a category within a month/year
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


    /* ---------------------------------------------------
     * RESULT HOLDER (MPAndroidChart)
     * --------------------------------------------------- */

    // Simple POJO for aggregated results
    class CategoryTotal {
        public String category;
        public double total;
    }
}
