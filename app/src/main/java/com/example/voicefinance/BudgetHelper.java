package com.example.voicefinance;

import android.content.Context;
import android.content.SharedPreferences;

public class BudgetHelper {

    private static final String PREFS = "BudgetPrefs";
    private static final String KEY_MONTHLY_BUDGET = "monthly_budget";

    // Save monthly budget
    public static void setMonthlyBudget(Context context, double amount) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit()
                .putFloat(KEY_MONTHLY_BUDGET, (float) amount)
                .apply();
    }

    // Get monthly budget
    // RETURNS 0 IF NOT SET → FEATURE DISABLED
    public static double getMonthlyBudget(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        return prefs.getFloat(KEY_MONTHLY_BUDGET, 0f);
    }

    // Clear budget (optional action)
    public static void clearMonthlyBudget(Context context) {
        SharedPreferences prefs =
                context.getSharedPreferences(PREFS, Context.MODE_PRIVATE);
        prefs.edit().remove(KEY_MONTHLY_BUDGET).apply();
    }
}
