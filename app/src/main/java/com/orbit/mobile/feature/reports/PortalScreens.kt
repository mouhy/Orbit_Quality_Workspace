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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.theme.roleColor
import com.orbit.mobile.core.ui.charts.GaugeArc
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.arr
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.dbl
import com.orbit.mobile.core.util.int
import com.orbit.mobile.core.util.obj
import com.orbit.mobile.core.util.str
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.api.AnalyticsApi
import com.orbit.mobile.data.api.ProfileApi
import com.orbit.mobile.data.dto.UserAnalyticsDto
import com.orbit.mobile.domain.repository.MemberAnalyticsRepository
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.KpiCard
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonObject
import javax.inject.Inject
import kotlin.math.roundToInt

// SubAdmin state
data class SubAdminState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val data: JsonObject? = null
)

// SubAdmin VM
@HiltViewModel
class SubAdminViewModel @Inject constructor(
    private val api: AnalyticsApi
) : ViewModel() {

    private val _state = MutableStateFlow(SubAdminState())
    val state: StateFlow<SubAdminState> = _state

    init {
        refresh()
    }

    // Load dashboard
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            safeApiCall { api.subadminDashboard() }
                .onSuccess { data -> _state.update { it.copy(loading = false, data = data) } }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }
}

// Shared portal
@Composable
private fun SubAdminAnalyticsContent(
    state: SubAdminState,
    onRetry: () -> Unit
) {
    val colors = OrbitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.error?.let { ErrorBanner(it.asString(), onRetry) }

        when {
            state.loading -> LoadingHint()
            state.data != null -> {
                val data = state.data
                val kpis = data.obj("kpis") ?: data

                // Health gauge
                val health = kpis.int("health_score") ?: kpis.int("healthScore")
                ?: kpis.int("avg_progress") ?: 0
                DashCard {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                val kpiCards = listOfNotNull(
                    kpis.int("total_projects")?.let {
                        Triple(stringResource(R.string.kpi_total_projects), it, OrbitPrimary)
                    },
                    kpis.int("active_projects")?.let {
                        Triple(stringResource(R.string.kpi_active_projects), it, OrbitSuccess)
                    },
                    kpis.int("total_tasks")?.let {
                        Triple(stringResource(R.string.rp_total_tasks), it, OrbitWarning)
                    },
                    kpis.int("team_members")?.let {
                        Triple(stringResource(R.string.stats_total_members), it, OrbitPurple)
                    }
                )
                kpiCards.chunked(2).forEach { rowItems ->
                    Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                        rowItems.forEach { (label, value, tone) ->
                            KpiCard(
                                label = label,
                                value = value.toString(),
                                color = tone,
                                spark = spark,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Projects list
                val projects = data.arr("projects")
                if (projects != null && projects.isNotEmpty()) {
                    DashCard {
                        DashCardHeader(
                            title = stringResource(R.string.nav_projects),
                            subtitle = stringResource(R.string.sa_projects_sub)
                        )
                        projects.take(10).forEach { element ->
                            val project = runCatching { element.jsonObject }.getOrNull()
                                ?: return@forEach
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProgressRing(
                                    pct = project.int("progress") ?: 0,
                                    color = OrbitPrimary,
                                    size = 36.dp,
                                    thick = 3.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Column(modifier = Modifier.weight(1f)) {
                                    Text(
                                        text = project.str("title") ?: "",
                                        style = MaterialTheme.typography.labelMedium,
                                        fontWeight = FontWeight.Bold,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = project.str("status") ?: "",
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                                (project.dbl("ai_score") ?: project.dbl("aiScore"))?.let {
                                    TinyPill("AI ${it.roundToInt()}%", OrbitPurple)
                                }
                            }
                        }
                    }
                }

                // Team distribution
                val team = data.arr("team_distribution") ?: data.arr("team")
                if (team != null && team.isNotEmpty()) {
                    DashCard {
                        DashCardHeader(title = stringResource(R.string.sa_team_title))
                        team.take(10).forEach { element ->
                            val member = runCatching { element.jsonObject }.getOrNull()
                                ?: return@forEach
                            val name = member.str("name") ?: member.str("user_name") ?: ""
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HashAvatar(name = name.ifBlank { "U" }, size = 30.dp)
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                member.int("tasks")?.let {
                                    TinyPill(
                                        stringResource(R.string.tp_tasks_pill, it),
                                        OrbitSuccess
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

// SubAdmin portal
@Composable
fun SubAdminPortalScreen(viewModel: SubAdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubAdminAnalyticsContent(state = state, onRetry = viewModel::refresh)
}

// TaskMaster page
@Composable
fun TaskMasterScreen(viewModel: SubAdminViewModel = hiltViewModel()) {
    val state by viewModel.state.collectAsStateWithLifecycle()
    SubAdminAnalyticsContent(state = state, onRetry = viewModel::refresh)
}

// Portfolio state
data class PortfolioState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val profile: JsonObject? = null,
    val portfolio: JsonObject? = null,
    val analytics: UserAnalyticsDto? = null
)

// Portfolio VM
@HiltViewModel
class PortfolioViewModel @Inject constructor(
    private val profileApi: ProfileApi,
    private val analyticsRepo: MemberAnalyticsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val userId: String = savedStateHandle.get<String>("userId") ?: ""

    private val _state = MutableStateFlow(PortfolioState())
    val state: StateFlow<PortfolioState> = _state

    init {
        refresh()
    }

    // Load all
    fun refresh() {
        if (userId.isBlank()) {
            _state.update { it.copy(loading = false) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val profileDeferred = async { safeApiCall { profileApi.profile(userId) } }
            val portfolioDeferred = async { safeApiCall { profileApi.portfolio(userId) } }
            val analyticsDeferred = async { analyticsRepo.userAnalytics(userId) }

            val profileResult = profileDeferred.await()
            val portfolioResult = portfolioDeferred.await()
            val analyticsResult = analyticsDeferred.await()

            _state.update { current ->
                var next = current.copy(loading = false)
                when (profileResult) {
                    is ApiResult.Success -> next = next.copy(profile = profileResult.data)
                    is ApiResult.Failure ->
                        next = next.copy(error = profileResult.error.toUiText())
                }
                (portfolioResult as? ApiResult.Success)?.let {
                    next = next.copy(portfolio = it.data)
                }
                (analyticsResult as? ApiResult.Success)?.let {
                    next = next.copy(analytics = it.data)
                }
                next
            }
        }
    }
}

// Portfolio page
@Composable
fun PortfolioScreen(viewModel: PortfolioViewModel = hiltViewModel()) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

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
            else -> {
                val profile = state.profile
                val name = profile?.str("name") ?: profile?.str("full_name") ?: ""
                val role = profile?.str("role") ?: ""

                DashCard {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        HashAvatar(name = name.ifBlank { "U" }, size = 56.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = name,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = profile?.str("email") ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                            Spacer(Modifier.height(4.dp))
                            if (role.isNotBlank()) {
                                TinyPill(role.replace("_", " "), roleColor(role))
                            }
                        }
                    }
                    profile?.str("bio")?.takeIf { it.isNotBlank() }?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = it,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                    }
                }

                state.analytics?.let { analytics ->
                    DashCard {
                        DashCardHeader(title = stringResource(R.string.pf_stats_title))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            PortfolioStat(
                                value = analytics.tasksDone.toString(),
                                label = stringResource(R.string.tp_stat_done),
                                tone = OrbitSuccess
                            )
                            PortfolioStat(
                                value = analytics.tasksInProgress.toString(),
                                label = stringResource(R.string.tp_stat_in_progress),
                                tone = OrbitPrimary
                            )
                            PortfolioStat(
                                value = "${(analytics.avgAccuracy * 100).roundToInt()}%",
                                label = stringResource(R.string.tp_ai_quality),
                                tone = OrbitPurple
                            )
                        }
                    }
                }

                val items = state.portfolio?.arr("projects")
                    ?: state.portfolio?.arr("items")
                if (items != null && items.isNotEmpty()) {
                    DashCard {
                        DashCardHeader(title = stringResource(R.string.title_portfolio))
                        items.take(10).forEach { element ->
                            val item = runCatching { element.jsonObject }.getOrNull()
                                ?: return@forEach
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                ProgressRing(
                                    pct = item.int("progress") ?: 0,
                                    color = OrbitPrimary,
                                    size = 34.dp,
                                    thick = 3.dp
                                )
                                Spacer(Modifier.width(10.dp))
                                Text(
                                    text = item.str("title") ?: "",
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary,
                                    modifier = Modifier.weight(1f),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                                item.str("status")?.let { TinyPill(it, OrbitWarning) }
                            }
                        }
                    }
                }
            }
        }
    }
}

// Stat column
@Composable
private fun PortfolioStat(value: String, label: String, tone: androidx.compose.ui.graphics.Color) {
    val colors = OrbitTheme.colors
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.ExtraBold,
            color = tone
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted
        )
    }
}
