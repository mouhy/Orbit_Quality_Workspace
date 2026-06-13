package com.orbit.mobile.feature.tasks

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.AnalyzeTaskRequest
import com.orbit.mobile.data.dto.QualityAnalysisDto
import com.orbit.mobile.data.dto.standardText
import com.orbit.mobile.domain.repository.TasksRepository
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.TinyPill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// Sample task
data class SampleTask(
    val id: String,
    val name: String,
    val category: String,
    val assignees: List<String>,
    val status: String,
    val priority: String,
    val dueDate: String,
    val overdue: Boolean = false,
    val projectId: String? = null
)

// Web samples
private fun sampleTasks(): List<SampleTask> = listOf(
    SampleTask("1", "Design System Audit & Update", "Finance • #TSK-10-24", listOf("AB", "CD"), "Not Completed Yet", "HIGH", "Oct 24, 2023", projectId = "public-group"),
    SampleTask("2", "API Integration for Payment Gateway", "Marketing • #TSK-10-39", listOf("EF"), "Not Completed Yet", "MED", "Oct 28, 2023", projectId = "all-sub-admin"),
    SampleTask("3", "Drafting Q4 Budget Proposal", "Finance • #TSK-11-01", listOf("GH"), "Completed", "LOW", "Oct 15, 2023", projectId = "public-group"),
    SampleTask("4", "Security Vulnerability Patch v2.3", "Security • #TSK-10-25", emptyList(), "Not Completed Yet", "HIGH", "Overdue", overdue = true, projectId = "public-group"),
    SampleTask("5", "Marketing Copy for Landing Page", "Content • #TSK-SD-231", listOf("IJ", "KL"), "Not Completed Yet", "MED", "Nov 02, 2023", projectId = "public-group")
)

// Tasks state
data class TasksState(
    val tasks: List<SampleTask> = sampleTasks(),
    val analysisTask: SampleTask? = null,
    val analysisLoading: Boolean = false,
    val analysisResult: QualityAnalysisDto? = null,
    val analysisError: UiText? = null
)

// Tasks VM
@HiltViewModel
class TasksViewModel @Inject constructor(
    private val repo: TasksRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TasksState())
    val state: StateFlow<TasksState> = _state

    // Toggle done
    fun toggle(id: String) {
        _state.update { s ->
            s.copy(tasks = s.tasks.map {
                if (it.id == id) {
                    it.copy(
                        status = if (it.status == "Completed") "Not Completed Yet" else "Completed"
                    )
                } else it
            })
        }
    }

    // AI analyze
    fun analyze(task: SampleTask) {
        _state.update {
            it.copy(
                analysisTask = task,
                analysisLoading = true,
                analysisResult = null,
                analysisError = null
            )
        }
        viewModelScope.launch {
            repo.analyze(
                task.id,
                AnalyzeTaskRequest(
                    taskTitle = task.name,
                    taskDescription =
                        "${task.category} | Priority ${task.priority} | Status ${task.status}",
                    taskId = task.id,
                    projectId = task.projectId
                )
            )
                .onSuccess { result ->
                    _state.update { it.copy(analysisLoading = false, analysisResult = result) }
                }
                .onFailure { e ->
                    _state.update { it.copy(analysisLoading = false, analysisError = e.toUiText()) }
                }
        }
    }

    fun closeAnalysis() {
        _state.update {
            it.copy(
                analysisTask = null,
                analysisLoading = false,
                analysisResult = null,
                analysisError = null
            )
        }
    }
}

// Status tone
private fun taskStatusColor(status: String): Color = when (status) {
    "Done", "Completed" -> OrbitSuccess
    "Review" -> OrbitPurple
    else -> OrbitPrimary
}

private fun taskPriorityColor(priority: String): Color = when (priority) {
    "HIGH" -> OrbitDanger
    "MED" -> OrbitWarning
    else -> Color(0xFF94A3B8)
}

