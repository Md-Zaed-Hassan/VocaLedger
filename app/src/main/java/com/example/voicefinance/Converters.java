package com.example.voicefinance;

import androidx.room.TypeConverter;

public class Converters {
    @TypeConverter
    public static TransactionType fromString(String value) {
        return value == null ? null : TransactionType.valueOf(value);
    }

    @TypeConverter
    public static String toString(TransactionType value) {
        return value == null ? null : value.name();
    }
}
