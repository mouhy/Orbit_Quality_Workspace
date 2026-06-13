package com.orbit.mobile.feature.qc

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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Archive
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.AreaLineChart
import com.orbit.mobile.core.ui.charts.BarEntry
import com.orbit.mobile.core.ui.charts.HeatGrid
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.charts.VerticalBarChart
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.Downloader
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.data.dto.RuleDto
import com.orbit.mobile.data.dto.StandardCreateRequest
import com.orbit.mobile.data.dto.StandardDto
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.KpiCard
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// QC tabs
private enum class QcTab { INTELLIGENCE, OVERVIEW, STANDARDS, EVALUATIONS, ANALYTICS }

// QC dashboard
@Composable
fun QCDashboardScreen(
    viewModel: QcViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(QcTab.INTELLIGENCE) }
    var showStandardForm by remember { mutableStateOf(false) }
    var editStandard by remember { mutableStateOf<StandardDto?>(null) }

    fun export(format: String) {
        scope.launch {
            val body = viewModel.export(format) ?: return@launch
            val file = Downloader.saveToCache(context, body, "qc_report.$format")
            Downloader.open(context, file)
        }
    }

    // Return refresh
    OnReturnRefresh { viewModel.refresh() }

    // Pull refresh
    OrbitPullRefresh(
        refreshing = state.loading,
        onRefresh = { viewModel.refresh() }
    ) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

        // Tabs
        Row(
            modifier = Modifier.horizontalScroll(rememberScrollState()),
            horizontalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            QcTab.entries.forEach { t ->
                val active = tab == t
                val label = when (t) {
                    QcTab.INTELLIGENCE -> stringResource(R.string.qc_tab_intelligence)
                    QcTab.OVERVIEW -> stringResource(R.string.pj_tab_overview)
                    QcTab.STANDARDS -> stringResource(R.string.qc_tab_standards)
                    QcTab.EVALUATIONS -> stringResource(R.string.qc_tab_evaluations)
                    QcTab.ANALYTICS -> stringResource(R.string.dash_action_analytics)
                }
                Surface(
                    onClick = { tab = t },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) OrbitPrimary else colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) OrbitPrimary else colors.borderStrong
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        when {
            state.loading -> LoadingHint()
            else -> when (tab) {
                QcTab.INTELLIGENCE -> IntelligenceTab(state)
                QcTab.OVERVIEW -> OverviewTab(state)
                QcTab.STANDARDS -> StandardsTab(
                    state = state,
                    onCreate = { showStandardForm = true },
                    onEdit = { editStandard = it },
                    onArchive = viewModel::archiveStandard
                )
                QcTab.EVALUATIONS -> EvaluationsTab(state)
                QcTab.ANALYTICS -> AnalyticsTab(state, onExport = { export(it) })
            }
        }
    }
    }

    if (showStandardForm || editStandard != null) {
        StandardFormSheet(
            editing = editStandard,
            busy = state.busy,
            onDismiss = {
                showStandardForm = false
                editStandard = null
            },
            onSave = { id, body ->
                viewModel.saveStandard(id, body) { ok, _ ->
                    if (ok) {
                        showStandardForm = false
                        editStandard = null
                    }
                }
            }
        )
    }
}

// Intelligence tab
@Composable
private fun IntelligenceTab(state: QcState) {
    val colors = OrbitTheme.colors
    val spark = state.overview?.scoreTrend?.map { it.avgScore.roundToInt() }
        ?.takeIf { it.size >= 2 } ?: listOf(0, 0)

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                label = stringResource(R.string.qc_kpi_evaluations),
                value = state.evaluations.size.toString(),
                color = OrbitPrimary,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.qc_kpi_avg_score),
                value = "${state.avgScore}%",
                color = if (state.avgScore >= 70) OrbitSuccess else OrbitWarning,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
        }
        Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
            KpiCard(
                label = stringResource(R.string.qc_kpi_standards),
                value = state.standards.count { it.status == "active" }.toString(),
                color = OrbitPurple,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
            KpiCard(
                label = stringResource(R.string.qc_kpi_pass_rate),
                value = "${
                    if (state.evaluations.isEmpty()) 0
                    else state.evaluations.count { it.complianceScore >= 70 } * 100 /
                        state.evaluations.size
                }%",
                color = OrbitSuccess,
                spark = spark,
                modifier = Modifier.weight(1f)
            )
        }
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.qc_bottlenecks_title),
                subtitle = stringResource(R.string.qc_bottlenecks_sub)
            )
            val bottlenecks = state.overview?.bottlenecks ?: emptyList()
            if (bottlenecks.isEmpty()) {
                Text(
                    text = stringResource(R.string.tp_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
            bottlenecks.take(6).forEach { item ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = item.name,
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textPrimary,
                        modifier = Modifier.weight(1f),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    TinyPill(
                        stringResource(R.string.qc_hours, item.avgHours.roundToInt()),
                        OrbitWarning
                    )
                }
            }
        }
    }
}

