package com.orbit.mobile.feature.tasks

import androidx.compose.foundation.layout.Arrangement
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
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
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
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.CreateTaskRequest
import com.orbit.mobile.data.dto.MembershipDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.domain.repository.TasksRepository
import com.orbit.mobile.domain.repository.TeamsRepository
import com.orbit.mobile.feature.auth.AuthErrorBox
import com.orbit.mobile.feature.dashboard.HashAvatar
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Priorities list
private val PRIORITIES = listOf("Low", "Medium", "High")

// Create state
data class CreateTaskState(
    val taskName: String = "",
    val priority: String = "",
    val teamId: String = "",
    val details: String = "",
    val reportType: String = "",
    val visibility: String = "team",
    val assignees: Set<String> = emptySet(),
    val memberSearch: String = "",
    val teams: List<TeamDto> = emptyList(),
    val members: List<MembershipDto> = emptyList(),
    val loadingMembers: Boolean = false,
    val creatingTeam: Boolean = false,
    val newTeamName: String = "",
    val submitting: Boolean = false,
    val error: UiText? = null,
    val done: Boolean = false
) {
    val filteredMembers: List<MembershipDto>
        get() {
            val q = memberSearch.trim().lowercase()
            if (q.isEmpty()) return members
            return members.filter {
                (it.userFullName ?: "").lowercase().contains(q) ||
                    (it.userEmail ?: "").lowercase().contains(q)
            }
        }

    val canSubmit: Boolean
        get() = taskName.isNotBlank() && priority.isNotBlank() && teamId.isNotBlank() &&
            assignees.isNotEmpty() && !submitting
}

// Create VM
@HiltViewModel
class CreateTaskViewModel @Inject constructor(
    private val tasksRepo: TasksRepository,
    private val teamsRepo: TeamsRepository
) : ViewModel() {

    private val _state = MutableStateFlow(CreateTaskState())
    val state: StateFlow<CreateTaskState> = _state

    init {
        loadTeams()
    }

    fun setName(v: String) = _state.update { it.copy(taskName = v, error = null) }
    fun setPriority(v: String) = _state.update { it.copy(priority = v) }
    fun setDetails(v: String) = _state.update { it.copy(details = v) }
    fun setReportType(v: String) = _state.update { it.copy(reportType = v) }
    fun setVisibility(v: String) = _state.update { it.copy(visibility = v) }
    fun setMemberSearch(v: String) = _state.update { it.copy(memberSearch = v) }
    fun setCreatingTeam(v: Boolean) = _state.update { it.copy(creatingTeam = v) }
    fun setNewTeamName(v: String) = _state.update { it.copy(newTeamName = v) }

    // Load teams
    private fun loadTeams() {
        viewModelScope.launch {
            teamsRepo.list().onSuccess { list ->
                _state.update { it.copy(teams = list) }
            }
        }
    }

    // Pick team
    fun selectTeam(teamId: String) {
        _state.update {
            it.copy(teamId = teamId, assignees = emptySet(), loadingMembers = true)
        }
        viewModelScope.launch {
            teamsRepo.members(teamId)
                .onSuccess { members ->
                    _state.update { it.copy(members = members, loadingMembers = false) }
                }
                .onFailure {
                    _state.update { it.copy(members = emptyList(), loadingMembers = false) }
                }
        }
    }

    // Toggle assignee
    fun toggleAssignee(userId: String) {
        _state.update {
            it.copy(
                assignees = if (it.assignees.contains(userId)) it.assignees - userId
                else it.assignees + userId
            )
        }
    }

    // Select all
    fun toggleAll() {
        _state.update { s ->
            val filteredIds = s.filteredMembers.map { it.userId }
            val allSelected = filteredIds.isNotEmpty() && filteredIds.all { it in s.assignees }
            s.copy(
                assignees = if (allSelected) s.assignees - filteredIds.toSet()
                else s.assignees + filteredIds
            )
        }
    }

    // Inline team
    fun createTeam() {
        val name = _state.value.newTeamName.trim()
        if (name.isEmpty()) return
        viewModelScope.launch {
            teamsRepo.create(com.orbit.mobile.data.dto.CreateTeamRequest(name = name))
                .onSuccess { team ->
                    _state.update {
                        it.copy(
                            teams = it.teams + team,
                            creatingTeam = false,
                            newTeamName = ""
                        )
                    }
                    selectTeam(team.teamId)
                }
                .onFailure { e -> _state.update { it.copy(error = e.toUiText()) } }
        }
    }

    // Submit task
    fun submit() {
        val s = _state.value
        if (s.teamId.isBlank()) {
            _state.update { it.copy(error = UiText.Res(R.string.ct_select_team_error)) }
            return
        }
        if (s.assignees.isEmpty()) {
            _state.update { it.copy(error = UiText.Res(R.string.ct_select_assignee_error)) }
            return
        }
        _state.update { it.copy(submitting = true, error = null) }
        viewModelScope.launch {
            tasksRepo.create(
                CreateTaskRequest(
                    title = s.taskName,
                    description = s.details,
                    teamId = s.teamId,
                    assignedTo = s.assignees.first(),
                    assignees = s.assignees.toList(),
                    visibility = s.visibility,
                    priority = s.priority.lowercase(),
                    status = "todo",
                    reportType = s.reportType.ifBlank { null }
                )
            )
                .onSuccess { _state.update { it.copy(submitting = false, done = true) } }
                .onFailure { e ->
                    _state.update { it.copy(submitting = false, error = e.toUiText()) }
                }
        }
    }
}

