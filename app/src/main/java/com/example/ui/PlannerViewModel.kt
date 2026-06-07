package com.example.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.BuildConfig
import com.example.data.database.AppDatabase
import com.example.data.model.SchedulePlan
import com.example.data.model.Task
import com.example.data.repository.TaskRepository
import com.example.network.Content
import com.example.network.GenerateContentRequest
import com.example.network.Part
import com.example.network.RetrofitClient
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

sealed interface GenerationState {
    object Idle : GenerationState
    object Loading : GenerationState
    data class Success(val rawPlan: String, val blocks: List<ScheduleBlock>) : GenerationState
    data class Error(val message: String) : GenerationState
}

data class ScheduleBlock(
    val timeRange: String,
    val activity: String,
    val isBreak: Boolean
)

class PlannerViewModel(private val repository: TaskRepository) : ViewModel() {

    // Tasks and Schedule Flow from Database
    val allTasks: StateFlow<List<Task>> = repository.allTasks
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    val savedPlan: StateFlow<SchedulePlan?> = repository.schedulePlan
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = null
        )

    // User Input States
    private val _availableHours = MutableStateFlow(6)
    val availableHours: StateFlow<Int> = _availableHours.asStateFlow()

    private val _generationState = MutableStateFlow<GenerationState>(GenerationState.Idle)
    val generationState: StateFlow<GenerationState> = _generationState.asStateFlow()

    init {
        // Hydrate generation state when saved plan updates
        viewModelScope.launch {
            savedPlan.collect { plan ->
                if (plan != null) {
                    _availableHours.value = plan.availableHours
                    _generationState.value = GenerationState.Success(
                        rawPlan = plan.planText,
                        blocks = parsePlanText(plan.planText)
                    )
                }
            }
        }
    }

    fun setAvailableHours(hours: Int) {
        _availableHours.value = hours.coerceIn(1, 24)
    }

    // Task Interactions
    fun addTask(title: String, priority: String) {
        viewModelScope.launch {
            repository.insertTask(
                Task(
                    title = title,
                    priority = priority,
                    isCompleted = false
                )
            )
        }
    }

    fun toggleTaskCompletion(task: Task) {
        viewModelScope.launch {
            repository.updateTask(task.copy(isCompleted = !task.isCompleted))
        }
    }

    fun updateTaskDetails(task: Task, newTitle: String, newPriority: String) {
        viewModelScope.launch {
            repository.updateTask(task.copy(title = newTitle, priority = newPriority))
        }
    }

    fun deleteTask(task: Task) {
        viewModelScope.launch {
            repository.deleteTask(task)
        }
    }

    fun clearAllTasks() {
        viewModelScope.launch {
            repository.clearAllTasks()
            repository.clearSchedulePlan()
            _generationState.value = GenerationState.Idle
        }
    }

    // AI Plan Generation
    fun generateSchedule() {
        val tasks = allTasks.value
        val hours = _availableHours.value

        if (tasks.isEmpty()) {
            _generationState.value = GenerationState.Error("Please add at least one task first.")
            return
        }

        _generationState.value = GenerationState.Loading

        viewModelScope.launch {
            try {
                val apiKey = BuildConfig.GEMINI_API_KEY
                if (apiKey == "MY_GEMINI_API_KEY" || apiKey.isBlank()) {
                    // Friendly explanation if API key is not fully configured
                    _generationState.value = GenerationState.Error(
                        "Gemini API key is not yet configured in AI Studio secrets. " +
                                "Please add GEMINI_API_KEY inside the Secrets Panel to generate schedules."
                    )
                    return@launch
                }

                val taskDescription = tasks.joinToString("\n") { task ->
                    "- ${task.title} (Priority: ${task.priority}, Completed: ${if (task.isCompleted) "Yes" else "No"})"
                }

                val systemInstruction = """
                    You are DayFlow, a highly efficient, calm, and professional daily productivity scheduler.
                    Your objective is to create a realistic and balanced schedule based on user tasks and available hours.
                    
                    Format your output EXACTLY as a list of lines, with each line in the following visual format:
                    Time Range | Activity Description
                    
                    Example:
                    09:00 AM - 10:30 AM | Solve 3 DSA Problems (High Priority)
                    10:30 AM - 10:45 AM | Short Coffee Break & Stretching
                    10:45 AM - 12:15 PM | Learn React Core Concepts
                    12:15 PM - 01:00 PM | Portfolio Project Details
                    06:00 PM - 07:00 PM | Evening Gym Session
                    
                    Strict Rules:
                    - Only output one item per block per line.
                    - Start with the time block, followed by '|', then the activity name with brief priority tag.
                    - Prioritize the 'High' priority tasks first, then 'Medium', then 'Low'.
                    - The total duration of task activities (excluding breaks) should equal or be close to the available hours: $hours hours.
                    - Keep it balanced, include periodic short breaks of 10-15 minutes.
                    - Do not include any headers, footers, conversational intros, or standard markdown headers. Just list the lines.
                """.trimIndent()

                val prompt = """
                    I have $hours available hours today. Here are my tasks for today:
                    $taskDescription
                    
                    Create my structured schedule plan.
                """.trimIndent()

                val request = GenerateContentRequest(
                    contents = listOf(Content(parts = listOf(Part(text = prompt)))),
                    systemInstruction = Content(parts = listOf(Part(text = systemInstruction)))
                )

                val response = RetrofitClient.service.generateContent(
                    model = "gemini-3.5-flash",
                    apiKey = apiKey,
                    request = request
                )

                val text = response.candidates?.firstOrNull()?.content?.parts?.firstOrNull()?.text
                if (text != null && text.isNotBlank()) {
                    // Save to Room DB for persistence
                    val plan = SchedulePlan(
                        availableHours = hours,
                        planText = text
                    )
                    repository.saveSchedulePlan(plan)
                    
                    _generationState.value = GenerationState.Success(
                        rawPlan = text,
                        blocks = parsePlanText(text)
                    )
                } else {
                    _generationState.value = GenerationState.Error("Received empty response from Gemini API. Please try again.")
                }

            } catch (e: Exception) {
                _generationState.value = GenerationState.Error("Generation failed: ${e.localizedMessage ?: "Unknown error"}")
            }
        }
    }

    private fun parsePlanText(text: String): List<ScheduleBlock> {
        val blocks = mutableListOf<ScheduleBlock>()
        val lines = text.split("\n")
        for (line in lines) {
            val trimmed = line.trim()
            if (trimmed.isEmpty()) continue
            if (trimmed.contains("|")) {
                val parts = trimmed.split("|")
                val rawTime = parts[0].trim().replace("^[-*#\\s+]+".toRegex(), "")
                val rawActivity = if (parts.size > 1) parts[1].trim() else ""
                if (rawTime.isNotEmpty() && rawActivity.isNotEmpty()) {
                    val isBreak = rawActivity.contains("break", ignoreCase = true) ||
                            rawActivity.contains("rest", ignoreCase = true) ||
                            rawActivity.contains("coffee", ignoreCase = true) ||
                            rawActivity.contains("stretch", ignoreCase = true)
                    blocks.add(ScheduleBlock(rawTime, rawActivity, isBreak))
                }
            } else {
                // Try simple space/hyphen separation or display full line
                if (trimmed.length > 5 && (trimmed.contains("-") || trimmed.contains("AM") || trimmed.contains("PM"))) {
                    val isBreak = trimmed.contains("break", ignoreCase = true) ||
                            trimmed.contains("rest", ignoreCase = true) ||
                            trimmed.contains("coffee", ignoreCase = true)
                    blocks.add(ScheduleBlock("Activity Block", trimmed, isBreak))
                }
            }
        }
        return blocks
    }

    // Factory Class
    companion object {
        fun provideFactory(context: android.content.Context): ViewModelProvider.Factory {
            return object : ViewModelProvider.Factory {
                override fun <T : ViewModel> create(modelClass: Class<T>): T {
                    val database = AppDatabase.getDatabase(context)
                    val repository = TaskRepository(database.taskDao())
                    @Suppress("UNCHECKED_CAST")
                    return PlannerViewModel(repository) as T
                }
            }
        }
    }
}
