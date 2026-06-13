package com.orbit.mobile.feature.projects

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Icon
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
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.theme.projectStatusColor
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.components.EmptyState
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.ui.components.StaggeredAppear
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.domain.repository.ProjectsRepository
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.dashboard.daysUntil
import com.orbit.mobile.feature.dashboard.deadlineShortLabel
import com.orbit.mobile.feature.shell.InnerRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Filter keys
private val FILTERS = listOf("ALL", "ACTIVE", "ON HOLD", "COMPLETED")

// Staff state
data class StaffProjectsState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val filter: String = "ALL",
    val search: String = "",
    val projects: List<ProjectDto> = emptyList()
) {
    val real: List<ProjectDto> get() = projects.filterNot { it.isSystem }

    val filtered: List<ProjectDto>
        get() {
            var list = real
            if (filter != "ALL") {
                list = list.filter { it.status.uppercase() == filter }
            }
            val q = search.trim().lowercase()
            if (q.isNotEmpty()) {
                list = list.filter {
                    it.title.lowercase().contains(q) ||
                        (it.description ?: "").lowercase().contains(q)
                }
            }
            return list
        }
}

// Staff VM
@HiltViewModel
class StaffProjectsViewModel @Inject constructor(
    private val repo: ProjectsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(StaffProjectsState())
    val state: StateFlow<StaffProjectsState> = _state

    init {
        refresh()
    }

    fun setFilter(value: String) = _state.update { it.copy(filter = value) }
    fun setSearch(value: String) = _state.update { it.copy(search = value) }

    // Load projects
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.list()
                .onSuccess { list ->
                    _state.update { it.copy(loading = false, projects = list) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }
}

// Staff projects
@Composable
fun StaffProjectsScreen(
    onNavigate: (String) -> Unit,
    viewModel: StaffProjectsViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    val activeCount = state.real.count { it.status.uppercase() == "ACTIVE" }
    val doneCount = state.real.count { it.status.uppercase() == "COMPLETED" }
    val overdueCount = state.real.count {
        val d = daysUntil(it.dueDate)
        d != null && d < 0 && it.status.uppercase() != "COMPLETED"
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

        // Pills
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            TinyPill(stringResource(R.string.sp_active_pill, activeCount), OrbitSuccess)
            TinyPill(stringResource(R.string.sp_done_pill, doneCount), OrbitTheme.colors.textSecondary)
            if (overdueCount > 0) {
                TinyPill(stringResource(R.string.dash_overdue_pill, overdueCount), OrbitDanger)
            }
        }

        // Search
        OrbitTextField(
            value = state.search,
            onValueChange = viewModel::setSearch,
            placeholder = stringResource(R.string.field_search_hint),
            leading = {
                Icon(
                    imageVector = Icons.Outlined.Search,
                    contentDescription = null,
                    tint = colors.textMuted,
                    modifier = Modifier.size(16.dp)
                )
            }
        )

        // Filters
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            FILTERS.forEach { f ->
                val active = state.filter == f
                val label = when (f) {
                    "ACTIVE" -> stringResource(R.string.status_active)
                    "ON HOLD" -> stringResource(R.string.status_on_hold)
                    "COMPLETED" -> stringResource(R.string.status_completed)
                    else -> stringResource(R.string.sp_filter_all)
                }
                Surface(
                    onClick = { viewModel.setFilter(f) },
                    shape = RoundedCornerShape(10.dp),
                    color = if (active) Color(0xFF6366F1) else colors.surface,
                    border = androidx.compose.foundation.BorderStroke(
                        1.dp,
                        if (active) Color(0xFF6366F1) else colors.borderStrong
                    )
                ) {
                    Text(
                        text = label,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else colors.textMuted,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    )
                }
            }
        }

        when {
            state.loading -> LoadingHint()
            state.filtered.isEmpty() -> EmptyState(
                title = if (state.filter == "ALL") {
                    stringResource(R.string.dash_no_projects_assigned)
                } else {
                    stringResource(R.string.sp_no_filtered)
                },
                description = if (state.filter == "ALL") {
                    stringResource(R.string.sp_admin_assign)
                } else {
                    stringResource(R.string.sp_try_filter)
                }
            )
            else -> state.filtered.forEachIndexed { index, p ->
                val tone = projectStatusColor(p.status)
                val days = daysUntil(p.dueDate)
                // Card entrance
                StaggeredAppear(index = index) {
                Surface(
                    onClick = {
                        onNavigate("${InnerRoutes.WORKSPACE}?projectId=${p.projectId}")
                    },
                    shape = RoundedCornerShape(14.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        ProgressRing(pct = p.progress, color = tone, size = 44.dp, thick = 3.5.dp)
                        Spacer(Modifier.width(12.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = p.title,
                                style = MaterialTheme.typography.titleSmall,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = p.description?.ifBlank { null }
                                    ?: stringResource(R.string.pj_no_description),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis
                            )
                            Spacer(Modifier.height(5.dp))
                            Row(
                                horizontalArrangement = Arrangement.spacedBy(6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                TinyPill(p.status, tone)
                                if (days != null) {
                                    Text(
                                        text = deadlineShortLabel(days),
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
                }
            }
        }
    }
    }
}
