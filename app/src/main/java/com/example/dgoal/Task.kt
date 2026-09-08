package com.example.dgoal

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "tasks")
data class Task(
    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,
    val title: String,
    var completed: Boolean = false,
    var completedDays: MutableSet<Int> = mutableSetOf()
)