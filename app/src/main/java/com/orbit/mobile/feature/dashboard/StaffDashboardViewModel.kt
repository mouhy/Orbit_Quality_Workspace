package com.orbit.mobile.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.UserDashboardDto
import com.orbit.mobile.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Staff state
data class StaffDashboardState(
    val loading: Boolean = true,
    val seeding: Boolean = false,
    val error: UiText? = null,
    val projects: List<ProjectDto> = emptyList(),
    val analytics: UserDashboardDto? = null
) {
    val myProjects: List<ProjectDto> get() = projects.filterNot { it.isSystem }
}

// Staff VM
@HiltViewModel
class StaffDashboardViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(StaffDashboardState())
    val state: StateFlow<StaffDashboardState> = _state

    val userName: String get() = session.session.value.fullName ?: ""

    init {
        fetchAll()
    }

    // Load all
    fun fetchAll(autoSeed: Boolean = false) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            if (autoSeed) {
                _state.update { it.copy(seeding = true) }
                repo.seedDemo()
                _state.update { it.copy(seeding = false) }
            }
            val userId = session.userId ?: ""
            val projectsDeferred = async { repo.projects() }
            val analyticsDeferred = async { repo.userDashboard(userId) }

            val projectsResult = projectsDeferred.await()
            val analyticsResult = analyticsDeferred.await()

            var projects: List<ProjectDto> = _state.value.projects
            when (projectsResult) {
                is ApiResult.Success -> projects = projectsResult.data
                is ApiResult.Failure -> _state.update {
                    it.copy(error = projectsResult.error.toUiText())
                }
            }
            val analytics = (analyticsResult as? ApiResult.Success)?.data
                ?: _state.value.analytics

            _state.update {
                it.copy(loading = false, projects = projects, analytics = analytics)
            }

            // Auto seed
            val real = projects.filterNot { it.isSystem }
            if (!autoSeed && real.isEmpty() && projectsResult is ApiResult.Success) {
                fetchAll(autoSeed = true)
            }
        }
    }
}
