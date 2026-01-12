package com.example.voicefinance;

import android.content.Context;
import androidx.lifecycle.LiveData;

public class BudgetHelper {

    // Save new budget (only one can be active)
    public static void saveBudget(Context ctx, double amount, long start, long end) {
        AppDatabase.databaseWriteExecutor.execute(() -> {
            BudgetDao dao = AppDatabase.getDatabase(ctx).budgetDao();
            dao.deactivateAll();
            dao.insert(new Budget(amount, start, end, true));
        });
    }

    // Live active budget (UI safe)
    public static LiveData<Budget> getActiveBudget(Context ctx) {
        return AppDatabase.getDatabase(ctx).budgetDao().getActiveBudget();
    }
}
