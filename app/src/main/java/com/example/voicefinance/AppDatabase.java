package com.example.voicefinance;

import android.content.Context;

import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import androidx.room.migration.Migration;
import androidx.sqlite.db.SupportSQLiteDatabase;
import androidx.annotation.NonNull;

@Database(entities = {Transaction.class, Budget.class}, version = 4)
 // Now version 4
public abstract class AppDatabase extends RoomDatabase {
    public abstract TransactionDao transactionDao();
    public abstract BudgetDao budgetDao();
    private static volatile AppDatabase INSTANCE;
    private static final int NUMBER_OF_THREADS = 4;

    static final ExecutorService databaseWriteExecutor =
            Executors.newFixedThreadPool(NUMBER_OF_THREADS);

    static AppDatabase getDatabase(final Context context) {
        if (INSTANCE == null) {
            synchronized (AppDatabase.class) {
                if (INSTANCE == null) {
                    INSTANCE = Room.databaseBuilder(context.getApplicationContext(),
                                    AppDatabase.class, "transaction_database")
                            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                            .build();
                }
            }
        }
        return INSTANCE;
    }
    static final Migration MIGRATION_2_3 = new Migration(2, 3) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase database) {

            boolean hasCreatedAt = false;
            boolean hasUpdatedAt = false;

            try (android.database.Cursor cursor =
                         database.query("PRAGMA table_info(transactions)")) {

                while (cursor.moveToNext()) {
                    String columnName = cursor.getString(
                            cursor.getColumnIndexOrThrow("name")
                    );
                    if ("created_at".equals(columnName)) {
                        hasCreatedAt = true;
                    }
                    if ("updated_at".equals(columnName)) {
                        hasUpdatedAt = true;
                    }
                }
            }

            if (!hasCreatedAt) {
                database.execSQL(
                        "ALTER TABLE transactions ADD COLUMN created_at INTEGER NOT NULL DEFAULT 0"
                );
            }

            if (!hasUpdatedAt) {
                database.execSQL(
                        "ALTER TABLE transactions ADD COLUMN updated_at INTEGER"
                );
            }
        }
    };
    static final Migration MIGRATION_3_4 = new Migration(3,4) {
        @Override
        public void migrate(@NonNull SupportSQLiteDatabase db) {
            db.execSQL(
                    "CREATE TABLE IF NOT EXISTS budget (" +
                            "id INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL," +
                            "amount REAL NOT NULL," +
                            "startDate INTEGER NOT NULL," +
                            "endDate INTEGER NOT NULL," +
                            "active INTEGER NOT NULL)"
            );
        }
    };


}