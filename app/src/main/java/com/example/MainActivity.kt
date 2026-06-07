package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.core.Spring
import com.example.data.model.Task
import com.example.ui.GenerationState
import com.example.ui.PlannerViewModel
import com.example.ui.ScheduleBlock
import com.example.ui.theme.DayFlowTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            DayFlowTheme {
                val context = LocalContext.current
                val viewModel: PlannerViewModel = androidx.lifecycle.viewmodel.compose.viewModel(
                    factory = PlannerViewModel.provideFactory(context)
                )

                val sharedPrefs = remember {
                    context.getSharedPreferences("dayflow_prefs", android.content.Context.MODE_PRIVATE)
                }
                var isOnboardingCompleted by remember {
                    mutableStateOf(sharedPrefs.getBoolean("onboarding_completed", false))
                }

                if (!isOnboardingCompleted) {
                    OnboardingScreen(
                        onFinished = {
                            sharedPrefs.edit().putBoolean("onboarding_completed", true).apply()
                            isOnboardingCompleted = true
                        }
                    )
                } else {
                    // Fill the full outer screen under status bars
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .frostedGlassBackground()
                    ) {
                        Scaffold(
                            modifier = Modifier.fillMaxSize(),
                            containerColor = Color.Transparent, // Let the beautiful base gradient shine through
                            contentWindowInsets = WindowInsets.safeDrawing
                        ) { innerPadding ->
                            PlannerScreen(
                                viewModel = viewModel,
                                modifier = Modifier.padding(innerPadding)
                            )
                        }
                    }
                }
            }
        }
    }
}

// Reusable modifiers for the beautiful Frosted Glass (Glassmorphic) Theme
@Composable
fun Modifier.glassCard(
    cornerRadius: Dp = 24.dp,
    borderAlpha: Float = 0.40f,
    bgAlpha: Float = 0.55f,
    darkTheme: Boolean = isSystemInDarkTheme()
): Modifier {
    val baseColor = if (darkTheme) Color(0xFF1E293B) else Color.White
    val borderColor = if (darkTheme) Color.White.copy(alpha = 0.15f) else Color.White.copy(alpha = borderAlpha)
    return this
        .clip(RoundedCornerShape(cornerRadius))
        .background(baseColor.copy(alpha = bgAlpha))
        .border(1.dp, borderColor, RoundedCornerShape(cornerRadius))
}

@Composable
fun Modifier.frostedGlassBackground(darkTheme: Boolean = isSystemInDarkTheme()): Modifier {
    val background = if (darkTheme) Color(0xFF090D16) else Color(0xFFF1F5F9)
    val primaryGlow = if (darkTheme) Color(0xFF1D4ED8) else Color(0xFF93C5FD)
    val accentGlow = if (darkTheme) Color(0xFF6D28D9) else Color(0xFFC7D2FE)
    val supplementaryGlow = if (darkTheme) Color(0xFF0F766E) else Color(0xFFA7F3D0)
    
    return this.drawBehind {
        // Draw primary back background
        drawRect(color = background)
        
        // Draw beautifully positioned ambient blur orb gradients
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(primaryGlow.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.15f, size.height * 0.2f),
                radius = size.width * 0.75f
            ),
            radius = size.width * 0.75f,
            center = Offset(size.width * 0.15f, size.height * 0.2f)
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(accentGlow.copy(alpha = 0.18f), Color.Transparent),
                center = Offset(size.width * 0.85f, size.height * 0.65f),
                radius = size.width * 0.85f
            ),
            radius = size.width * 0.85f,
            center = Offset(size.width * 0.85f, size.height * 0.65f)
        )
        
        drawCircle(
            brush = Brush.radialGradient(
                colors = listOf(supplementaryGlow.copy(alpha = 0.15f), Color.Transparent),
                center = Offset(size.width * 0.35f, size.height * 0.95f),
                radius = size.width * 0.6f
            ),
            radius = size.width * 0.6f,
            center = Offset(size.width * 0.35f, size.height * 0.95f)
        )
    }
}

