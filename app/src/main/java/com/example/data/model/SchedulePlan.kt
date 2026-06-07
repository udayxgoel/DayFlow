package com.example.data.model

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "schedule_plans")
data class SchedulePlan(
    @PrimaryKey val id: Int = 1, // Store single plan for the day
    val availableHours: Int,
    val planText: String, // Markdowns/blocks representing schedule
    val updatedAt: Long = System.currentTimeMillis()
)
