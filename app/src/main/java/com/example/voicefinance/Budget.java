package com.example.voicefinance;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "budget")
public class Budget {

    @PrimaryKey(autoGenerate = true)
    public int id;

    public double amount;        // Total budget
    public long startDate;       // millis
    public long endDate;         // millis
    public boolean active;       // only ONE allowed

    public Budget(double amount, long startDate, long endDate, boolean active) {
        this.amount = amount;
        this.startDate = startDate;
        this.endDate = endDate;
        this.active = active;
    }
}
