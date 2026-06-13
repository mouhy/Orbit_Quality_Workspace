package com.orbit.mobile.feature.teams

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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.ExperimentalMaterial3Api
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.theme.roleColor
import com.orbit.mobile.core.ui.charts.BarEntry
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.charts.VerticalBarChart
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.UserAnalyticsDto
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.domain.repository.DashboardRepository
import com.orbit.mobile.domain.repository.MemberAnalyticsRepository
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.min
import kotlin.math.roundToInt

// Analytics roles
private val ANALYTICS_ROLES = setOf("admin", "sub_admin", "staff")

// Scored member
data class ScoredMember(
    val user: UserDto,
    val analytics: UserAnalyticsDto?,
    val projectCount: Int
) {
    // Member score
    val score: Int
        get() {
            val a = analytics ?: return min(projectCount * 4, 30)
            val taskScore = a.tasksDone * 5
            val aiScore = (a.avgAccuracy * 10).roundToInt()
            val progressBonus = a.tasksInProgress * 2
            return min(100, taskScore + aiScore + progressBonus + projectCount * 4)
        }

    val completionPct: Int
        get() {
            val a = analytics ?: return 0
            return (a.tasksDone * 100.0 / maxOf(a.tasksTotal, 1)).roundToInt()
        }

    val aiPct: Int get() = ((analytics?.avgAccuracy ?: 0.0) * 100).roundToInt()
}

// Team page state
data class TeamPageState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val members: List<UserDto> = emptyList(),
    val projects: List<ProjectDto> = emptyList(),
    val analytics: Map<String, UserAnalyticsDto> = emptyMap(),
    val roleFilter: String = "all"
) {
    val scored: List<ScoredMember>
        get() = members.filter { it.role in ANALYTICS_ROLES }.map { user ->
            val pc = projects.count { p ->
                p.subAdmins.any { it.id == user.id } || p.staff.any { it.id == user.id }
            }
            ScoredMember(user, analytics[user.id], pc)
        }.sortedByDescending { it.score }

    val filtered: List<ScoredMember>
        get() = if (roleFilter == "all") scored else scored.filter { it.user.role == roleFilter }
}

// Team page VM
@HiltViewModel
class TeamPageViewModel @Inject constructor(
    private val dashRepo: DashboardRepository,
    private val analyticsRepo: MemberAnalyticsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamPageState())
    val state: StateFlow<TeamPageState> = _state

    init {
        refresh()
    }

    fun setRoleFilter(value: String) = _state.update { it.copy(roleFilter = value) }

    // Load all
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val usersDeferred = async { dashRepo.users() }
            val projectsDeferred = async { dashRepo.projects() }

            val usersResult = usersDeferred.await()
            val projectsResult = projectsDeferred.await()

            var users: List<UserDto> = emptyList()
            when (usersResult) {
                is ApiResult.Success -> users = usersResult.data
                is ApiResult.Failure ->
                    _state.update { it.copy(error = usersResult.error.toUiText()) }
            }
            val projects = (projectsResult as? ApiResult.Success)?.data ?: emptyList()

            // Per-member analytics
            val targets = users.filter { it.role in ANALYTICS_ROLES }
            val analyticsMap = targets.map { user ->
                async {
                    user.id to (analyticsRepo.userAnalytics(user.id) as? ApiResult.Success)?.data
                }
            }.awaitAll().mapNotNull { (id, a) -> a?.let { id to it } }.toMap()

            _state.update {
                it.copy(
                    loading = false,
                    members = users,
                    projects = projects.filterNot { p -> p.isSystem },
                    analytics = analyticsMap
                )
            }
        }
    }
}