@Composable
fun PlannerScreen(
    viewModel: PlannerViewModel,
    modifier: Modifier = Modifier
) {
    val tasks by viewModel.allTasks.collectAsStateWithLifecycle()
    val availableHours by viewModel.availableHours.collectAsStateWithLifecycle()
    val generationState by viewModel.generationState.collectAsStateWithLifecycle()

    var taskInput by remember { mutableStateOf("") }
    var selectedPriority by remember { mutableStateOf("Medium") }
    var editingTask by remember { mutableStateOf<Task?>(null) }
    
    val completedBlocks = remember { mutableStateMapOf<String, Boolean>() }

    if (editingTask != null) {
        EditTaskDialog(
            task = editingTask!!,
            onDismiss = { editingTask = null },
            onSave = { title, priority ->
                viewModel.updateTaskDetails(editingTask!!, title, priority)
            }
        )
    }

    Column(
        modifier = modifier.fillMaxSize()
    ) {
        // App Header Section beautifully styled in glass style
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(40.dp)
                        .clip(RoundedCornerShape(12.dp))
                        .background(MaterialTheme.colorScheme.primary),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Filled.DateRange,
                        contentDescription = "DayFlow logo",
                        tint = Color.White,
                        modifier = Modifier.size(22.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "DayFlow",
                        fontSize = 21.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "AI-Powered Daily Planner",
                        fontSize = 11.sp,
                        color = MaterialTheme.colorScheme.secondary,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }

            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Clear Day Action button (New Day)
                if (tasks.isNotEmpty()) {
                    IconButton(
                        onClick = { viewModel.clearAllTasks() },
                        modifier = Modifier
                            .size(38.dp)
                            .background(
                                MaterialTheme.colorScheme.error.copy(alpha = 0.12f),
                                CircleShape
                            )
                            .testTag("clear_all_button")
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Clear",
                            tint = MaterialTheme.colorScheme.error,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }

                // Profile initials icon (represented in the Frosted Glass theme layout)
                Box(
                    modifier = Modifier
                        .size(38.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "JD",
                        color = MaterialTheme.colorScheme.primary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }

        // Dividers with glass opacity
        HorizontalDivider(color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f), thickness = 1.dp)

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f),
            contentPadding = PaddingValues(bottom = 32.dp)
        ) {
            // Hours & Task Inputs Section
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Available Hours Container (Glass Style)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 24.dp, bgAlpha = 0.45f)
                            .padding(18.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = "Available Hours Today",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 15.sp,
                                    color = MaterialTheme.colorScheme.onBackground
                                )
                                Text(
                                    text = "Allocated daily workspace hours",
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.secondary,
                                    fontWeight = FontWeight.Medium
                                )
                            }
                            
                            // Fine custom UI glass control pill
                            Row(
                                modifier = Modifier
                                    .background(
                                        color = if (isSystemInDarkTheme()) Color(0xFF1E293B).copy(alpha = 0.9f) else Color.White,
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(100.dp)
                                    )
                                    .padding(horizontal = 6.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(4.dp)
                            ) {
                                IconButton(
                                    onClick = { viewModel.setAvailableHours(availableHours - 1) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                            CircleShape
                                        )
                                        .testTag("decrement_hours_button")
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .width(8.dp)
                                            .height(2.dp)
                                            .background(MaterialTheme.colorScheme.onSurface)
                                    )
                                }

                                Text(
                                    text = "$availableHours",
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.widthIn(min = 22.dp),
                                    textAlign = TextAlign.Center
                                )

                                IconButton(
                                    onClick = { viewModel.setAvailableHours(availableHours + 1) },
                                    modifier = Modifier
                                        .size(32.dp)
                                        .background(
                                            MaterialTheme.colorScheme.onBackground.copy(alpha = 0.05f),
                                            CircleShape
                                        )
                                        .testTag("increment_hours_button")
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.Add,
                                        contentDescription = "Increase hours",
                                        tint = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.size(16.dp)
                                    )
                                }
                            }
                        }
                    }

                    // Add Task Input Glass Block (high-fidelity design)
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .glassCard(cornerRadius = 24.dp, bgAlpha = 0.5f)
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        Text(
                            text = "Add Daily Focus Task",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        // Translucent styled search/input bar
                        OutlinedTextField(
                            value = taskInput,
                            onValueChange = { taskInput = it },
                            placeholder = { Text("What needs to get done?", fontSize = 13.sp, color = MaterialTheme.colorScheme.secondary.copy(alpha = 0.7f)) },
                            singleLine = true,
                            modifier = Modifier
                                .fillMaxWidth()
                                .testTag("task_input_field"),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF0F172A).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.8f),
                                unfocusedContainerColor = if (isSystemInDarkTheme()) Color(0xFF0F172A).copy(alpha = 0.3f) else Color.White.copy(alpha = 0.5f),
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.12f),
                                focusedTextColor = MaterialTheme.colorScheme.onBackground,
                                unfocusedTextColor = MaterialTheme.colorScheme.onBackground
                            ),
                            shape = RoundedCornerShape(16.dp)
                        )

                        // Saturated beautiful priority tags
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = "Preference Priority",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.secondary
                            )

                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                listOf("High", "Medium", "Low").forEach { p ->
                                    val isSelected = selectedPriority == p
                                    val chipColor = when (p) {
                                        "High" -> Color(0xFFEF4444)
                                        "Medium" -> Color(0xFFF59E0B)
                                        else -> Color(0xFF3B82F6)
                                    }

                                    Box(
                                        modifier = Modifier
                                            .clip(RoundedCornerShape(20.dp))
                                            .background(
                                                if (isSelected) chipColor.copy(alpha = 0.15f)
                                                else Color.Transparent
                                            )
                                            .border(
                                                width = 1.dp,
                                                color = if (isSelected) chipColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                                shape = RoundedCornerShape(20.dp)
                                            )
                                            .clickable { selectedPriority = p }
                                            .padding(horizontal = 14.dp, vertical = 7.dp)
                                            .testTag("priority_chip_$p")
                                    ) {
                                        Text(
                                            text = p.uppercase(),
                                            fontSize = 9.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            color = if (isSelected) chipColor else MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                        }

                        // Sparkly customized primary glass action button
                        Button(
                            onClick = {
                                if (taskInput.trim().isNotBlank()) {
                                    viewModel.addTask(taskInput.trim(), selectedPriority)
                                    taskInput = ""
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(46.dp)
                                .testTag("add_task_button"),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            shape = RoundedCornerShape(14.dp)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = "Add", modifier = Modifier.size(16.dp))
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Add Task To Focus", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }
            }

            // Task Progress Section Card
            item {
                val totalTasks = tasks.size
                val completedTasks = tasks.count { it.isCompleted }
                val progress = if (totalTasks > 0) completedTasks.toFloat() / totalTasks.toFloat() else 0f
                val animatedProgress by animateFloatAsState(
                    targetValue = progress,
                    animationSpec = tween(500),
                    label = "TaskProgressBarProgress"
                )

                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp)
                        .glassCard(cornerRadius = 24.dp, bgAlpha = 0.45f)
                        .testTag("task_progress_card")
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(20.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Text(
                            text = "Task Progress",
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            color = MaterialTheme.colorScheme.onBackground
                        )

                        if (totalTasks == 0) {
                            Text(
                                text = "No tasks added yet",
                                fontSize = 13.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                fontWeight = FontWeight.Medium,
                                modifier = Modifier.testTag("no_tasks_progress_text")
                            )
                        } else {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "$completedTasks of $totalTasks tasks completed",
                                    fontSize = 13.sp,
                                    color = MaterialTheme.colorScheme.onBackground,
                                    fontWeight = FontWeight.Medium,
                                    modifier = Modifier.testTag("tasks_completed_count_text")
                                )
                                Text(
                                    text = "${(progress * 100).toInt()}%",
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.testTag("tasks_completed_percentage_text")
                                )
                            }

                            // Custom animated/styled Progress Bar
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(100.dp))
                                    .background(MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f))
                                    .testTag("progress_bar_background")
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxHeight()
                                        .fillMaxWidth(animatedProgress)
                                        .clip(RoundedCornerShape(100.dp))
                                        .background(
                                            Brush.horizontalGradient(
                                                colors = listOf(
                                                    MaterialTheme.colorScheme.primary,
                                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.8f)
                                                )
                                            )
                                        )
                                        .testTag("progress_bar_indicator")
                                )
                            }
                        }
                    }
                }
            }

            // Task List Header
            item {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "STUDIO FOCUS LIST",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 1.sp,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Box(
                        modifier = Modifier
                            .background(
                                MaterialTheme.colorScheme.primary.copy(alpha = 0.12f),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = "${tasks.size} Items",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
            }

            // Empty state placeholder (Glass panel guide)
            if (tasks.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 20.dp, vertical = 12.dp)
                            .glassCard(cornerRadius = 24.dp, bgAlpha = 0.35f)
                            .padding(32.dp)
                    ) {
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(10.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = "No tasks",
                                tint = MaterialTheme.colorScheme.secondary.copy(alpha = 0.5f),
                                modifier = Modifier.size(40.dp)
                            )
                            Text(
                                text = "Start your day with intent.",
                                fontSize = 15.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            Text(
                                text = "Add focus objects and we will generate a beautiful day plan balanced around breathers.",
                                fontSize = 11.sp,
                                color = MaterialTheme.colorScheme.secondary,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(horizontal = 12.dp)
                            )
                        }
                    }
                }
            } else {
                items(tasks, key = { it.id }) { task ->
                    TaskItemRow(
                        task = task,
                        onToggle = { viewModel.toggleTaskCompletion(task) },
                        onDelete = { viewModel.deleteTask(task) },
                        onEditClick = { editingTask = task }
                    )
                }
            }

            // Schedule Builder CTA Button & AI Schedule Results output
            item {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 20.dp),
                    verticalArrangement = Arrangement.spacedBy(20.dp)
                ) {
                    // Generate button
                    Button(
                        onClick = { viewModel.generateSchedule() },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp)
                            .testTag("generate_schedule_button"),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        ),
                        shape = RoundedCornerShape(16.dp)
                    ) {
                        Icon(Icons.Filled.Star, contentDescription = "AI", modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Generate My Day",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    // Display generation layouts
                    AnimatedContent(
                        targetState = generationState,
                        transitionSpec = {
                            fadeIn() togetherWith fadeOut()
                        },
                        label = "ScheduleState"
                    ) { state ->
                        when (state) {
                            is GenerationState.Idle -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassCard(cornerRadius = 24.dp, bgAlpha = 0.35f)
                                        .padding(24.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(6.dp)
                                    ) {
                                        Icon(
                                            Icons.Filled.Star,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.3f),
                                            modifier = Modifier.size(28.dp)
                                        )
                                        Text(
                                            text = "Ready to build your roadmap?",
                                            fontWeight = FontWeight.SemiBold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            is GenerationState.Loading -> {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .glassCard(cornerRadius = 24.dp, bgAlpha = 0.45f)
                                        .padding(32.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Column(
                                        horizontalAlignment = Alignment.CenterHorizontally,
                                        verticalArrangement = Arrangement.spacedBy(14.dp)
                                    ) {
                                        CircularProgressIndicator(strokeWidth = 3.dp, color = MaterialTheme.colorScheme.primary)
                                        Text(
                                            text = "Intelligence engine is forming your day...",
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp,
                                            color = MaterialTheme.colorScheme.secondary
                                        )
                                    }
                                }
                            }
                            is GenerationState.Success -> {
                                Column(verticalArrangement = Arrangement.spacedBy(14.dp)) {
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Filled.DateRange,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(8.dp))
                                        Text(
                                            text = "YOUR INTENTIONAL ROADMAP",
                                            fontSize = 11.sp,
                                            fontWeight = FontWeight.ExtraBold,
                                            letterSpacing = 1.sp,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }

                                    // Custom dark space background timeline sheet to preserve Frosted Glass look in theSuccess layout
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = Color(0xFF0F172A)), // Custom dark space panel
                                        shape = RoundedCornerShape(24.dp),
                                        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
                                        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f))
                                    ) {
                                        // Drawing neon space glows inside Success container
                                        Column(
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .drawBehind {
                                                    drawCircle(
                                                        brush = Brush.radialGradient(
                                                            colors = listOf(Color(0xFF2563EB).copy(alpha = 0.15f), Color.Transparent),
                                                            center = Offset(size.width * 0.95f, size.height * 0.05f),
                                                            radius = size.width * 0.5f
                                                        ),
                                                        radius = size.width * 0.5f,
                                                        center = Offset(size.width * 0.95f, size.height * 0.05f)
                                                    )
                                                }
                                                .padding(20.dp),
                                            verticalArrangement = Arrangement.spacedBy(0.dp)
                                        ) {
                                            state.blocks.forEachIndexed { index, block ->
                                                TimelineBlockRow(
                                                    block = block,
                                                    isLast = index == state.blocks.size - 1,
                                                    isCompleted = completedBlocks[block.timeRange + block.activity] == true,
                                                    onToggle = {
                                                        val key = block.timeRange + block.activity
                                                        completedBlocks[key] = !(completedBlocks[key] ?: false)
                                                    }
                                                )
                                            }
                                        }
                                    }

                                    // Proactive layout for warning (sleek red warning box)
                                    Card(
                                        modifier = Modifier.fillMaxWidth(),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.10f)),
                                        shape = RoundedCornerShape(16.dp),
                                        border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.2f))
                                    ) {
                                        Row(
                                            modifier = Modifier.padding(14.dp),
                                            verticalAlignment = Alignment.Top
                                        ) {
                                            Icon(
                                                Icons.Default.Warning,
                                                contentDescription = "Warning",
                                                tint = MaterialTheme.colorScheme.error,
                                                modifier = Modifier.size(16.dp).offset(y = 1.dp)
                                            )
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Security Warning: I have included your API keys in the generated APK file for this prototype. Please be aware that Android APKs can be easily decompiled, and these keys can be extracted by anyone who has access to the file. Do not share this APK file publicly or with unauthorized individuals to prevent potential misuse.",
                                                fontSize = 10.sp,
                                                fontWeight = FontWeight.Medium,
                                                color = MaterialTheme.colorScheme.error,
                                                lineHeight = 14.sp
                                            )
                                        }
                                    }
                                }
                            }
                            is GenerationState.Error -> {
                                Card(
                                    modifier = Modifier.fillMaxWidth(),
                                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.25f)),
                                    shape = RoundedCornerShape(16.dp),
                                    border = BorderStroke(1.dp, MaterialTheme.colorScheme.error.copy(alpha = 0.25f))
                                ) {
                                    Column(
                                        modifier = Modifier.padding(16.dp),
                                        verticalArrangement = Arrangement.spacedBy(4.dp)
                                    ) {
                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            Icon(Icons.Default.Warning, contentDescription = "Error", tint = MaterialTheme.colorScheme.error, modifier = Modifier.size(20.dp))
                                            Spacer(modifier = Modifier.width(8.dp))
                                            Text(
                                                text = "Could Not Formulate Roadmap",
                                                fontWeight = FontWeight.Bold,
                                                fontSize = 13.sp,
                                                color = MaterialTheme.colorScheme.error
                                            )
                                        }
                                        Text(
                                            text = state.message,
                                            fontSize = 11.sp,
                                            color = MaterialTheme.colorScheme.error.copy(alpha = 0.85f),
                                            lineHeight = 15.sp
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun TaskItemRow(
    task: Task,
    onToggle: () -> Unit,
    onDelete: () -> Unit,
    onEditClick: () -> Unit
) {
    val tagColor = when (task.priority) {
        "High" -> Color(0xFFEF4444)
        "Medium" -> Color(0xFFF59E0B)
        else -> Color(0xFF3B82F6)
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 6.dp)
            .glassCard(cornerRadius = 18.dp, bgAlpha = 0.45f)
            .testTag("task_item_${task.title}")
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Row(
                modifier = Modifier.weight(1f),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Customized modern checkmark container box
                Box(
                    modifier = Modifier
                        .size(24.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(
                            if (task.isCompleted) MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            else Color.Transparent
                        )
                        .border(
                            width = 2.dp,
                            color = if (task.isCompleted) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.15f),
                            shape = RoundedCornerShape(6.dp)
                        )
                        .clickable { onToggle() }
                        .testTag("checkbox_${task.id}"),
                    contentAlignment = Alignment.Center
                ) {
                    if (task.isCompleted) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = "Completed",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }
                
                Spacer(modifier = Modifier.width(14.dp))
                
                Column {
                    Text(
                        text = task.title,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (task.isCompleted) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onBackground,
                        textDecoration = if (task.isCompleted) TextDecoration.LineThrough else null,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Box(
                        modifier = Modifier
                            .background(tagColor.copy(alpha = 0.12f), RoundedCornerShape(6.dp))
                            .padding(horizontal = 7.dp, vertical = 3.dp)
                    ) {
                        Text(
                            text = task.priority.uppercase() + " PRIORITY",
                            fontSize = 8.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = tagColor,
                            letterSpacing = 0.5.sp
                        )
                    }
                }
            }

            Row(horizontalArrangement = Arrangement.spacedBy(4.dp)) {
                IconButton(
                    onClick = onEditClick,
                    modifier = Modifier.size(34.dp).testTag("edit_button_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = "Edit Task",
                        tint = MaterialTheme.colorScheme.secondary,
                        modifier = Modifier.size(15.dp)
                    )
                }

                IconButton(
                    onClick = onDelete,
                    modifier = Modifier.size(34.dp).testTag("delete_button_${task.id}")
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Delete Task",
                        tint = MaterialTheme.colorScheme.error.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                }
            }
        }
    }
}

@Composable
fun TimelineBlockRow(
    block: ScheduleBlock,
    isLast: Boolean,
    isCompleted: Boolean,
    onToggle: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onToggle() }
            .testTag("timeline_block_${block.activity}"),
        verticalAlignment = Alignment.Top
    ) {
        Box(
            modifier = Modifier
                .width(100.dp)
                .padding(vertical = 12.dp)
        ) {
            Text(
                text = block.timeRange,
                fontSize = 11.sp,
                color = if (isCompleted) Color.White.copy(alpha = 0.35f) else Color.White.copy(alpha = 0.6f),
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Start,
                maxLines = 2,
                lineHeight = 15.sp,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null
            )
        }

        Column(
            modifier = Modifier
                .width(36.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Spacer(modifier = Modifier.height(14.dp))
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .clip(CircleShape)
                    .background(
                        if (isCompleted) Color(0xFF22C55E)
                        else if (block.isBreak) Color.White.copy(alpha = 0.3f)
                        else Color(0xFF2563EB)
                    )
            )

            if (!isLast) {
                Box(
                    modifier = Modifier
                        .width(2.dp)
                        .height(38.dp)
                        .background(Color.White.copy(alpha = 0.12f))
                )
            } else {
                Spacer(modifier = Modifier.height(20.dp))
            }
        }

        Column(
            modifier = Modifier
                .weight(1f)
                .padding(vertical = 10.dp, horizontal = 4.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = block.activity,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = if (isCompleted) Color.White.copy(alpha = 0.35f) else Color.White,
                textDecoration = if (isCompleted) TextDecoration.LineThrough else null,
                lineHeight = 17.sp
            )

            if (block.isBreak) {
                Box(
                    modifier = Modifier
                        .background(
                            Color.White.copy(alpha = 0.08f),
                            RoundedCornerShape(4.dp)
                        )
                        .padding(horizontal = 6.dp, vertical = 2.dp)
                ) {
                    Text(
                        text = "BREATHER",
                        fontSize = 8.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = Color(0xFF22C55E),
                        letterSpacing = 0.5.sp
                    )
                }
            }
        }
    }
}

