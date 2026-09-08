package com.example.dgoal

import androidx.room.TypeConverter

class Converters {

    @TypeConverter
    fun fromCompletedDays(days: MutableSet<Int>): String {
        return days.joinToString(separator = ",")
    }

    @TypeConverter
    fun toCompletedDays(data: String): MutableSet<Int> {
        if (data.isEmpty()) {
            return mutableSetOf()
        }
        return data.split(",").map { it.toInt() }.toMutableSet()
    }
}