// Team page
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TeamPageScreen(
    viewModel: TeamPageViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var selected by remember { mutableStateOf<ScoredMember?>(null) }

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

        // Awards
        val top = state.scored.firstOrNull()
        val mostHelpful = state.scored.maxByOrNull { it.analytics?.tasksDone ?: 0 }
        val bestAi = state.scored.maxByOrNull { it.analytics?.avgAccuracy ?: 0.0 }
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.tp_leaderboard_title),
                subtitle = stringResource(R.string.tp_leaderboard_sub)
            )
            if (state.loading) {
                LoadingHint()
            } else {
                AwardRow(
                    medal = "🏆",
                    label = stringResource(R.string.tp_top_performer),
                    name = top?.user?.name,
                    detail = top?.let {
                        stringResource(
                            R.string.tp_top_reason,
                            it.score,
                            it.analytics?.tasksDone ?: 0
                        )
                    },
                    tone = OrbitWarning
                )
                AwardRow(
                    medal = "✅",
                    label = stringResource(R.string.tp_most_helpful),
                    name = mostHelpful?.user?.name,
                    detail = mostHelpful?.let {
                        stringResource(
                            R.string.tp_helpful_reason,
                            it.analytics?.tasksDone ?: 0,
                            it.projectCount
                        )
                    },
                    tone = OrbitSuccess
                )
                AwardRow(
                    medal = "🤖",
                    label = stringResource(R.string.tp_best_ai),
                    name = bestAi?.user?.name,
                    detail = bestAi?.let {
                        stringResource(R.string.tp_ai_reason, it.aiPct)
                    },
                    tone = OrbitPurple
                )
            }
        }

        // Role averages
        DashCard {
            DashCardHeader(
                title = stringResource(R.string.tp_role_chart_title),
                subtitle = stringResource(R.string.tp_role_chart_sub)
            )
            if (!state.loading) {
                val groups = listOf("admin", "sub_admin", "staff")
                val bars = groups.map { role ->
                    val list = state.scored.filter { it.user.role == role }
                    val avg = if (list.isEmpty()) 0
                    else (list.sumOf { it.analytics?.tasksDone ?: 0 }.toDouble() / list.size)
                        .roundToInt()
                    BarEntry(role.replace("_", " "), avg, roleColor(role))
                }
                VerticalBarChart(bars = bars, height = 110.dp)
            }
        }

        // Role filter
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            listOf("all", "admin", "sub_admin", "staff").forEach { role ->
                val active = state.roleFilter == role
                Surface(
                    onClick = { viewModel.setRoleFilter(role) },
                    shape = RoundedCornerShape(20.dp),
                    color = if (active) OrbitPrimary else colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) OrbitPrimary else colors.borderStrong
                    )
                ) {
                    Text(
                        text = if (role == "all") stringResource(R.string.sp_filter_all)
                        else role.replace("_", " "),
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        }

        // Directory
        when {
            state.loading -> LoadingHint()
            else -> state.filtered.forEach { member ->
                Surface(
                    onClick = { selected = member },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HashAvatar(name = member.user.name, size = 44.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = member.user.name,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = member.user.email,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(4.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                                TinyPill(
                                    member.user.role.replace("_", " "),
                                    roleColor(member.user.role)
                                )
                                TinyPill(
                                    stringResource(R.string.tp_tasks_pill, member.analytics?.tasksDone ?: 0),
                                    OrbitSuccess
                                )
                                TinyPill(
                                    stringResource(R.string.tp_ai_pill, member.aiPct),
                                    OrbitPurple
                                )
                            }
                        }
                        Spacer(Modifier.width(8.dp))
                        Column(horizontalAlignment = Alignment.CenterHorizontally) {
                            Text(
                                text = member.score.toString(),
                                style = MaterialTheme.typography.titleMedium,
                                fontWeight = FontWeight.ExtraBold,
                                color = OrbitPrimary
                            )
                            Text(
                                text = stringResource(R.string.tp_score_label).uppercase(),
                                style = OrbitTextStyles.techLabel,
                                color = colors.textMuted
                            )
                        }
                    }
                }
            }
        }
    }
    }

    // Member detail
    selected?.let { member ->
        ModalBottomSheet(
            onDismissRequest = { selected = null },
            containerColor = colors.popupBg,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 20.dp)
                    .padding(bottom = 28.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    HashAvatar(name = member.user.name, size = 52.dp)
                    Spacer(Modifier.width(12.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.user.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Text(
                            text = member.user.email,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        TinyPill(
                            member.user.role.replace("_", " "),
                            roleColor(member.user.role)
                        )
                    }
                    ProgressRing(
                        pct = member.completionPct,
                        color = OrbitSuccess,
                        size = 56.dp,
                        thick = 5.dp
                    )
                }
                Spacer(Modifier.height(16.dp))

                val stats = listOf(
                    Triple(
                        stringResource(R.string.tp_stat_done),
                        member.analytics?.tasksDone,
                        OrbitSuccess
                    ),
                    Triple(
                        stringResource(R.string.tp_stat_in_progress),
                        member.analytics?.tasksInProgress,
                        Color(0xFF6366F1)
                    ),
                    Triple(
                        stringResource(R.string.tp_stat_review),
                        member.analytics?.tasksInReview,
                        OrbitWarning
                    ),
                    Triple(
                        stringResource(R.string.tp_stat_total),
                        member.analytics?.tasksTotal,
                        colors.textSecondary
                    )
                )
                stats.forEach { (label, value, tone) ->
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(8.dp)
                                .background(tone, RoundedCornerShape(2.dp))
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = label,
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = value?.toString() ?: "—",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = tone
                        )
                    }
                }
                Spacer(Modifier.height(10.dp))

                // Bars
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${member.completionPct}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrbitSuccess
                        )
                        Text(
                            text = stringResource(R.string.dash_completion),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "${member.aiPct}%",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrbitPurple
                        )
                        Text(
                            text = stringResource(R.string.tp_ai_quality),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = member.projectCount.toString(),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrbitPrimary
                        )
                        Text(
                            text = stringResource(R.string.dash_kpi_projects),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }
            }
        }
    }
}

// Award row
@Composable
private fun AwardRow(
    medal: String,
    label: String,
    name: String?,
    detail: String?,
    tone: Color
) {
    val colors = OrbitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .background(tone.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
            .border(1.dp, tone.copy(alpha = 0.14f), RoundedCornerShape(10.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(text = medal, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.width(10.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = label.uppercase(),
                style = OrbitTextStyles.techLabel,
                color = tone
            )
            Text(
                text = name ?: stringResource(R.string.tp_no_data),
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            if (detail != null) {
                Text(
                    text = detail,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
    }
}
