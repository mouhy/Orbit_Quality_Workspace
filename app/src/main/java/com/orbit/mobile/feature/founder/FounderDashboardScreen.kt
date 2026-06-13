package com.orbit.mobile.feature.founder

import androidx.compose.foundation.background
import androidx.compose.foundation.border
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
import androidx.compose.runtime.setValue
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
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTeal
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.BarEntry
import com.orbit.mobile.core.ui.charts.GaugeArc
import com.orbit.mobile.core.ui.charts.VerticalBarChart
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.data.dto.EntityCreateRequest
import com.orbit.mobile.data.dto.EntityDto
import com.orbit.mobile.data.dto.FrameworkDto
import com.orbit.mobile.data.dto.FrameworkRequest
import com.orbit.mobile.feature.auth.AuthErrorBox
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.KpiCard
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.shell.InnerRoutes
import kotlin.math.roundToInt

/** Founder command center: metrics, financials, AI trends, alerts, frameworks and entities. */
@Composable
fun FounderDashboardScreen(
    onNavigate: (String) -> Unit,
    viewModel: FounderViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val metrics = state.metrics

    // Sheet targets for frameworks / entities management
    var frameworkForm by remember { mutableStateOf(false) }
    var editFramework by remember { mutableStateOf<FrameworkDto?>(null) }
    var entityForm by remember { mutableStateOf(false) }
    var assignEntity by remember { mutableStateOf<EntityDto?>(null) }
    var sheetError by remember { mutableStateOf<UiText?>(null) }

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

        when {
            state.loading -> LoadingHint()
            metrics != null -> {
                // Platform health gauge
                DashCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        GaugeArc(score = metrics.platformHealthScore, label = "", size = 120.dp)
                        Text(
                            text = stringResource(R.string.fd_platform_health).uppercase(),
                            style = OrbitTextStyles.techLabel,
                            color = colors.textMuted
                        )
                    }
                }

                // Core KPI grid
                val spark = listOf(1, 2, 2, 3, 3, 4)
                val kpis = listOf(
                    Triple(stringResource(R.string.fd_entities), metrics.totalEntities.toString(), OrbitPrimary),
                    Triple(stringResource(R.string.title_teams), metrics.totalTeams.toString(), OrbitTeal),
                    Triple(stringResource(R.string.stats_total_members), metrics.totalUsers.toString(), OrbitPurple),
                    Triple(stringResource(R.string.fd_active_now), metrics.activeNow.toString(), OrbitSuccess),
                    Triple(stringResource(R.string.fd_pending_evals), metrics.pendingAiEvals.toString(), OrbitWarning),
                    Triple(stringResource(R.string.qc_kpi_avg_score), "${metrics.avgQualityScore.roundToInt()}%", OrbitSuccess),
                    Triple(stringResource(R.string.fd_total_reports), metrics.totalReports.toString(), OrbitPrimary),
                    Triple(stringResource(R.string.qc_kpi_pass_rate), "${metrics.passRate.roundToInt()}%", OrbitPurple)
                )
                kpis.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEach { (label, value, tone) ->
                            KpiCard(
                                label = label,
                                value = value,
                                color = tone,
                                spark = spark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // MRR + subscription tiers
                DashCard {
                    DashCardHeader(
                        title = stringResource(R.string.fd_financials),
                        subtitle = stringResource(R.string.fd_financials_sub)
                    )
                    Text(
                        text = "$${metrics.financials.mrr.roundToInt()}",
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.ExtraBold,
                        color = OrbitSuccess
                    )
                    Text(
                        text = stringResource(R.string.fd_mrr),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(10.dp))
                    val tierPalette = listOf(OrbitPrimary, OrbitPurple, OrbitWarning, OrbitTeal)
                    metrics.financials.tierDistribution.entries.forEachIndexed { i, entry ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            TinyPill(entry.key, tierPalette[i % tierPalette.size])
                            Spacer(Modifier.weight(1f))
                            Text(
                                text = entry.value.toString(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = tierPalette[i % tierPalette.size]
                            )
                        }
                    }
                }

                // 7-day AI accept/reject trend
                DashCard {
                    DashCardHeader(
                        title = stringResource(R.string.fd_ai_trends),
                        subtitle = stringResource(R.string.fd_ai_trends_sub)
                    )
                    if (metrics.aiTrends.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tp_no_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    } else {
                        VerticalBarChart(
                            bars = metrics.aiTrends.takeLast(7).map {
                                BarEntry(it.day.takeLast(5), it.accepted, OrbitSuccess)
                            },
                            height = 100.dp
                        )
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            TinyPill(
                                "${stringResource(R.string.fd_accepted)} ${
                                    metrics.aiTrends.sumOf { it.accepted }
                                }",
                                OrbitSuccess
                            )
                            TinyPill(
                                "${stringResource(R.string.fd_rejected)} ${
                                    metrics.aiTrends.sumOf { it.rejected }
                                }",
                                OrbitDanger
                            )
                        }
                    }
                }

                // System alerts feed
                DashCard {
                    DashCardHeader(title = stringResource(R.string.fd_alerts))
                    if (metrics.systemAlerts.isEmpty()) {
                        Text(
                            text = stringResource(R.string.fd_no_alerts),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    metrics.systemAlerts.take(6).forEach { alert ->
                        val tone = when (alert.type) {
                            "error" -> OrbitDanger
                            "warning" -> OrbitWarning
                            "success" -> OrbitSuccess
                            else -> OrbitPrimary
                        }
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp, 30.dp)
                                    .background(tone, RoundedCornerShape(4.dp))
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = alert.message,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textPrimary
                                )
                                Text(
                                    text = timeAgo(alert.time).asString(),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                        }
                    }
                }

                // Frameworks management
                DashCard {
                    DashCardHeader(
                        title = stringResource(R.string.fd_frameworks),
                        action = {
                            IconButton(onClick = { frameworkForm = true }) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = stringResource(R.string.action_create),
                                    tint = OrbitPrimary
                                )
                            }
                        }
                    )
                    if (state.frameworks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tp_no_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    state.frameworks.forEach { framework ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = framework.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        R.string.fd_threshold,
                                        framework.acceptanceThreshold ?: 70
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                            TinyPill(
                                if (framework.isActive == true) {
                                    stringResource(R.string.it_status_active)
                                } else stringResource(R.string.it_status_suspended),
                                if (framework.isActive == true) OrbitSuccess else colors.textMuted
                            )
                            IconButton(
                                onClick = { editFramework = framework },
                                modifier = Modifier.size(28.dp)
                            ) {
                                Icon(
                                    Icons.Outlined.Edit,
                                    contentDescription = stringResource(R.string.action_edit),
                                    tint = colors.textSecondary,
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }
                    }
                }

                // Entities management
                DashCard {
                    DashCardHeader(
                        title = stringResource(R.string.fd_entities),
                        action = {
                            IconButton(onClick = { entityForm = true }) {
                                Icon(
                                    Icons.Outlined.Add,
                                    contentDescription = stringResource(R.string.action_create),
                                    tint = OrbitPrimary
                                )
                            }
                        }
                    )
                    if (state.entities.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tp_no_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    state.entities.forEach { entity ->
                        Surface(
                            onClick = { assignEntity = entity },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = entity.name,
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = stringResource(
                                            R.string.fd_entity_meta,
                                            entity.userCount ?: 0,
                                            entity.itStaffIds.size
                                        ),
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                                TinyPill(entity.subscriptionTier ?: "Basic", OrbitPurple)
                            }
                        }
                    }
                }

                // Quick actions
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OrbitButton(
                        text = stringResource(R.string.nav_it_accounts),
                        onClick = { onNavigate(InnerRoutes.FOUNDER_ACCOUNTS) },
                        variant = OrbitButtonVariant.Secondary,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
    }

    // Framework create/edit sheet
    if (frameworkForm || editFramework != null) {
        FrameworkFormSheet(
            editing = editFramework,
            busy = state.busy,
            externalError = sheetError,
            onDismiss = {
                frameworkForm = false
                editFramework = null
                sheetError = null
            },
            onSave = { id, body ->
                viewModel.saveFramework(id, body) { ok, err ->
                    if (ok) {
                        frameworkForm = false
                        editFramework = null
                    } else sheetError = err
                }
            }
        )
    }

    // Entity creation sheet
    if (entityForm) {
        EntityFormSheet(
            busy = state.busy,
            externalError = sheetError,
            onDismiss = {
                entityForm = false
                sheetError = null
            },
            onCreate = { body ->
                viewModel.createEntity(body) { ok, err ->
                    if (ok) entityForm = false else sheetError = err
                }
            }
        )
    }

    // IT assignment sheet
    assignEntity?.let { entity ->
        AssignItSheet(
            entity = entity,
            accounts = state.accounts,
            busy = state.busy,
            onDismiss = { assignEntity = null },
            onAssign = { ids ->
                viewModel.assignIt(entity.id, ids) { ok, _ ->
                    if (ok) assignEntity = null
                }
            }
        )
    }
}

