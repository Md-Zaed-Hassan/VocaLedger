package com.example.voicefinance;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "transactions")
public class Transaction {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public String label;
    public double amount;
    public long timestamp;

    // --- NEW FIELD ---
    public String category; // This adds the new column to our database

    // --- UPDATED CONSTRUCTOR ---
    public Transaction(String label, double amount, long timestamp, String category) {
        this.label = label;
        this.amount = amount;
        this.timestamp = timestamp;
        this.category = category; // Set the new category
    }
}