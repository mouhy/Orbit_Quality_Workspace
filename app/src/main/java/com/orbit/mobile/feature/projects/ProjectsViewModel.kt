package com.orbit.mobile.feature.projects

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.CreateProjectRequest
import com.orbit.mobile.data.dto.CreateTeamRequest
import com.orbit.mobile.data.dto.MembershipDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.data.dto.UpdateProjectRequest
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.domain.repository.DashboardRepository
import com.orbit.mobile.domain.repository.ProjectsRepository
import com.orbit.mobile.domain.repository.TeamsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Page tabs
enum class ProjectsTab { OVERVIEW, PROJECTS, TEAMS }

// Page state
data class ProjectsState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val toast: UiText? = null,
    val tab: ProjectsTab = ProjectsTab.OVERVIEW,
    val search: String = "",
    val projects: List<ProjectDto> = emptyList(),
    val teams: List<TeamDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val busy: Boolean = false
) {
    val customProjects: List<ProjectDto> get() = projects.filterNot { it.isSystem }

    val filteredProjects: List<ProjectDto>
        get() {
            val q = search.trim().lowercase()
            if (q.isEmpty()) return customProjects
            return customProjects.filter {
                it.title.lowercase().contains(q) ||
                    (it.description ?: "").lowercase().contains(q)
            }
        }
}

// Projects VM
@HiltViewModel
class ProjectsViewModel @Inject constructor(
    private val projectsRepo: ProjectsRepository,
    private val teamsRepo: TeamsRepository,
    private val dashRepo: DashboardRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(ProjectsState())
    val state: StateFlow<ProjectsState> = _state

    // Current role
    val currentRole: String? get() = session.role

    init {
        refresh()
    }

    fun setTab(tab: ProjectsTab) = _state.update { it.copy(tab = tab) }

    fun setSearch(value: String) = _state.update { it.copy(search = value) }

    fun clearToast() = _state.update { it.copy(toast = null) }

    // Load all
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val projectsDeferred = async { projectsRepo.list() }
            val teamsDeferred = async { teamsRepo.list() }
            val usersDeferred = async { dashRepo.users() }

            val projectsResult = projectsDeferred.await()
            val teamsResult = teamsDeferred.await()
            val usersResult = usersDeferred.await()

            _state.update { current ->
                var next = current.copy(loading = false)
                when (projectsResult) {
                    is ApiResult.Success -> next = next.copy(projects = projectsResult.data)
                    is ApiResult.Failure ->
                        next = next.copy(error = projectsResult.error.toUiText())
                }
                (teamsResult as? ApiResult.Success)?.let { next = next.copy(teams = it.data) }
                (usersResult as? ApiResult.Success)?.let { next = next.copy(users = it.data) }
                next
            }
        }
    }

    // Create project
    fun createProject(body: CreateProjectRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            projectsRepo.create(body)
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    // Update project
    fun updateProject(id: String, body: UpdateProjectRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            projectsRepo.update(id, body)
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    // Quick status
    fun changeStatus(id: String, status: String) {
        viewModelScope.launch {
            projectsRepo.update(id, UpdateProjectRequest(status = status))
                .onSuccess { refresh() }
                .onFailure { e -> _state.update { it.copy(toast = e.toUiText()) } }
        }
    }

    // Delete project
    fun deleteProject(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            projectsRepo.delete(id)
            _state.update { it.copy(busy = false) }
            refresh()
            onDone()
        }
    }

    // Create team
    fun createTeam(body: CreateTeamRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            teamsRepo.create(body)
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    // Update team
    fun updateTeam(id: String, name: String, desc: String, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            teamsRepo.update(id, name, desc)
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    // Delete team
    fun deleteTeam(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            teamsRepo.delete(id)
            _state.update { it.copy(busy = false) }
            refresh()
            onDone()
        }
    }

    // Load members
    suspend fun loadMembers(teamId: String): List<MembershipDto> =
        (teamsRepo.members(teamId) as? ApiResult.Success)?.data ?: emptyList()

    // Sync members
    fun syncMembers(
        teamId: String,
        current: List<MembershipDto>,
        targetIds: Map<String, String>,
        onDone: () -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val currentIds = current.map { it.userId }.toSet()
            // Remove gone
            current.filterNot { targetIds.containsKey(it.userId) }.forEach {
                teamsRepo.removeMember(teamId, it.userId)
            }
            // Add new
            targetIds.filterKeys { it !in currentIds }.forEach { (userId, role) ->
                teamsRepo.addMember(teamId, userId, role)
            }
            _state.update { it.copy(busy = false) }
            refresh()
            onDone()
        }
    }
}