/** Form for creating or editing a quality framework (prompt + threshold slider). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun FrameworkFormSheet(
    editing: FrameworkDto?,
    busy: Boolean,
    externalError: UiText?,
    onDismiss: () -> Unit,
    onSave: (String?, FrameworkRequest) -> Unit
) {
    val colors = OrbitTheme.colors
    var name by remember { mutableStateOf(editing?.name ?: "") }
    var description by remember { mutableStateOf(editing?.description ?: "") }
    var prompt by remember { mutableStateOf(editing?.aiPromptTemplate ?: "") }
    var threshold by remember {
        mutableStateOf((editing?.acceptanceThreshold ?: 70).toFloat())
    }

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
                text = if (editing == null) stringResource(R.string.fd_new_framework)
                else stringResource(R.string.fd_edit_framework),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            OrbitTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.field_name),
                placeholder = stringResource(R.string.fd_framework_placeholder)
            )
            OrbitTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.pj_desc_label),
                placeholder = stringResource(R.string.pj_desc_placeholder),
                singleLine = false,
                minLines = 2
            )
            OrbitTextField(
                value = prompt,
                onValueChange = { prompt = it },
                label = stringResource(R.string.fd_prompt_label),
                placeholder = stringResource(R.string.fd_prompt_placeholder),
                singleLine = false,
                minLines = 3
            )
            // Acceptance threshold slider (0-100)
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.fd_threshold, threshold.roundToInt()),
                    style = MaterialTheme.typography.labelMedium,
                    color = colors.textSecondary,
                    modifier = Modifier.width(120.dp)
                )
                Slider(
                    value = threshold,
                    onValueChange = { threshold = it },
                    valueRange = 0f..100f,
                    colors = SliderDefaults.colors(
                        thumbColor = OrbitPrimary,
                        activeTrackColor = OrbitPrimary
                    ),
                    modifier = Modifier.weight(1f)
                )
            }
            externalError?.let { AuthErrorBox(message = it.asString()) }
            OrbitButton(
                text = stringResource(R.string.action_save),
                onClick = {
                    onSave(
                        editing?.id,
                        FrameworkRequest(
                            name = name.trim(),
                            description = description,
                            aiPromptTemplate = prompt,
                            acceptanceThreshold = threshold.roundToInt()
                        )
                    )
                },
                enabled = name.isNotBlank() && prompt.length >= 10 && !busy,
                loading = busy,
                fullWidth = true
            )
        }
    }
}

/** Creates a tenant entity with a subscription tier. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun EntityFormSheet(
    busy: Boolean,
    externalError: UiText?,
    onDismiss: () -> Unit,
    onCreate: (EntityCreateRequest) -> Unit
) {
    val colors = OrbitTheme.colors
    var name by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var tier by remember { mutableStateOf("Basic") }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.fd_new_entity),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            OrbitTextField(
                value = name,
                onValueChange = { name = it },
                label = stringResource(R.string.field_name),
                placeholder = stringResource(R.string.fd_entity_placeholder)
            )
            OrbitTextField(
                value = description,
                onValueChange = { description = it },
                label = stringResource(R.string.pj_desc_label),
                placeholder = stringResource(R.string.pj_desc_placeholder),
                singleLine = false,
                minLines = 2
            )
            // Subscription tier chips
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf("Basic", "Pro", "Enterprise").forEach { option ->
                    val active = tier == option
                    Surface(
                        onClick = { tier = option },
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) OrbitPurple else colors.surface2,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            colors.borderStrong
                        )
                    ) {
                        Text(
                            text = option,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            externalError?.let { AuthErrorBox(message = it.asString()) }
            OrbitButton(
                text = stringResource(R.string.action_create),
                onClick = {
                    onCreate(
                        EntityCreateRequest(
                            name = name.trim(),
                            description = description,
                            subscriptionTier = tier
                        )
                    )
                },
                enabled = name.isNotBlank() && !busy,
                loading = busy,
                fullWidth = true
            )
        }
    }
}

/** Multi-select of IT staff accounts to attach to an entity. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun AssignItSheet(
    entity: EntityDto,
    accounts: List<com.orbit.mobile.data.dto.FounderAccountDto>,
    busy: Boolean,
    onDismiss: () -> Unit,
    onAssign: (List<String>) -> Unit
) {
    val colors = OrbitTheme.colors
    var selected by remember { mutableStateOf(entity.itStaffIds.toSet()) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = stringResource(R.string.fd_assign_it, entity.name),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Column(
                modifier = Modifier
                    .heightIn(max = 320.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                accounts.forEach { account ->
                    val active = selected.contains(account.id)
                    Surface(
                        onClick = {
                            selected = if (active) selected - account.id
                            else selected + account.id
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 3.dp),
                        shape = RoundedCornerShape(10.dp),
                        color = if (active) OrbitPrimary.copy(alpha = 0.08f)
                        else Color.Transparent,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) OrbitPrimary else colors.border
                        )
                    ) {
                        Column(modifier = Modifier.padding(10.dp)) {
                            Text(
                                text = account.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Text(
                                text = account.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
            OrbitButton(
                text = stringResource(R.string.action_save),
                onClick = { onAssign(selected.toList()) },
                enabled = !busy,
                loading = busy,
                fullWidth = true
            )
        }
    }
}