// Create screen
@Composable
fun CreateTaskScreen(
    onDone: () -> Unit,
    viewModel: CreateTaskViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()

    androidx.compose.runtime.LaunchedEffect(state.done) {
        if (state.done) onDone()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        Text(
            text = stringResource(R.string.ct_title),
            style = MaterialTheme.typography.headlineSmall,
            color = colors.textPrimary
        )
        Text(
            text = stringResource(R.string.ct_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )

        state.error?.let { AuthErrorBox(message = it.asString()) }

        // Task name
        OrbitTextField(
            value = state.taskName,
            onValueChange = viewModel::setName,
            label = stringResource(R.string.ct_name_label),
            placeholder = stringResource(R.string.ct_name_placeholder),
            enabled = !state.submitting
        )

        // Priority
        Column {
            Text(
                text = stringResource(R.string.ct_priority_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                PRIORITIES.forEach { p ->
                    val active = state.priority == p
                    Surface(
                        onClick = { viewModel.setPriority(p) },
                        shape = RoundedCornerShape(20.dp),
                        color = if (active) OrbitPrimary else colors.surface2,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) OrbitPrimary else colors.borderStrong
                        )
                    ) {
                        Text(
                            text = when (p) {
                                "Low" -> stringResource(R.string.ct_priority_low)
                                "High" -> stringResource(R.string.ct_priority_high)
                                else -> stringResource(R.string.ct_priority_medium)
                            },
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 7.dp)
                        )
                    }
                }
            }
        }

        // Team picker
        Column {
            Text(
                text = stringResource(R.string.ct_team_label).uppercase(),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textMuted
            )
            Spacer(Modifier.height(6.dp))
            if (!state.creatingTeam) {
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    state.teams.forEach { team ->
                        val active = state.teamId == team.teamId
                        Surface(
                            onClick = { viewModel.selectTeam(team.teamId) },
                            shape = RoundedCornerShape(10.dp),
                            color = if (active) OrbitPrimary.copy(alpha = 0.1f) else colors.surface2,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (active) OrbitPrimary else colors.borderStrong
                            )
                        ) {
                            Text(
                                text = team.name,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = if (active) OrbitPrimary else colors.textPrimary,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 12.dp, vertical = 11.dp)
                            )
                        }
                    }
                    OrbitButton(
                        text = stringResource(R.string.ct_new_team),
                        onClick = { viewModel.setCreatingTeam(true) },
                        variant = OrbitButtonVariant.Secondary,
                        leadingIcon = Icons.Outlined.Add
                    )
                }
            } else {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    OrbitTextField(
                        value = state.newTeamName,
                        onValueChange = viewModel::setNewTeamName,
                        placeholder = stringResource(R.string.tm_name_placeholder),
                        modifier = Modifier.weight(1f)
                    )
                    OrbitButton(
                        text = stringResource(R.string.action_save),
                        onClick = viewModel::createTeam,
                        enabled = state.newTeamName.isNotBlank()
                    )
                    OrbitButton(
                        text = stringResource(R.string.action_cancel),
                        onClick = { viewModel.setCreatingTeam(false) },
                        variant = OrbitButtonVariant.Ghost
                    )
                }
            }
        }

        // Assignees
        if (state.teamId.isNotBlank()) {
            Column {
                Text(
                    text = stringResource(R.string.ct_assignees_label).uppercase(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textMuted
                )
                Spacer(Modifier.height(6.dp))
                if (state.loadingMembers) {
                    Text(
                        text = stringResource(R.string.ct_loading_members),
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textMuted
                    )
                } else {
                    OrbitTextField(
                        value = state.memberSearch,
                        onValueChange = viewModel::setMemberSearch,
                        placeholder = stringResource(R.string.ct_search_members),
                        leading = {
                            Icon(
                                imageVector = Icons.Outlined.Search,
                                contentDescription = null,
                                tint = colors.textMuted,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    )
                    Spacer(Modifier.height(8.dp))
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .heightIn(max = 260.dp)
                            .verticalScroll(rememberScrollState())
                    ) {
                        // Select all
                        val filteredIds = state.filteredMembers.map { it.userId }
                        val allSelected = filteredIds.isNotEmpty() &&
                            filteredIds.all { it in state.assignees }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Checkbox(
                                checked = allSelected,
                                onCheckedChange = { viewModel.toggleAll() },
                                colors = CheckboxDefaults.colors(checkedColor = OrbitPrimary)
                            )
                            Text(
                                text = stringResource(R.string.ct_select_all),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f)
                            )
                            if (state.assignees.isNotEmpty()) {
                                Text(
                                    text = stringResource(
                                        R.string.pj_selected_count,
                                        state.assignees.size
                                    ),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OrbitPrimary
                                )
                            }
                        }
                        state.filteredMembers.forEach { m ->
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Checkbox(
                                    checked = state.assignees.contains(m.userId),
                                    onCheckedChange = { viewModel.toggleAssignee(m.userId) },
                                    colors = CheckboxDefaults.colors(checkedColor = OrbitPrimary)
                                )
                                HashAvatar(
                                    name = m.userFullName ?: m.userEmail ?: "U",
                                    size = 26.dp
                                )
                                Spacer(Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = m.userFullName ?: m.userEmail ?: m.userId,
                                        style = MaterialTheme.typography.labelMedium,
                                        color = colors.textPrimary,
                                        maxLines = 1,
                                        overflow = TextOverflow.Ellipsis
                                    )
                                    Text(
                                        text = m.role,
                                        style = MaterialTheme.typography.labelSmall,
                                        color = colors.textMuted
                                    )
                                }
                            }
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))

                // Visibility
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.ct_visibility_label),
                        style = MaterialTheme.typography.labelMedium,
                        color = colors.textSecondary
                    )
                    listOf("team", "private").forEach { v ->
                        val active = state.visibility == v
                        Surface(
                            onClick = { viewModel.setVisibility(v) },
                            shape = RoundedCornerShape(20.dp),
                            color = if (active) OrbitSuccess.copy(alpha = 0.12f)
                            else colors.surface2,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                if (active) OrbitSuccess else colors.borderStrong
                            )
                        ) {
                            Text(
                                text = if (v == "team") {
                                    stringResource(R.string.ct_visibility_team)
                                } else {
                                    stringResource(R.string.ct_visibility_private)
                                },
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = if (active) OrbitSuccess else colors.textSecondary,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                            )
                        }
                    }
                }
            }
        }

        // Report type
        OrbitTextField(
            value = state.reportType,
            onValueChange = viewModel::setReportType,
            label = stringResource(R.string.ct_report_type_label),
            placeholder = stringResource(R.string.ct_report_type_placeholder),
            enabled = !state.submitting
        )

        // Details
        OrbitTextField(
            value = state.details,
            onValueChange = viewModel::setDetails,
            label = stringResource(R.string.ct_details_label),
            placeholder = stringResource(R.string.ct_details_placeholder),
            singleLine = false,
            minLines = 3,
            enabled = !state.submitting
        )

        // Actions
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            OrbitButton(
                text = stringResource(R.string.action_cancel),
                onClick = onDone,
                variant = OrbitButtonVariant.Ghost,
                modifier = Modifier.weight(1f)
            )
            OrbitButton(
                text = stringResource(R.string.ct_submit),
                onClick = viewModel::submit,
                enabled = state.canSubmit,
                loading = state.submitting,
                modifier = Modifier.weight(1f)
            )
        }
        Spacer(Modifier.height(8.dp))
    }
}
