package com.example.data.database

import androidx.room.*
import com.example.data.model.SchedulePlan
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

@Dao
interface TaskDao {
    @Query("SELECT * FROM tasks ORDER BY createdAt DESC")
    fun getAllTasksFlow(): Flow<List<Task>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTask(task: Task)

    @Update
    suspend fun updateTask(task: Task)

    @Delete
    suspend fun deleteTask(task: Task)

    @Query("DELETE FROM tasks")
    suspend fun deleteAllTasks()

    // Plan persistence (Single record representation with key 1)
    @Query("SELECT * FROM schedule_plans WHERE id = 1 LIMIT 1")
    fun getSchedulePlanFlow(): Flow<SchedulePlan?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedulePlan(plan: SchedulePlan)

    @Query("DELETE FROM schedule_plans WHERE id = 1")
    suspend fun deleteSchedulePlan()
}