@Composable
fun EditTaskDialog(
    task: Task,
    onDismiss: () -> Unit,
    onSave: (String, String) -> Unit
) {
    var title by remember { mutableStateOf(task.title) }
    var priority by remember { mutableStateOf(task.priority) }

    Dialog(onDismissRequest = onDismiss) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            shape = RoundedCornerShape(24.dp),
            colors = CardDefaults.cardColors(containerColor = if (isSystemInDarkTheme()) Color(0xFF1E293B) else Color.White),
            elevation = CardDefaults.cardElevation(defaultElevation = 12.dp),
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.15f))
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Edit Focus Task",
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp,
                    color = MaterialTheme.colorScheme.onBackground
                )

                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Task Title", fontSize = 13.sp) },
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("edit_task_input"),
                    shape = RoundedCornerShape(12.dp)
                )

                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        text = "Priority Level",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.secondary
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf("High", "Medium", "Low").forEach { p ->
                            val isSelected = priority == p
                            val chipColor = when (p) {
                                "High" -> Color(0xFFEF4444)
                                "Medium" -> Color(0xFFF59E0B)
                                else -> Color(0xFF3B82F6)
                            }
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(
                                        if (isSelected) chipColor.copy(alpha = 0.15f)
                                        else MaterialTheme.colorScheme.background
                                    )
                                    .border(
                                        width = 1.dp,
                                        color = if (isSelected) chipColor else MaterialTheme.colorScheme.onBackground.copy(alpha = 0.08f),
                                        shape = RoundedCornerShape(8.dp)
                                    )
                                    .clickable { priority = p }
                                    .padding(vertical = 10.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = p,
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) chipColor else MaterialTheme.colorScheme.secondary
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel", fontSize = 14.sp)
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                    Button(
                        onClick = {
                            if (title.trim().isNotBlank()) {
                                onSave(title.trim(), priority)
                                onDismiss()
                            }
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = MaterialTheme.colorScheme.primary),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        Text("Save Changes", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                }
            }
        }
    }
}

