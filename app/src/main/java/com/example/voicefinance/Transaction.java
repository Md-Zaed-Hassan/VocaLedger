package com.example.voicefinance;

import androidx.room.ColumnInfo;
import androidx.room.Entity;
import androidx.room.PrimaryKey;
import androidx.room.TypeConverters;

import org.jetbrains.annotations.Nullable;

@Entity(tableName = "transactions")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String label;
    public double amount;
    public long timestamp;


    public String category;

    @ColumnInfo(name = "created_at")
    public long createdAt;

    @ColumnInfo(name = "updated_at")
    @Nullable
    public Long updatedAt;

    @TypeConverters(Converters.class)
    public TransactionType type;

    public Transaction(String label, double amount, long timestamp, String category, TransactionType type) {
        this.label = label;
        this.amount = amount;
        this.timestamp = timestamp;
        this.createdAt = System.currentTimeMillis();
        this.category = category;
        this.type = type;
    }
}
