package com.orbit.mobile.feature.teams

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
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
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
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.EmptyState
import com.orbit.mobile.core.ui.components.OnReturnRefresh
import com.orbit.mobile.core.ui.components.OrbitBadge
import com.orbit.mobile.core.ui.components.OrbitPullRefresh
import com.orbit.mobile.core.ui.components.StaggeredAppear
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.domain.repository.TeamsRepository
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.shell.InnerRoutes
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Teams state
data class TeamsState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val teams: List<TeamDto> = emptyList()
)

// Teams VM
@HiltViewModel
class TeamsViewModel @Inject constructor(
    private val repo: TeamsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(TeamsState())
    val state: StateFlow<TeamsState> = _state

    init {
        refresh()
    }

    // Load teams
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.list()
                .onSuccess { list -> _state.update { it.copy(loading = false, teams = list) } }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }
}

// Teams list
@Composable
fun TeamsScreen(
    onNavigate: (String) -> Unit,
    viewModel: TeamsViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

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

        Text(
            text = stringResource(R.string.tm_page_sub),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )

        when {
            state.loading -> LoadingHint()
            state.teams.isEmpty() -> EmptyState(
                title = stringResource(R.string.tm_no_teams),
                description = stringResource(R.string.state_empty_desc)
            )
            else -> state.teams.forEachIndexed { index, team ->
                // Card entrance
                StaggeredAppear(index = index) {
                Surface(
                    onClick = { onNavigate("${InnerRoutes.TEAMS}/${team.teamId}") },
                    shape = RoundedCornerShape(16.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .background(
                                        OrbitPurple.copy(alpha = 0.12f),
                                        RoundedCornerShape(12.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = team.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.titleMedium,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = OrbitPurple
                                )
                            }
                            Spacer(Modifier.weight(1f))
                            OrbitBadge(
                                text = stringResource(
                                    R.string.tm_members_count,
                                    team.memberCount ?: 1
                                ),
                                color = OrbitPrimary
                            )
                        }
                        Spacer(Modifier.height(10.dp))
                        Text(
                            text = team.name,
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = team.description?.ifBlank { null }
                                ?: stringResource(R.string.tm_no_description),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween
                        ) {
                            Text(
                                text = stringResource(R.string.tm_view_details),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = OrbitPrimary
                            )
                            Text(
                                text = "→",
                                style = MaterialTheme.typography.labelMedium,
                                color = OrbitPrimary
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
