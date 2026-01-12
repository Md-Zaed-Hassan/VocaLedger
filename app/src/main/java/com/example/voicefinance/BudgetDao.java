package com.example.voicefinance;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;

@Dao
public interface BudgetDao {

    @Query("DELETE FROM budget")
    void clearBudgets();

    @Query("UPDATE budget SET active = 0")
    void deactivateAll();

    @Insert
    void insert(Budget budget);

    @Query("SELECT * FROM budget WHERE active = 1 LIMIT 1")
    LiveData<Budget> getActiveBudget();


}