// Overview tab
@Composable
private fun OverviewTab(state: QcState) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.qc_trend_title),
                subtitle = stringResource(R.string.qc_trend_sub)
            )
            val trend = state.overview?.scoreTrend ?: emptyList()
            if (trend.size >= 2) {
                AreaLineChart(
                    data = trend.map { it.avgScore.roundToInt() },
                    labels = trend.takeLast(7).map { it.date.takeLast(5) },
                    color = OrbitSuccess,
                    height = 100.dp
                )
            } else {
                Text(
                    text = stringResource(R.string.tp_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = OrbitTheme.colors.textMuted
                )
            }
        }
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.qc_heatmap_title),
                subtitle = stringResource(R.string.qc_heatmap_sub)
            )
            HeatGrid(data = state.heatmap, height = 96.dp, cellColor = OrbitPurple)
        }
    }
}

// Standards tab
@Composable
private fun StandardsTab(
    state: QcState,
    onCreate: () -> Unit,
    onEdit: (StandardDto) -> Unit,
    onArchive: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        OrbitButton(
            text = stringResource(R.string.qc_new_standard),
            onClick = onCreate,
            leadingIcon = Icons.Outlined.Add,
            fullWidth = true
        )
        if (state.standards.isEmpty()) {
            Text(
                text = stringResource(R.string.qc_no_standards),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        state.standards.forEach { standard ->
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = standard.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = stringResource(
                                    R.string.qc_rules_count,
                                    standard.rules.size
                                ),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        TinyPill(
                            standard.status,
                            if (standard.status == "active") OrbitSuccess else colors.textMuted
                        )
                        IconButton(
                            onClick = { onEdit(standard) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                        IconButton(
                            onClick = { onArchive(standard.standardId) },
                            modifier = Modifier.size(28.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Archive,
                                contentDescription = stringResource(R.string.qc_archive),
                                tint = OrbitDanger,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    standard.description?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

// Evaluations tab
@Composable
private fun EvaluationsTab(state: QcState) {
    val colors = OrbitTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        if (state.evaluations.isEmpty()) {
            Text(
                text = stringResource(R.string.ai_no_history),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted
            )
        }
        state.evaluations.forEach { item ->
            val score = item.complianceScore.roundToInt()
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = colors.surface,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
            ) {
                Row(
                    modifier = Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    ProgressRing(
                        pct = score,
                        color = if (score >= 70) OrbitSuccess else OrbitDanger,
                        size = 42.dp,
                        thick = 4.dp
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = item.taskTitle,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            item.reportType?.let { TinyPill(it, OrbitPrimary) }
                            Text(
                                text = timeAgo(item.createdAt).asString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
}

// Analytics tab
@Composable
private fun AnalyticsTab(state: QcState, onExport: (String) -> Unit) {
    val colors = OrbitTheme.colors
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.qc_project_scores),
                subtitle = stringResource(R.string.qc_project_scores_sub)
            )
            val scores = state.overview?.projectScores ?: emptyList()
            if (scores.isEmpty()) {
                Text(
                    text = stringResource(R.string.tp_no_data),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            } else {
                VerticalBarChart(
                    bars = scores.take(5).map {
                        BarEntry(
                            it.displayName.take(8),
                            it.displayScore.roundToInt(),
                            OrbitPrimary
                        )
                    },
                    height = 120.dp
                )
            }
        }
        DashCard {
            DashCardHeader(title = stringResource(R.string.qc_export_title))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbitButton(
                    text = stringResource(R.string.qc_export_csv),
                    onClick = { onExport("csv") },
                    variant = OrbitButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
                OrbitButton(
                    text = stringResource(R.string.qc_export_pdf),
                    onClick = { onExport("pdf") },
                    variant = OrbitButtonVariant.Secondary,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}

// Standard form
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun StandardFormSheet(
    editing: StandardDto?,
    busy: Boolean,
    onDismiss: () -> Unit,
    onSave: (String?, StandardCreateRequest) -> Unit
) {
    val colors = OrbitTheme.colors
    var title by remember { mutableStateOf(editing?.title ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var rules by remember { mutableStateOf(editing?.rules ?: emptyList()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .heightIn(max = 560.dp)
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = if (editing == null) stringResource(R.string.qc_new_standard)
                else stringResource(R.string.qc_edit_standard),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            OrbitTextField(
                value = title,
                onValueChange = { title = it },
                label = stringResource(R.string.ev_title_label),
                placeholder = stringResource(R.string.qc_standard_placeholder)
            )
            OrbitTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.pj_desc_label),
                placeholder = stringResource(R.string.pj_desc_placeholder),
                singleLine = false,
                minLines = 2
            )

            Text(
                text = stringResource(R.string.qc_rules_title).uppercase(),
                style = OrbitTextStyles.techLabel,
                color = colors.textMuted
            )
            rules.forEachIndexed { index, rule ->
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                        .padding(10.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OrbitTextField(
                        value = rule.label,
                        onValueChange = { value ->
                            rules = rules.mapIndexed { i, r ->
                                if (i == index) r.copy(label = value) else r
                            }
                        },
                        placeholder = stringResource(R.string.qc_rule_label)
                    )
                    OrbitTextField(
                        value = rule.instructions ?: "",
                        onValueChange = { value ->
                            rules = rules.mapIndexed { i, r ->
                                if (i == index) r.copy(instructions = value.ifBlank { null })
                                else r
                            }
                        },
                        placeholder = stringResource(R.string.qc_rule_instructions)
                    )
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(
                                R.string.qc_rule_weight,
                                (rule.weight * 100).roundToInt()
                            ),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            modifier = Modifier.width(90.dp)
                        )
                        Slider(
                            value = rule.weight.toFloat(),
                            onValueChange = { value ->
                                rules = rules.mapIndexed { i, r ->
                                    if (i == index) r.copy(weight = value.toDouble()) else r
                                }
                            },
                            valueRange = 0f..1f,
                            colors = SliderDefaults.colors(
                                thumbColor = OrbitPrimary,
                                activeTrackColor = OrbitPrimary
                            ),
                            modifier = Modifier.weight(1f)
                        )
                        Surface(
                            onClick = {
                                rules = rules.filterIndexed { i, _ -> i != index }
                            },
                            color = Color.Transparent
                        ) {
                            Text(
                                text = stringResource(R.string.action_remove),
                                style = MaterialTheme.typography.labelSmall,
                                color = OrbitDanger,
                                modifier = Modifier.padding(4.dp)
                            )
                        }
                    }
                }
            }
            OrbitButton(
                text = stringResource(R.string.qc_add_rule),
                onClick = { rules = rules + RuleDto(label = "") },
                variant = OrbitButtonVariant.Secondary,
                leadingIcon = Icons.Outlined.Add
            )

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                OrbitButton(
                    text = stringResource(R.string.action_cancel),
                    onClick = onDismiss,
                    variant = OrbitButtonVariant.Ghost,
                    modifier = Modifier.weight(1f)
                )
                OrbitButton(
                    text = stringResource(R.string.action_save),
                    onClick = {
                        onSave(
                            editing?.standardId,
                            StandardCreateRequest(
                                title = title.trim(),
                                description = description.ifBlank { null },
                                rules = rules.filter { it.label.isNotBlank() }
                            )
                        )
                    },
                    enabled = title.trim().length >= 3 && !busy,
                    loading = busy,
                    modifier = Modifier.weight(1f)
                )
            }
        }
    }
}
