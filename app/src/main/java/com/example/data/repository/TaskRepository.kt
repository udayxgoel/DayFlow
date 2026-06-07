package com.example.data.repository

import com.example.data.database.TaskDao
import com.example.data.model.SchedulePlan
import com.example.data.model.Task
import kotlinx.coroutines.flow.Flow

class TaskRepository(private val taskDao: TaskDao) {
    val allTasks: Flow<List<Task>> = taskDao.getAllTasksFlow()
    val schedulePlan: Flow<SchedulePlan?> = taskDao.getSchedulePlanFlow()

    suspend fun insertTask(task: Task) {
        taskDao.insertTask(task)
    }

    suspend fun updateTask(task: Task) {
        taskDao.updateTask(task)
    }

    suspend fun deleteTask(task: Task) {
        taskDao.deleteTask(task)
    }

    suspend fun clearAllTasks() {
        taskDao.deleteAllTasks()
    }

    suspend fun saveSchedulePlan(plan: SchedulePlan) {
        taskDao.insertSchedulePlan(plan)
    }

    suspend fun clearSchedulePlan() {
        taskDao.deleteSchedulePlan()
    }
}
