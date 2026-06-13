package com.orbit.mobile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.AreaLineChart
import com.orbit.mobile.core.ui.charts.BarEntry
import com.orbit.mobile.core.ui.charts.GaugeArc
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.charts.VerticalBarChart
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.feature.shell.InnerRoutes
import kotlin.math.max
import kotlin.math.min
import kotlin.math.roundToInt

// Staff header
private val StaffGradient = Brush.linearGradient(
    listOf(Color(0xFF059669), Color(0xFF0D9488), Color(0xFF0284C7))
)

// Staff dashboard
@Composable
fun StaffDashboardContent(
    onNavigate: (String) -> Unit,
    viewModel: StaffDashboardViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val analytics = state.analytics

    val sb = analytics?.statusBreakdown
    val totalTasks = analytics?.totalTasks ?: 0
    val doneTasks = sb?.done ?: 0
    val onTimeRate = analytics?.onTimeRate ?: 0.0
    val compRate = if (totalTasks > 0) (doneTasks * 100.0 / totalTasks).roundToInt() else 0
    val prodScore = (compRate * 0.6 + onTimeRate * 0.4).roundToInt()

    val taskList = analytics?.activeTasksList ?: emptyList()
    val overdueCount = taskList.count {
        val d = daysUntil(it.deadline)
        d != null && d < 0
    }
    val todayCount = taskList.count { daysUntil(it.deadline) == 0 }
    val deadlines = taskList.filter { it.deadline != null }
        .sortedBy { it.deadline }
        .take(10)
    val myProjects = state.myProjects
    val avgProg = if (myProjects.isEmpty()) 0
    else (myProjects.sumOf { it.progress }.toDouble() / myProjects.size).roundToInt()
    val weekSpark = listOf(
        max(0, compRate - 16), max(0, compRate - 11), max(0, compRate - 7),
        max(0, compRate - 4), max(0, compRate - 1), compRate, min(100, compRate + 2)
    )

    // Return refresh
    OnReturnRefresh { viewModel.fetchAll() }

    // Pull refresh
    OrbitPullRefresh(
        refreshing = state.loading,
        onRefresh = { viewModel.fetchAll() }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Banners
        state.error?.let { ErrorBanner(it.asString()) { viewModel.fetchAll() } }
        if (state.seeding) SeedingBanner(stringResource(R.string.dash_loading_demo))

        // Welcome header
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(StaffGradient, RoundedCornerShape(20.dp))
                .padding(20.dp)
        ) {
            Row(verticalAlignment = Alignment.Top) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = todayLabel().uppercase(),
                        style = OrbitTextStyles.techLabel,
                        color = Color.White.copy(alpha = 0.55f)
                    )
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.dash_welcome_back, viewModel.userName),
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderPill(
                    stringResource(R.string.dash_active_tasks_pill, analytics?.activeTasks ?: 0),
                    OrbitSuccess
                )
                if (overdueCount > 0) {
                    HeaderPill(
                        stringResource(R.string.dash_overdue_pill, overdueCount),
                        Color(0xFFFCA5A5)
                    )
                }
                HeaderPill(
                    stringResource(R.string.dash_projects_pill, myProjects.size),
                    Color.White.copy(alpha = 0.75f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Surface(
                onClick = { onNavigate(InnerRoutes.TASKFLOW_PUBLIC) },
                shape = RoundedCornerShape(10.dp),
                color = Color.White.copy(alpha = 0.12f),
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    Color.White.copy(alpha = 0.25f)
                )
            ) {
                Text(
                    text = stringResource(R.string.dash_public_channel),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                )
            }
        }

        // KPI grid
        val kpis = listOf(
            Triple(stringResource(R.string.dash_kpi_my_tasks), totalTasks.toString(), DashIndigo),
            Triple(stringResource(R.string.dash_kpi_overdue), overdueCount.toString(), OrbitDanger),
            Triple(stringResource(R.string.dash_kpi_done), doneTasks.toString(), OrbitSuccess),
            Triple(stringResource(R.string.dash_kpi_projects), myProjects.size.toString(), OrbitPurple),
            Triple(stringResource(R.string.dash_kpi_due_today), todayCount.toString(), OrbitWarning),
            Triple(
                stringResource(R.string.dash_kpi_productivity),
                "$prodScore%",
                if (prodScore >= 70) OrbitSuccess else OrbitWarning
            )
        )
        kpis.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (label, value, color) ->
                    KpiCard(
                        label = label,
                        value = value,
                        color = color,
                        spark = weekSpark,
                        loading = state.loading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // My tasks
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_my_tasks),
                subtitle = stringResource(
                    R.string.dash_active_done,
                    analytics?.activeTasks ?: 0,
                    doneTasks
                )
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                TinyPill("TODO ${sb?.todo ?: 0}", dashStatusColor("TODO"))
                TinyPill("IN PROGRESS ${sb?.inProgress ?: 0}", dashStatusColor("IN_PROGRESS"))
                TinyPill("REVIEW ${sb?.review ?: 0}", dashStatusColor("REVIEW"))
            }
            Spacer(Modifier.height(10.dp))
            when {
                state.loading -> LoadingHint()
                taskList.isEmpty() -> EmptyHint(stringResource(R.string.dash_no_active_tasks))
                else -> taskList.take(14).forEach { task ->
                    val days = daysUntil(task.deadline)
                    val pc = dashPriorityColor(task.priority)
                    val sc = dashStatusColor(task.status)
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onNavigate("staff-task?taskId=${task.id}") }
                            .padding(vertical = 7.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(4.dp, 36.dp)
                                .background(pc, RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = task.projectName ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                            Spacer(Modifier.height(3.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                                TinyPill(task.status.replace("_", " "), sc)
                                TinyPill(task.priority.uppercase(), pc)
                            }
                        }
                        if (days != null) {
                            Text(
                                text = deadlineShortLabel(days),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = when {
                                    days < 0 -> OrbitDanger
                                    days == 0 -> OrbitWarning
                                    else -> colors.textMuted
                                }
                            )
                        }
                    }
                }
            }
        }

        // Deadlines
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_upcoming_deadlines),
                subtitle = stringResource(R.string.dash_sorted_due)
            )
            if (!state.loading && deadlines.isEmpty()) {
                EmptyHint(stringResource(R.string.dash_no_deadlines))
            } else {
                deadlines.forEach { task ->
                    val days = daysUntil(task.deadline) ?: 0
                    val tone = when {
                        days < 0 -> OrbitDanger
                        days <= 3 -> OrbitWarning
                        else -> OrbitSuccess
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(tone.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                .border(1.dp, tone.copy(alpha = 0.13f), RoundedCornerShape(8.dp)),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = if (days <= 0) "!" else days.toString(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = tone
                            )
                        }
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = task.title,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = task.projectName ?: "—",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = deadlineShortLabel(days),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = tone
                            )
                            Text(
                                text = task.priority.uppercase(),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = dashPriorityColor(task.priority)
                            )
                        }
                    }
                }
            }
        }

        // Trend chart
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_productivity_trend),
                subtitle = stringResource(R.string.dash_7w_completion),
                action = {
                    TinyPill(
                        if (compRate >= 70) stringResource(R.string.dash_on_track)
                        else stringResource(R.string.dash_building),
                        if (compRate >= 70) OrbitSuccess else OrbitWarning
                    )
                }
            )
            if (!state.loading) {
                AreaLineChart(
                    data = weekSpark,
                    labels = listOf("6w", "5w", "4w", "3w", "2w", "1w", "•"),
                    color = OrbitSuccess,
                    height = 90.dp
                )
            }
        }

        // Workload bars
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_workload),
                subtitle = stringResource(R.string.dash_by_status)
            )
            if (!state.loading) {
                VerticalBarChart(
                    bars = listOf(
                        BarEntry("TODO", sb?.todo ?: 0, dashStatusColor("TODO")),
                        BarEntry("PROG", sb?.inProgress ?: 0, dashStatusColor("IN_PROGRESS")),
                        BarEntry("REV", sb?.review ?: 0, dashStatusColor("REVIEW")),
                        BarEntry("DONE", doneTasks, dashStatusColor("DONE"))
                    ),
                    height = 110.dp
                )
            }
        }

        // Score gauge
        DashCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.dash_score),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                if (!state.loading) {
                    GaugeArc(score = prodScore, label = "", size = 110.dp)
                    analytics?.burnoutRisk?.let { risk ->
                        Text(
                            text = stringResource(R.string.dash_risk, risk),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = when (risk) {
                                "High" -> OrbitDanger
                                "Medium" -> OrbitWarning
                                else -> OrbitSuccess
                            }
                        )
                    }
                }
            }
        }

        // My projects
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_my_projects),
                action = { ViewAllLink { onNavigate(InnerRoutes.MY_PROJECTS) } }
            )
            when {
                state.loading -> LoadingHint()
                myProjects.isEmpty() -> EmptyHint(stringResource(R.string.dash_no_projects_assigned))
                else -> myProjects.take(6).chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { p ->
                            val tone = if (p.progress >= 70) OrbitSuccess
                            else if (p.status.uppercase().contains("HOLD")) OrbitDanger
                            else OrbitWarning
                            Surface(
                                onClick = {
                                    onNavigate("${InnerRoutes.WORKSPACE}?projectId=${p.projectId}")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surface2,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    tone.copy(alpha = 0.13f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(12.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    ProgressRing(
                                        pct = p.progress,
                                        color = tone,
                                        size = 36.dp,
                                        thick = 3.dp
                                    )
                                    Spacer(Modifier.width(8.dp))
                                    Column(modifier = Modifier.weight(1f)) {
                                        Text(
                                            text = p.title,
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = colors.textPrimary,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                        Text(
                                            text = p.status,
                                            style = MaterialTheme.typography.labelSmall,
                                            fontWeight = FontWeight.Bold,
                                            color = tone
                                        )
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Quick actions
        DashCard {
            Text(
                text = stringResource(R.string.dash_quick_actions).uppercase(),
                style = OrbitTextStyles.techLabel,
                color = colors.textMuted
            )
            Spacer(Modifier.height(10.dp))
            val actions = listOf(
                Triple(stringResource(R.string.nav_my_projects), DashIndigo, InnerRoutes.MY_PROJECTS),
                Triple(stringResource(R.string.nav_public_channel), OrbitPurple, InnerRoutes.TASKFLOW_PUBLIC),
                Triple(stringResource(R.string.nav_calendar), OrbitSuccess, InnerRoutes.CALENDAR),
                Triple(stringResource(R.string.nav_settings), OrbitWarning, InnerRoutes.SETTINGS)
            )
            actions.forEach { (label, tone, route) ->
                Surface(
                    onClick = { onNavigate(route) },
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 6.dp),
                    shape = RoundedCornerShape(9.dp),
                    color = tone.copy(alpha = 0.06f),
                    border = androidx.compose.foundation.BorderStroke(1.dp, tone.copy(alpha = 0.13f))
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = tone,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 9.dp)
                    )
                }
            }
        }

        // My stats
        DashCard {
            Text(
                text = stringResource(R.string.dash_my_stats).uppercase(),
                style = OrbitTextStyles.techLabel,
                color = colors.textMuted
            )
            Spacer(Modifier.height(8.dp))
            val stats = listOf(
                Triple(
                    stringResource(R.string.dash_completion),
                    "$compRate%",
                    if (compRate >= 70) OrbitSuccess else OrbitWarning
                ),
                Triple(stringResource(R.string.dash_on_time), "${onTimeRate.roundToInt()}%", DashIndigo),
                Triple(stringResource(R.string.dash_avg_progress), "$avgProg%", OrbitPurple)
            )
            stats.forEach { (label, value, tone) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (state.loading) "—" else value,
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.ExtraBold,
                        color = tone
                    )
                }
            }
        }
    }
    }
}

// Header pill
@Composable
private fun HeaderPill(text: String, tone: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = tone,
        modifier = Modifier
            .background(tone.copy(alpha = 0.13f), RoundedCornerShape(20.dp))
            .border(1.dp, tone.copy(alpha = 0.25f), RoundedCornerShape(20.dp))
            .padding(horizontal = 10.dp, vertical = 4.dp)
    )
}

// Loading hint
@Composable
fun LoadingHint() {
    val colors = OrbitTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = stringResource(R.string.state_loading),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )
    }
}

// Empty hint
@Composable
fun EmptyHint(text: String) {
    val colors = OrbitTheme.colors
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 20.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )
    }
}