// Onboarding Data and Screen implementations
data class OnboardingPageData(
    val title: String,
    val subtitle: String
)

val onboardingPages = listOf(
    OnboardingPageData(
        title = "Welcome to DayFlow",
        subtitle = "Turn your tasks into a clear plan for the day."
    ),
    OnboardingPageData(
        title = "Add Your Tasks",
        subtitle = "Create your todo list and organize your priorities."
    ),
    OnboardingPageData(
        title = "Let AI Plan Your Day",
        subtitle = "AI generates a realistic schedule based on your tasks and available time."
    ),
    OnboardingPageData(
        title = "Stay Focused",
        subtitle = "Follow your personalized schedule and achieve more every day."
    )
)

@Composable
fun OnboardingScreen(
    onFinished: () -> Unit,
    modifier: Modifier = Modifier
) {
    var currentPage by remember { mutableStateOf(0) }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.White),
        contentAlignment = Alignment.Center
    ) {
        Scaffold(
            modifier = Modifier
                .fillMaxSize()
                .widthIn(max = 450.dp),
            containerColor = Color.White,
            contentWindowInsets = WindowInsets.safeDrawing
        ) { innerPadding ->
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding)
                    .background(Color.White),
                verticalArrangement = Arrangement.SpaceBetween,
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Top Skip Bar with generous spacing
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 12.dp),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (currentPage < 3) {
                        TextButton(
                            onClick = onFinished,
                            modifier = Modifier.testTag("skip_onboarding_button")
                        ) {
                            Text(
                                text = "Skip",
                                color = Color(0xFF64748B),
                                fontSize = 15.sp,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    } else {
                        Spacer(modifier = Modifier.height(38.dp))
                    }
                }

                // Centered visual & copy slides
                Column(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    AnimatedContent(
                        targetState = currentPage,
                        transitionSpec = {
                            if (targetState > initialState) {
                                (slideInHorizontally { width -> width } + fadeIn()) togetherWith
                                        (slideOutHorizontally { width -> -width } + fadeOut())
                            } else {
                                (slideInHorizontally { width -> -width } + fadeIn()) togetherWith
                                        (slideOutHorizontally { width -> width } + fadeOut())
                            }.using(SizeTransform(clip = false))
                        },
                        label = "OnboardingPageAnimation"
                    ) { index ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            verticalArrangement = Arrangement.spacedBy(36.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            OnboardingIllustration(index = index)

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.spacedBy(12.dp),
                                modifier = Modifier.padding(horizontal = 16.dp)
                            ) {
                                Text(
                                    text = onboardingPages[index].title,
                                    fontSize = 26.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFF0F172A),
                                    textAlign = TextAlign.Center,
                                    letterSpacing = (-0.5).sp
                                )
                                Text(
                                    text = onboardingPages[index].subtitle,
                                    fontSize = 15.sp,
                                    fontWeight = FontWeight.Normal,
                                    color = Color(0xFF475569),
                                    textAlign = TextAlign.Center,
                                    lineHeight = 22.sp
                                )
                            }
                        }
                    }
                }

                // Bottom Page Indicators & Action Buttons
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(24.dp)
                ) {
                    // Smooth indicator dots
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        for (i in 0..3) {
                            val isSelected = i == currentPage
                            val width by animateDpAsState(
                                targetValue = if (isSelected) 24.dp else 8.dp,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioMediumBouncy),
                                label = "OnboardingIndicatorWidth"
                            )
                            val alpha by animateFloatAsState(
                                targetValue = if (isSelected) 1f else 0.35f,
                                animationSpec = tween(300),
                                label = "OnboardingIndicatorAlpha"
                            )

                            Box(
                                modifier = Modifier
                                    .width(width)
                                    .height(8.dp)
                                    .clip(CircleShape)
                                    .background(Color(0xFF2563EB).copy(alpha = alpha))
                                    .clickable { currentPage = i }
                                    .testTag("onboarding_dot_$i")
                            )
                        }
                    }

                    // Large tactile Button
                    if (currentPage == 3) {
                        Button(
                            onClick = onFinished,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("get_started_onboarding_button")
                        ) {
                            Text(
                                text = "Get Started",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    } else {
                        Button(
                            onClick = { currentPage++ },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF2563EB),
                                contentColor = Color.White
                            ),
                            shape = RoundedCornerShape(16.dp),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(54.dp)
                                .testTag("next_onboarding_button")
                        ) {
                            Text(
                                text = "Next",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
fun OnboardingIllustration(index: Int, modifier: Modifier = Modifier) {
    when (index) {
        0 -> {
            Box(
                modifier = modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .border(2.dp, Color(0xFF2563EB).copy(alpha = 0.15f), CircleShape)
                        .background(Color(0xFF2563EB).copy(alpha = 0.03f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(120.dp)
                            .drawBehind {
                                drawArc(
                                    color = Color(0xFF2563EB).copy(alpha = 0.3f),
                                    startAngle = -90f,
                                    sweepAngle = 360f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 1.5.dp.toPx()
                                    )
                                )
                                drawArc(
                                    color = Color(0xFF2563EB),
                                    startAngle = -90f,
                                    sweepAngle = 135f,
                                    useCenter = false,
                                    style = androidx.compose.ui.graphics.drawscope.Stroke(
                                        width = 4.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                )
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(60.dp)
                                .drawBehind {
                                    drawLine(
                                        color = Color(0xFF2563EB).copy(alpha = 0.7f),
                                        start = center,
                                        end = Offset(center.x + 14.dp.toPx(), center.y - 14.dp.toPx()),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    drawLine(
                                        color = Color(0xFF2563EB),
                                        start = center,
                                        end = Offset(center.x, center.y + 22.dp.toPx()),
                                        strokeWidth = 3.dp.toPx(),
                                        cap = androidx.compose.ui.graphics.StrokeCap.Round
                                    )
                                    drawCircle(
                                        color = Color(0xFF2563EB),
                                        radius = 5.dp.toPx()
                                    )
                                    drawCircle(
                                        color = Color.White,
                                        radius = 2.dp.toPx()
                                    )
                                }
                        )
                    }
                }
            }
        }
        1 -> {
            Column(
                modifier = modifier
                    .width(220.dp)
                    .height(180.dp)
                    .background(Color(0xFFF8FAFC), RoundedCornerShape(18.dp))
                    .border(1.dp, Color(0xFFE2E8F0), RoundedCornerShape(18.dp))
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Text(
                    text = "TODAY'S CHORES",
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.sp,
                    color = Color(0xFF64748B)
                )

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .background(Color(0xFF2563EB).copy(alpha = 0.1f), RoundedCornerShape(5.dp))
                            .border(1.5.dp, Color(0xFF2563EB), RoundedCornerShape(5.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(11.dp)
                        )
                    }
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.width(90.dp).height(8.dp).background(Color(0xFF94A3B8), RoundedCornerShape(3.dp)))
                        Box(modifier = Modifier.width(40.dp).height(5.dp).background(Color(0xFFCBD5E1), RoundedCornerShape(1.5.dp)))
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.5.dp, Color(0xFF94A3B8).copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                        Box(modifier = Modifier.width(110.dp).height(8.dp).background(Color(0xFF0F172A), RoundedCornerShape(3.dp)))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFEF4444).copy(alpha = 0.12f), RoundedCornerShape(3.dp))
                                .padding(horizontal = 4.dp, vertical = 2.dp)
                        ) {
                            Text("HIGH", fontSize = 6.sp, fontWeight = FontWeight.Bold, color = Color(0xFFEF4444))
                        }
                    }
                }

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(18.dp)
                            .border(1.5.dp, Color(0xFF94A3B8).copy(alpha = 0.4f), RoundedCornerShape(5.dp))
                    )
                    Box(modifier = Modifier.width(75.dp).height(8.dp).background(Color(0xFF475569), RoundedCornerShape(3.dp)))
                }
            }
        }
        2 -> {
            Box(
                modifier = modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(160.dp)
                        .drawBehind {
                            val centerOffset = center
                            val node1 = Offset(center.x - 45.dp.toPx(), center.y - 30.dp.toPx())
                            val node2 = Offset(center.x + 45.dp.toPx(), center.y - 40.dp.toPx())
                            val node3 = Offset(center.x - 40.dp.toPx(), center.y + 35.dp.toPx())
                            val node4 = Offset(center.x + 40.dp.toPx(), center.y + 30.dp.toPx())

                            drawLine(Color(0xFF2563EB).copy(alpha = 0.2f), centerOffset, node1, strokeWidth = 1.5.dp.toPx())
                            drawLine(Color(0xFF2563EB).copy(alpha = 0.2f), centerOffset, node2, strokeWidth = 1.5.dp.toPx())
                            drawLine(Color(0xFF2563EB).copy(alpha = 0.2f), centerOffset, node3, strokeWidth = 1.5.dp.toPx())
                            drawLine(Color(0xFF2563EB).copy(alpha = 0.2f), centerOffset, node4, strokeWidth = 1.5.dp.toPx())

                            drawCircle(Color(0xFF2563EB).copy(alpha = 0.15f), radius = 9.dp.toPx(), center = node1)
                            drawCircle(Color(0xFF2563EB), radius = 3.5.dp.toPx(), center = node1)

                            drawCircle(Color(0xFF3B82F6).copy(alpha = 0.15f), radius = 8.dp.toPx(), center = node2)
                            drawCircle(Color(0xFF3B82F6), radius = 3.3.dp.toPx(), center = node2)

                            drawCircle(Color(0xFF22C55E).copy(alpha = 0.15f), radius = 10.dp.toPx(), center = node3)
                            drawCircle(Color(0xFF22C55E), radius = 4.dp.toPx(), center = node3)

                            drawCircle(Color(0xFFF59E0B).copy(alpha = 0.15f), radius = 8.dp.toPx(), center = node4)
                            drawCircle(Color(0xFFF59E0B), radius = 3.dp.toPx(), center = node4)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color(0xFF2563EB).copy(alpha = 0.08f), CircleShape)
                            .border(1.dp, Color(0xFF2563EB).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Filled.Star,
                            contentDescription = null,
                            tint = Color(0xFF2563EB),
                            modifier = Modifier.size(24.dp)
                        )
                    }
                }
            }
        }
        else -> {
            Box(
                modifier = modifier
                    .size(200.dp),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    modifier = Modifier
                        .size(150.dp)
                        .drawBehind {
                            drawCircle(
                                color = Color(0xFF2563EB).copy(alpha = 0.15f),
                                radius = size.width / 2,
                                style = androidx.compose.ui.graphics.drawscope.Stroke(
                                    width = 2.dp.toPx(),
                                    pathEffect = androidx.compose.ui.graphics.PathEffect.dashPathEffect(
                                        floatArrayOf(12f, 12f),
                                        0f
                                    )
                                )
                            )
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        modifier = Modifier
                            .size(100.dp)
                            .background(
                                brush = Brush.radialGradient(
                                    colors = listOf(Color(0xFF2563EB).copy(alpha = 0.1f), Color.Transparent)
                                ),
                                shape = CircleShape
                            )
                            .border(2.dp, Color(0xFF2563EB).copy(alpha = 0.2f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Box(
                            modifier = Modifier
                                .size(50.dp)
                                .background(Color(0xFF2563EB), CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Check,
                                contentDescription = null,
                                tint = Color.White,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
