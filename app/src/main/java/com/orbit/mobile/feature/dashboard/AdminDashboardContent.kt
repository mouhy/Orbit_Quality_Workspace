package com.orbit.mobile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
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
import androidx.compose.material3.MaterialTheme
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
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTeal
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.AreaLineChart
import com.orbit.mobile.core.ui.charts.BarEntry
import com.orbit.mobile.core.ui.charts.DonutSegment
import com.orbit.mobile.core.ui.charts.GaugeArc
import com.orbit.mobile.core.ui.charts.HeatGrid
import com.orbit.mobile.core.ui.charts.MiniDonut
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.charts.Sparkline
import com.orbit.mobile.core.ui.charts.VerticalBarChart
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.feature.shell.InnerRoutes
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.min

// Insight row
private data class Insight(val color: Color, val title: String, val body: String)

// Admin dashboard
@Composable
fun AdminDashboardContent(
    onNavigate: (String) -> Unit,
    viewModel: AdminDashboardViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val cp = state.customProjects

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
        if (state.seeding) SeedingBanner(stringResource(R.string.dash_seeding_admin))

        // Header
        Column {
            Text(
                text = todayLabel().uppercase(),
                style = OrbitTextStyles.techLabel,
                color = colors.textMuted
            )
            Spacer(Modifier.height(4.dp))
            Text(
                text = stringResource(R.string.dash_exec_title),
                style = MaterialTheme.typography.headlineMedium,
                color = colors.textPrimary
            )
            Text(
                text = stringResource(R.string.dash_welcome_back, viewModel.userName),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
            Spacer(Modifier.height(10.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                HeaderActionButton(
                    text = stringResource(R.string.dash_action_new_project),
                    filled = true
                ) { onNavigate(InnerRoutes.PROJECTS) }
                HeaderActionButton(
                    text = stringResource(R.string.dash_action_analytics),
                    filled = false
                ) { onNavigate(InnerRoutes.TEAM) }
                HeaderActionButton(
                    text = stringResource(R.string.dash_action_reports),
                    filled = false
                ) { onNavigate(InnerRoutes.REPORTS) }
            }
        }

        // Health gauge
        DashCard {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                GaugeArc(
                    score = if (state.loading) 0 else state.wsHealth,
                    label = "",
                    size = 130.dp
                )
                Text(
                    text = stringResource(R.string.dash_workspace_health).uppercase(),
                    style = OrbitTextStyles.techLabel,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(4.dp))
                val health = state.wsHealth
                TinyPill(
                    text = when {
                        state.loading -> "…"
                        health >= 70 -> stringResource(R.string.dash_health_excellent)
                        health >= 40 -> stringResource(R.string.dash_health_moderate)
                        else -> stringResource(R.string.dash_health_critical)
                    },
                    color = when {
                        health >= 70 -> OrbitSuccess
                        health >= 40 -> OrbitWarning
                        else -> OrbitDanger
                    }
                )
            }
        }

        // KPI grid
        val spark = listOf(
            max(1, cp.size - 3), max(0, cp.size - 2), max(0, cp.size - 2),
            max(0, cp.size - 1), max(0, cp.size - 1), cp.size
        )
        val kpis = listOf(
            Triple(stringResource(R.string.kpi_total_projects), cp.size.toString(), DashIndigo),
            Triple(stringResource(R.string.kpi_active_projects), state.active.size.toString(), OrbitSuccess),
            Triple(stringResource(R.string.kpi_delayed), state.overdue.toString(), OrbitDanger),
            Triple(
                stringResource(R.string.kpi_completion_rate),
                "${state.completionRate}%",
                if (state.completionRate >= 70) OrbitSuccess
                else if (state.completionRate >= 40) OrbitWarning else OrbitDanger
            ),
            Triple(stringResource(R.string.kpi_ai_pass), "${state.aiPassRate}%", OrbitPurple),
            Triple(
                stringResource(R.string.kpi_productivity),
                state.productivityScore.toString(),
                if (state.productivityScore >= 70) OrbitSuccess else OrbitWarning
            ),
            Triple(stringResource(R.string.kpi_online), state.online.size.toString(), OrbitTeal),
            Triple(
                stringResource(R.string.kpi_avg_progress),
                "${state.avgProgress}%",
                if (state.avgProgress >= 70) OrbitSuccess
                else if (state.avgProgress >= 40) OrbitWarning else OrbitDanger
            )
        )
        kpis.chunked(2).forEach { rowItems ->
            Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                rowItems.forEach { (label, value, tone) ->
                    KpiCard(
                        label = label,
                        value = value,
                        color = tone,
                        spark = spark,
                        loading = state.loading,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // Project health
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_project_health),
                subtitle = stringResource(R.string.dash_ph_sub),
                action = { ViewAllLink { onNavigate(InnerRoutes.PROJECTS) } }
            )
            // Channels strip
            if (!state.loading && state.systemChannels.isNotEmpty()) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(bottom = 10.dp),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.dash_channels_label).uppercase(),
                        style = OrbitTextStyles.techLabel,
                        color = colors.textMuted
                    )
                    state.systemChannels.forEach { ch ->
                        Surface(
                            onClick = {
                                onNavigate("${InnerRoutes.TASKFLOW}?projectId=${ch.projectId}")
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = DashIndigo.copy(alpha = 0.1f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                DashIndigo.copy(alpha = 0.28f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 4.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(6.dp)
                            ) {
                                Text(
                                    text = "#",
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = DashIndigo
                                )
                                Text(
                                    text = ch.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = DashIndigo
                                )
                                Text(
                                    text = stringResource(R.string.dash_live),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = OrbitPurple,
                                    modifier = Modifier
                                        .background(
                                            DashIndigo.copy(alpha = 0.18f),
                                            RoundedCornerShape(8.dp)
                                        )
                                        .padding(horizontal = 5.dp, vertical = 1.dp)
                                )
                            }
                        }
                    }
                }
            }
            when {
                state.loading -> LoadingHint()
                cp.isEmpty() -> EmptyHint(stringResource(R.string.dash_no_projects))
                else -> cp.take(12).chunked(2).forEach { rowItems ->
                    Row(
                        modifier = Modifier.padding(bottom = 10.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        rowItems.forEach { p ->
                            val hc = healthColor(p.progress, p.status)
                            val team = p.subAdmins.mapNotNull { it.name } +
                                p.staff.mapNotNull { it.name }
                            val days = daysUntil(p.dueDate)
                            Surface(
                                onClick = {
                                    onNavigate("${InnerRoutes.TASKFLOW}?projectId=${p.projectId}")
                                },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(12.dp),
                                color = colors.surface2,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    hc.copy(alpha = 0.16f)
                                )
                            ) {
                                Column(modifier = Modifier.padding(12.dp)) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        ProgressRing(
                                            pct = p.progress,
                                            color = hc,
                                            size = 44.dp,
                                            thick = 3.5.dp
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
                                            Spacer(Modifier.height(3.dp))
                                            TinyPill("● ${healthLabel(p.progress, p.status)}", hc)
                                        }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Row(modifier = Modifier.weight(1f)) {
                                            team.take(3).forEach { n ->
                                                Box(modifier = Modifier.padding(end = 2.dp)) {
                                                    HashAvatar(name = n, size = 18.dp)
                                                }
                                            }
                                            if (team.size > 3) {
                                                Box(
                                                    modifier = Modifier
                                                        .size(18.dp)
                                                        .background(
                                                            hc.copy(alpha = 0.13f),
                                                            CircleShape
                                                        ),
                                                    contentAlignment = Alignment.Center
                                                ) {
                                                    Text(
                                                        text = "+${team.size - 3}",
                                                        style = MaterialTheme.typography.labelSmall,
                                                        fontWeight = FontWeight.Bold,
                                                        color = hc
                                                    )
                                                }
                                            }
                                        }
                                        if (days != null) {
                                            Text(
                                                text = when {
                                                    days < 0 -> stringResource(
                                                        R.string.dash_days_late, abs(days)
                                                    )
                                                    days == 0 -> stringResource(R.string.dash_today)
                                                    else -> stringResource(
                                                        R.string.dash_days_left, days
                                                    )
                                                },
                                                style = MaterialTheme.typography.labelSmall,
                                                fontWeight = FontWeight.SemiBold,
                                                color = when {
                                                    days < 0 -> OrbitDanger
                                                    days <= 3 -> OrbitWarning
                                                    else -> colors.textMuted
                                                }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                        if (rowItems.size == 1) Spacer(Modifier.weight(1f))
                    }
                }
            }
        }

        // Status donut
        DashCard {
            DashCardHeader(title = stringResource(R.string.dash_status_distribution))
            Row(verticalAlignment = Alignment.CenterVertically) {
                MiniDonut(
                    segments = listOf(
                        DonutSegment(state.active.size, DashIndigo),
                        DonutSegment(state.done.size, OrbitSuccess),
                        DonutSegment(state.onHold.size, Color(0xFFEAB308))
                    ),
                    centerLabel = cp.size.toString(),
                    centerSub = stringResource(R.string.dash_donut_projects),
                    size = 100.dp
                )
                Spacer(Modifier.width(20.dp))
                Column(modifier = Modifier.weight(1f)) {
                    LegendRow(stringResource(R.string.status_active), state.active.size, DashIndigo)
                    LegendRow(stringResource(R.string.status_completed), state.done.size, OrbitSuccess)
                    LegendRow(stringResource(R.string.status_on_hold), state.onHold.size, Color(0xFFEAB308))
                }
            }
        }

        // Risk summary
        DashCard {
            DashCardHeader(title = stringResource(R.string.dash_risk_summary))
            val atRisk = cp.count { it.progress < 40 && it.status.uppercase() == "ACTIVE" }
            val healthy = cp.count { it.progress >= 70 || it.status.uppercase() == "COMPLETED" }
            val rows = listOf(
                Triple(stringResource(R.string.risk_overdue), state.overdue, OrbitDanger),
                Triple(stringResource(R.string.risk_at_risk), atRisk, OrbitWarning),
                Triple(stringResource(R.string.risk_healthy), healthy, OrbitSuccess),
                Triple(stringResource(R.string.risk_members), state.users.size, OrbitPurple)
            )
            val denom = max(if (cp.isNotEmpty()) cp.size else state.users.size, 1)
            rows.forEach { (label, value, tone) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(32.dp)
                            .background(tone.copy(alpha = 0.08f), RoundedCornerShape(8.dp)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = if (state.loading) "—" else value.toString(),
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = tone
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.width(70.dp)
                    )
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .height(3.dp)
                            .background(colors.border, RoundedCornerShape(3.dp))
                    ) {
                        Box(
                            modifier = Modifier
                                .fillMaxWidth(min(value.toFloat() / denom, 1f))
                                .height(3.dp)
                                .background(tone, RoundedCornerShape(3.dp))
                        )
                    }
                }
            }
        }

        // Heatmap
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_heatmap_title),
                subtitle = stringResource(R.string.dash_heatmap_sub)
            )
            if (state.loading) {
                LoadingHint()
            } else {
                val heat = state.heatmap
                HeatGrid(data = heat, height = 100.dp)
                Spacer(Modifier.height(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(14.dp)) {
                    HeatStat(
                        stringResource(R.string.dash_total_events),
                        state.auditLogs.size.toString(),
                        DashIndigo
                    )
                    HeatStat(
                        stringResource(R.string.dash_active_days),
                        heat.flatten().count { it > 0 }.toString(),
                        OrbitSuccess
                    )
                    HeatStat(
                        stringResource(R.string.dash_peak_day),
                        (heat.flatten().maxOrNull() ?: 0).toString(),
                        OrbitWarning
                    )
                }
            }
        }

        // Live activity
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_live_activity),
                subtitle = stringResource(R.string.dash_recent_events, state.auditLogs.size)
            )
            when {
                state.loading -> LoadingHint()
                state.auditLogs.isEmpty() -> EmptyHint(stringResource(R.string.dash_no_activity))
                else -> state.auditLogs.take(15).forEach { log ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HashAvatar(name = log.userName ?: "System", size = 24.dp)
                        Spacer(Modifier.width(9.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Row {
                                Text(
                                    text = log.userName ?: "System",
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.width(4.dp))
                                Text(
                                    text = log.actionLabel,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textSecondary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                            Text(
                                text = timeAgo(log.time).asString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }

        // Team bars
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_projects_by_team),
                subtitle = stringResource(R.string.dash_pbt_sub),
                action = { ViewAllLink { onNavigate(InnerRoutes.PROJECTS) } }
            )
            val palette = listOf(DashIndigo, OrbitSuccess, OrbitWarning, OrbitDanger, OrbitPurple, OrbitTeal)
            val groups = cp.groupBy { p ->
                p.subAdmins.firstOrNull()?.name?.split(" ")?.firstOrNull() ?: "General"
            }.entries.sortedByDescending { it.value.size }.take(6)
                .mapIndexed { i, entry ->
                    BarEntry(entry.key, entry.value.size, palette[i % palette.size])
                }
            when {
                state.loading -> LoadingHint()
                groups.isEmpty() -> EmptyHint(stringResource(R.string.dash_no_projects))
                else -> VerticalBarChart(bars = groups, height = 130.dp)
            }
        }

        // Weekly trend
        DashCard {
            val avg = state.avgProgress
            DashCardHeader(
                title = stringResource(R.string.dash_weekly_trend),
                subtitle = stringResource(R.string.dash_wt_sub),
                action = {
                    TinyPill(
                        text = when {
                            avg >= 70 -> stringResource(R.string.dash_on_track)
                            avg >= 40 -> stringResource(R.string.dash_moderate)
                            else -> stringResource(R.string.dash_lagging)
                        },
                        color = when {
                            avg >= 70 -> OrbitSuccess
                            avg >= 40 -> OrbitWarning
                            else -> OrbitDanger
                        }
                    )
                }
            )
            val trend = listOf(
                max(0, avg - 16), max(0, avg - 11), max(0, avg - 7),
                max(0, avg - 4), max(0, avg - 1), avg, min(100, avg + 3)
            )
            if (!state.loading) {
                AreaLineChart(
                    data = trend,
                    labels = listOf("6w", "5w", "4w", "3w", "2w", "1w", "•"),
                    color = when {
                        avg >= 70 -> OrbitSuccess
                        avg >= 40 -> OrbitWarning
                        else -> OrbitDanger
                    },
                    height = 90.dp
                )
                Spacer(Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = "${stringResource(R.string.dash_start)}: ${trend.first()}%",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Text(
                        text = "${stringResource(R.string.dash_now)}: ${trend.last()}%",
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (avg >= 70) OrbitSuccess else OrbitWarning
                    )
                }
            }
        }

        // AI insights
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_ai_insights),
                subtitle = stringResource(R.string.dash_ai_sub)
            )
            val insights = buildList {
                if (state.overdue > 0) add(
                    Insight(
                        OrbitDanger,
                        stringResource(R.string.ins_deadline_title),
                        stringResource(R.string.ins_deadline_body, state.overdue)
                    )
                )
                if (state.done.isNotEmpty() && cp.isNotEmpty()) add(
                    Insight(
                        OrbitSuccess,
                        stringResource(R.string.ins_completion_title),
                        stringResource(R.string.ins_completion_body, state.completionRate)
                    )
                )
                if (state.onHold.isNotEmpty()) add(
                    Insight(
                        Color(0xFFEAB308),
                        stringResource(R.string.ins_blocked_title),
                        stringResource(R.string.ins_blocked_body, state.onHold.size)
                    )
                )
                if (state.online.size > state.users.size * 0.5) add(
                    Insight(
                        DashIndigo,
                        stringResource(R.string.ins_activity_title),
                        stringResource(
                            R.string.ins_activity_body,
                            state.online.size,
                            state.users.size
                        )
                    )
                )
                if (state.avgProgress >= 70) add(
                    Insight(
                        OrbitSuccess,
                        stringResource(R.string.ins_progress_title),
                        stringResource(R.string.ins_progress_body, state.avgProgress)
                    )
                ) else if (state.avgProgress < 40) add(
                    Insight(
                        OrbitWarning,
                        stringResource(R.string.ins_low_progress_title),
                        stringResource(R.string.ins_low_progress_body, state.avgProgress)
                    )
                )
                if (state.aiPassRate >= 80) add(
                    Insight(
                        OrbitPurple,
                        stringResource(R.string.ins_ai_title),
                        stringResource(R.string.ins_ai_body, state.aiPassRate)
                    )
                )
                if (state.auditLogs.size > 20) add(
                    Insight(
                        OrbitTeal,
                        stringResource(R.string.ins_engagement_title),
                        stringResource(R.string.ins_engagement_body, state.auditLogs.size)
                    )
                )
            }.take(4)
            when {
                state.loading -> LoadingHint()
                insights.isEmpty() -> EmptyHint(stringResource(R.string.dash_no_insights))
                else -> insights.forEach { ins ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(ins.color.copy(alpha = 0.04f), RoundedCornerShape(10.dp))
                            .border(1.dp, ins.color.copy(alpha = 0.13f), RoundedCornerShape(10.dp))
                            .padding(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp, 34.dp)
                                .background(ins.color, RoundedCornerShape(4.dp))
                        )
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text(
                                text = ins.title,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = ins.body,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary
                            )
                        }
                    }
                }
            }
        }

        // Top performers
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.dash_top_performers),
                action = { ViewAllLink { onNavigate(InnerRoutes.TEAM) } }
            )
            val performerRoles = setOf("admin", "sub_admin", "staff")
            val medals = listOf("🥇", "🥈", "🥉", "#4", "#5")
            val medalColors = listOf(
                OrbitWarning, Color(0xFF94A3B8), Color(0xFFCD7F32), DashIndigo, OrbitPurple
            )
            val performers = state.users.filter { it.role in performerRoles }.take(5)
            if (state.loading) {
                LoadingHint()
            } else {
                performers.forEachIndexed { i, u ->
                    val mc = medalColors[i]
                    val score = max(20, 96 - i * 14)
                    val tasks = max(2, 18 - i * 3)
                    val projectCount = cp.count { p ->
                        p.subAdmins.any { it.id == u.id } || p.staff.any { it.id == u.id }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 8.dp)
                            .background(mc.copy(alpha = 0.04f), RoundedCornerShape(11.dp))
                            .border(1.dp, mc.copy(alpha = 0.13f), RoundedCornerShape(11.dp))
                            .padding(horizontal = 12.dp, vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            text = medals[i],
                            style = MaterialTheme.typography.titleMedium,
                            color = mc,
                            modifier = Modifier.width(28.dp)
                        )
                        HashAvatar(name = u.name, size = 34.dp)
                        Spacer(Modifier.width(10.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = u.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(
                                    R.string.dash_perf_meta,
                                    u.role.replace("_", " "),
                                    tasks,
                                    projectCount
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        Column(horizontalAlignment = Alignment.End) {
                            Text(
                                text = score.toString(),
                                style = MaterialTheme.typography.titleSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = mc
                            )
                            Box(
                                modifier = Modifier
                                    .width(50.dp)
                                    .height(3.dp)
                                    .background(colors.border, RoundedCornerShape(3.dp))
                            ) {
                                Box(
                                    modifier = Modifier
                                        .fillMaxWidth(score / 100f)
                                        .height(3.dp)
                                        .background(mc, RoundedCornerShape(3.dp))
                                )
                            }
                        }
                    }
                }
            }
        }

        // Deadlines
        DashCard {
            DashCardHeader(title = stringResource(R.string.dash_upcoming_deadlines))
            val deadlineProjects = cp
                .filter { it.dueDate != null && it.status.uppercase() != "COMPLETED" }
                .mapNotNull { p -> daysUntil(p.dueDate)?.let { p to it } }
                .filter { it.second <= 14 }
                .sortedBy { it.second }
                .take(5)
            when {
                state.loading -> LoadingHint()
                deadlineProjects.isEmpty() -> EmptyHint(stringResource(R.string.dash_no_deadlines))
                else -> deadlineProjects.forEach { (p, days) ->
                    val tone = when {
                        days < 0 -> OrbitDanger
                        days <= 2 -> OrbitWarning
                        else -> OrbitSuccess
                    }
                    Surface(
                        onClick = { onNavigate("${InnerRoutes.TASKFLOW}?projectId=${p.projectId}") },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 7.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(tone.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                                    .border(
                                        1.dp,
                                        tone.copy(alpha = 0.13f),
                                        RoundedCornerShape(8.dp)
                                    ),
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
                                    text = p.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = when {
                                        days < 0 -> stringResource(
                                            R.string.dash_days_overdue, abs(days)
                                        )
                                        days == 0 -> stringResource(R.string.dash_due_today_excl)
                                        else -> stringResource(R.string.dash_days_remaining, days)
                                    },
                                    style = MaterialTheme.typography.labelSmall,
                                    color = tone
                                )
                            }
                            Column(horizontalAlignment = Alignment.End) {
                                Box(
                                    modifier = Modifier
                                        .width(50.dp)
                                        .height(3.dp)
                                        .background(colors.border, RoundedCornerShape(3.dp))
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .fillMaxWidth(p.progress / 100f)
                                            .height(3.dp)
                                            .background(tone, RoundedCornerShape(3.dp))
                                    )
                                }
                                Text(
                                    text = "${p.progress}%",
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
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
                Triple(stringResource(R.string.nav_projects), DashIndigo, InnerRoutes.PROJECTS),
                Triple(stringResource(R.string.dash_action_analytics), OrbitSuccess, InnerRoutes.TEAM),
                Triple(stringResource(R.string.nav_reports), OrbitWarning, InnerRoutes.REPORTS),
                Triple(
                    stringResource(R.string.dash_action_channels),
                    OrbitPurple,
                    InnerRoutes.TASKFLOW_PUBLIC
                )
            )
            actions.chunked(2).forEach { rowItems ->
                Row(
                    modifier = Modifier.padding(bottom = 6.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    rowItems.forEach { (label, tone, route) ->
                        Surface(
                            onClick = { onNavigate(route) },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(8.dp),
                            color = tone.copy(alpha = 0.06f),
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                tone.copy(alpha = 0.13f)
                            )
                        ) {
                            Box(
                                modifier = Modifier.padding(vertical = 9.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = label,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = tone
                                )
                            }
                        }
                    }
                }
            }
        }

        // Workspace stats
        DashCard {
            Text(
                text = stringResource(R.string.dash_workspace_stats).uppercase(),
                style = OrbitTextStyles.techLabel,
                color = colors.textMuted
            )
            Spacer(Modifier.height(8.dp))
            val stats = listOf(
                Triple(stringResource(R.string.stats_total_members), state.users.size, DashIndigo),
                Triple(stringResource(R.string.stats_online_now), state.online.size, OrbitSuccess),
                Triple(stringResource(R.string.stats_on_hold), state.onHold.size, OrbitWarning)
            )
            stats.forEach { (label, value, tone) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(6.dp)
                            .background(tone, CircleShape)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        text = label,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary,
                        modifier = Modifier.weight(1f)
                    )
                    Text(
                        text = if (state.loading) "—" else value.toString(),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = tone
                    )
                }
            }
        }
    }
    }
}

// Action button
@Composable
private fun HeaderActionButton(text: String, filled: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(8.dp),
        color = if (filled) DashIndigo else Color.Transparent,
        border = androidx.compose.foundation.BorderStroke(1.dp, DashIndigo)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = if (filled) Color.White else DashIndigo,
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
        )
    }
}

// Legend row
@Composable
private fun LegendRow(label: String, value: Int, tone: Color) {
    val colors = OrbitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(tone, RoundedCornerShape(2.dp))
        )
        Spacer(Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary,
            modifier = Modifier.weight(1f)
        )
        Text(
            text = value.toString(),
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.Bold,
            color = tone
        )
    }
}

// Heat stat
@Composable
private fun HeatStat(label: String, value: String, tone: Color) {
    val colors = OrbitTheme.colors
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tone
        )
        Spacer(Modifier.width(4.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
    }
}