// Tasks page
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TasksScreen(
    viewModel: TasksViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Completion stats
    val completed = 24
    val inProgress = 9
    val remaining = 4
    val percentage = 65

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Completion card
        DashCard {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.tk_completion_title),
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Text(
                        text = stringResource(R.string.tk_completion_sub),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                Text(
                    text = "$percentage%",
                    style = MaterialTheme.typography.headlineMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = OrbitPrimary
                )
            }
            Spacer(Modifier.height(10.dp))
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(8.dp)
                    .background(colors.border, RoundedCornerShape(8.dp))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(percentage / 100f)
                        .height(8.dp)
                        .background(OrbitPrimary, RoundedCornerShape(8.dp))
                )
            }
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                StatDot(
                    stringResource(R.string.tk_stat_completed, completed),
                    OrbitPrimary
                )
                StatDot(
                    stringResource(R.string.tk_stat_in_progress, inProgress),
                    OrbitWarning
                )
                StatDot(
                    stringResource(R.string.tk_stat_remaining, remaining),
                    colors.textMuted
                )
            }
        }

        // Tasks list
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.nav_tasks),
                subtitle = stringResource(R.string.tk_showing, state.tasks.size)
            )
            state.tasks.forEach { task ->
                val done = task.status == "Completed"
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Checkbox(
                        checked = done,
                        onCheckedChange = { viewModel.toggle(task.id) },
                        colors = CheckboxDefaults.colors(checkedColor = OrbitSuccess)
                    )
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = task.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = task.category,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TinyPill(
                                if (done) stringResource(R.string.tk_status_completed)
                                else stringResource(R.string.tk_status_not_completed),
                                taskStatusColor(task.status)
                            )
                            TinyPill(task.priority, taskPriorityColor(task.priority))
                            Text(
                                text = task.dueDate,
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = if (task.overdue) FontWeight.Bold else FontWeight.Normal,
                                color = if (task.overdue) OrbitDanger else colors.textSecondary
                            )
                        }
                        Spacer(Modifier.height(6.dp))
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            if (task.assignees.isEmpty()) {
                                Text(
                                    text = stringResource(R.string.tk_unassigned),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            } else {
                                Row {
                                    task.assignees.forEach { initials ->
                                        Box(modifier = Modifier.padding(end = 2.dp)) {
                                            HashAvatar(name = initials, size = 22.dp)
                                        }
                                    }
                                }
                            }
                            Spacer(Modifier.weight(1f))
                            Surface(
                                onClick = { viewModel.analyze(task) },
                                shape = RoundedCornerShape(8.dp),
                                color = OrbitPrimary.copy(alpha = 0.08f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    OrbitPrimary.copy(alpha = 0.3f)
                                )
                            ) {
                                Text(
                                    text = stringResource(R.string.tk_analyze_ai),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OrbitPrimary,
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 4.dp
                                    )
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    // Analysis sheet
    if (state.analysisTask != null) {
        ModalBottomSheet(
            onDismissRequest = viewModel::closeAnalysis,
            containerColor = colors.popupBg,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.tk_analysis_title),
                    style = MaterialTheme.typography.headlineSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = state.analysisTask?.name ?: "",
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(16.dp))

                when {
                    state.analysisLoading -> Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 24.dp),
                        horizontalArrangement = Arrangement.Center,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(18.dp),
                            color = OrbitPrimary,
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = stringResource(R.string.tk_analyzing),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                    state.analysisError != null -> Text(
                        text = state.analysisError!!.asString(),
                        style = MaterialTheme.typography.bodySmall,
                        color = OrbitDanger
                    )
                    state.analysisResult != null -> {
                        val result = state.analysisResult!!
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            ProgressRing(
                                pct = result.complianceScore.roundToInt(),
                                color = if (result.complianceScore >= 70) OrbitSuccess
                                else OrbitWarning,
                                size = 72.dp,
                                thick = 6.dp
                            )
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = stringResource(R.string.tk_compliance_score),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                                result.verdict?.let {
                                    TinyPill(
                                        it.uppercase(),
                                        if (result.complianceScore >= 70) OrbitSuccess
                                        else OrbitDanger
                                    )
                                }
                            }
                        }
                        Spacer(Modifier.height(14.dp))
                        if (result.passedStandards.isNotEmpty()) {
                            AnalysisSection(
                                title = stringResource(R.string.tk_passed),
                                items = result.passedStandards.map { standardText(it) },
                                tone = OrbitSuccess
                            )
                        }
                        if (result.failedStandards.isNotEmpty()) {
                            AnalysisSection(
                                title = stringResource(R.string.tk_failed),
                                items = result.failedStandards.map { standardText(it) },
                                tone = OrbitDanger
                            )
                        }
                        if (result.suggestions.isNotEmpty()) {
                            AnalysisSection(
                                title = stringResource(R.string.tk_suggestions),
                                items = result.suggestions,
                                tone = OrbitWarning
                            )
                        }
                    }
                }
            }
        }
    }
}

// Stat dot
@Composable
private fun StatDot(text: String, tone: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(10.dp)
                .background(tone, CircleShape)
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Medium,
            color = OrbitTheme.colors.textSecondary
        )
    }
}

// Result block
@Composable
fun AnalysisSection(title: String, items: List<String>, tone: Color) {
    val colors = OrbitTheme.colors
    Column(modifier = Modifier.padding(bottom = 12.dp)) {
        Text(
            text = title.uppercase(),
            style = OrbitTextStyles.techLabel,
            color = tone
        )
        Spacer(Modifier.height(6.dp))
        items.forEach { item ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .padding(top = 6.dp)
                        .size(5.dp)
                        .background(tone, CircleShape)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    text = item,
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
    }
}
