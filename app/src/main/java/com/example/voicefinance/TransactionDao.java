package com.example.voicefinance;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import java.util.List;

@Dao
public interface TransactionDao {

    @Insert
    void insert(Transaction transaction);

    @Query("SELECT * FROM transactions ORDER BY created_at DESC")
    LiveData<List<Transaction>> getAllTransactions();

    @Query("SELECT category, SUM(amount) as total FROM transactions WHERE amount < 0 AND timestamp >= :since GROUP BY category")
    LiveData<List<CategoryTotal>> getExpenseTotalsByCategorySince(long since);

    class CategoryTotal {
        public String category;
        public double total;
    }
}