package com.orbit.mobile.feature.reports

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSky
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTeal
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.theme.projectStatusColor
import com.orbit.mobile.core.ui.charts.DonutSegment
import com.orbit.mobile.core.ui.charts.GaugeArc
import com.orbit.mobile.core.ui.charts.MiniDonut
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.api.AnalyticsApi
import com.orbit.mobile.data.dto.PlatformOverviewDto
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.KpiCard
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// Reports state
data class ReportsState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val overview: PlatformOverviewDto? = null
)

// Reports VM
@HiltViewModel
class ReportsViewModel @Inject constructor(
    private val api: AnalyticsApi
) : ViewModel() {

    private val _state = MutableStateFlow(ReportsState())
    val state: StateFlow<ReportsState> = _state

    init {
        refresh()
    }

    // Load overview
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            safeApiCall { api.platformOverview() }
                .onSuccess { data ->
                    _state.update { it.copy(loading = false, overview = data) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }
}

// Reports page
@Composable
fun ReportsScreen(
    viewModel: ReportsViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val kpis = state.overview?.kpis

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
            kpis != null -> {
                // Health gauge
                DashCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        val health = if (kpis.totalProjects == 0) 0
                        else (kpis.completedProjects * 100 / kpis.totalProjects)
                        GaugeArc(score = health, label = "", size = 120.dp)
                        Text(
                            text = stringResource(R.string.rp_workspace_health),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                // KPI grid
                val spark = listOf(1, 2, 2, 3, 3, 4)
                val cards = listOf(
                    Triple(stringResource(R.string.kpi_total_projects), kpis.totalProjects.toString(), OrbitPrimary),
                    Triple(stringResource(R.string.kpi_active_projects), kpis.activeProjects.toString(), OrbitSuccess),
                    Triple(stringResource(R.string.status_completed), kpis.completedProjects.toString(), OrbitPurple),
                    Triple(stringResource(R.string.status_on_hold), kpis.onHoldProjects.toString(), OrbitWarning),
                    Triple(stringResource(R.string.rp_overdue_projects), kpis.overdueProjects.toString(), OrbitDanger),
                    Triple(stringResource(R.string.rp_total_tasks), kpis.totalTasks.toString(), OrbitSky),
                    Triple(stringResource(R.string.rp_done_tasks), kpis.doneTasks.toString(), OrbitSuccess),
                    Triple(stringResource(R.string.rp_overdue_tasks), kpis.overdueTasks.toString(), OrbitDanger),
                    Triple(stringResource(R.string.rp_due_today), kpis.tasksDueToday.toString(), OrbitWarning),
                    Triple(stringResource(R.string.stats_total_members), kpis.totalUsers.toString(), OrbitPrimary),
                    Triple(stringResource(R.string.stats_online_now), kpis.onlineUsers.toString(), OrbitTeal),
                    Triple(stringResource(R.string.rp_quality_reviews), kpis.qualityReviews.toString(), OrbitPurple)
                )
                cards.chunked(2).forEach { rowItems ->
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

                // Status donut
                DashCard {
                    DashCardHeader(title = stringResource(R.string.dash_status_distribution))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        MiniDonut(
                            segments = listOf(
                                DonutSegment(kpis.activeProjects, OrbitPrimary),
                                DonutSegment(kpis.completedProjects, OrbitSuccess),
                                DonutSegment(kpis.onHoldProjects, OrbitWarning)
                            ),
                            centerLabel = kpis.totalProjects.toString(),
                            centerSub = stringResource(R.string.dash_donut_projects),
                            size = 100.dp
                        )
                        Spacer(Modifier.width(16.dp))
                        Column {
                            TinyPill(
                                "${stringResource(R.string.rp_quality_pass)} ${
                                    kpis.qualityPassRate.roundToInt()
                                }%",
                                OrbitSuccess
                            )
                            Spacer(Modifier.height(6.dp))
                            TinyPill(
                                "${stringResource(R.string.qc_kpi_avg_score)} ${
                                    kpis.avgQualityScore.roundToInt()
                                }%",
                                OrbitPurple
                            )
                        }
                    }
                }

                // Projects health
                DashCard {
                    DashCardHeader(
                        title = stringResource(R.string.dash_project_health),
                        subtitle = stringResource(R.string.rp_projects_sub)
                    )
                    state.overview?.projects?.take(10)?.forEach { project ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            ProgressRing(
                                pct = project.progress,
                                color = projectStatusColor(project.status),
                                size = 38.dp,
                                thick = 3.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Column(modifier = Modifier.weight(1f)) {
                                Text(
                                    text = project.title,
                                    style = MaterialTheme.typography.labelMedium,
                                    fontWeight = FontWeight.Bold,
                                    color = colors.textPrimary,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                Text(
                                    text = stringResource(
                                        R.string.rp_tasks_meta,
                                        project.doneTasks,
                                        project.taskCount
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted
                                )
                            }
                            project.aiScore?.let {
                                TinyPill("AI ${it.roundToInt()}%", OrbitPurple)
                            }
                        }
                    }
                }
            }
        }
    }
    }
}
