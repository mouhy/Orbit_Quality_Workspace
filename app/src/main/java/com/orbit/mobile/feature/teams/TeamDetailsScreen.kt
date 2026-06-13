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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material3.HorizontalDivider
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
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitAvatar
import com.orbit.mobile.core.ui.components.OrbitBadge
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.MembershipDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.shell.InnerRoutes
import com.orbit.mobile.domain.repository.TeamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Details state
data class TeamDetailsState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val team: TeamDto? = null,
    val members: List<MembershipDto> = emptyList()
)

// Details VM
@HiltViewModel
class TeamDetailsViewModel @Inject constructor(
    private val repo: TeamsRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    private val teamId: String = savedStateHandle.get<String>("teamId") ?: ""

    private val _state = MutableStateFlow(TeamDetailsState())
    val state: StateFlow<TeamDetailsState> = _state

    init {
        refresh()
    }

    // Load data
    fun refresh() {
        if (teamId.isBlank()) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val teamDeferred = async { repo.get(teamId) }
            val membersDeferred = async { repo.members(teamId) }

            teamDeferred.await()
                .onSuccess { team -> _state.update { it.copy(team = team) } }
                .onFailure { e -> _state.update { it.copy(error = e.toUiText()) } }
            membersDeferred.await()
                .onSuccess { members -> _state.update { it.copy(members = members) } }
            _state.update { it.copy(loading = false) }
        }
    }
}

// Team details
@Composable
fun TeamDetailsScreen(
    onBack: () -> Unit,
    onNavigate: (String) -> Unit,
    viewModel: TeamDetailsViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Back link
        Surface(onClick = onBack, color = Color.Transparent) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.ArrowBack,
                    contentDescription = stringResource(R.string.action_back),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.tm_back_to_teams),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.SemiBold,
                    color = colors.textSecondary
                )
            }
        }

        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

        when {
            state.loading -> LoadingHint()
            state.team != null -> {
                val team = state.team!!
                Surface(
                    shape = RoundedCornerShape(20.dp),
                    color = colors.surface,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
                ) {
                    Column(modifier = Modifier.padding(20.dp)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(64.dp)
                                    .background(
                                        Brush.linearGradient(
                                            listOf(OrbitPurple, Color(0xFF6366F1))
                                        ),
                                        RoundedCornerShape(16.dp)
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = team.name.take(2).uppercase(),
                                    style = MaterialTheme.typography.headlineSmall,
                                    fontWeight = FontWeight.ExtraBold,
                                    color = Color.White
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column {
                                Text(
                                    text = team.name,
                                    style = MaterialTheme.typography.headlineSmall,
                                    color = colors.textPrimary
                                )
                                Spacer(Modifier.height(2.dp))
                                Text(
                                    text = team.description?.ifBlank { null }
                                        ?: stringResource(R.string.tm_details_no_description),
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textSecondary
                                )
                            }
                        }
                        Spacer(Modifier.height(16.dp))
                        HorizontalDivider(color = colors.border)
                        Spacer(Modifier.height(16.dp))

                        Text(
                            text = stringResource(R.string.tm_members_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.height(10.dp))

                        if (state.members.isEmpty()) {
                            Text(
                                text = stringResource(R.string.tm_no_members),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textMuted
                            )
                        } else {
                            state.members.forEach { m ->
                                Surface(
                                    onClick = {
                                        onNavigate("${InnerRoutes.PORTFOLIO}/${m.userId}")
                                    },
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .padding(bottom = 8.dp),
                                    shape = RoundedCornerShape(12.dp),
                                    color = colors.surface2,
                                    border = androidx.compose.foundation.BorderStroke(
                                        1.dp,
                                        colors.border
                                    )
                                ) {
                                    Row(
                                        modifier = Modifier.padding(12.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OrbitAvatar(
                                            name = m.userFullName ?: m.userEmail ?: "U",
                                            size = 44.dp
                                        )
                                        Spacer(Modifier.width(10.dp))
                                        Column(modifier = Modifier.weight(1f)) {
                                            Text(
                                                text = m.userFullName
                                                    ?: stringResource(R.string.tm_unknown_user),
                                                style = MaterialTheme.typography.titleSmall,
                                                color = colors.textPrimary,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Text(
                                                text = m.userEmail
                                                    ?: stringResource(R.string.tm_no_email),
                                                style = MaterialTheme.typography.labelSmall,
                                                color = colors.textMuted,
                                                maxLines = 1,
                                                overflow = TextOverflow.Ellipsis
                                            )
                                            Spacer(Modifier.height(3.dp))
                                            OrbitBadge(text = m.role, color = OrbitPrimary)
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
