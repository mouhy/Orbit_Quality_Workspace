package com.orbit.mobile.feature.dashboard

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.parseInstant
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.AuditLogDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.ceil
import kotlin.math.min
import kotlin.math.roundToInt

// Heat weeks
const val HEAT_WEEKS = 26

// Admin state
data class AdminDashboardState(
    val loading: Boolean = true,
    val seeding: Boolean = false,
    val error: UiText? = null,
    val projects: List<ProjectDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val auditLogs: List<AuditLogDto> = emptyList()
) {
    val customProjects: List<ProjectDto> get() = projects.filterNot { it.isSystem }
    val systemChannels: List<ProjectDto> get() = projects.filter { it.isSystem }

    val active: List<ProjectDto>
        get() = customProjects.filter { it.status.uppercase() == "ACTIVE" }
    val done: List<ProjectDto>
        get() = customProjects.filter { it.status.uppercase() == "COMPLETED" }
    val onHold: List<ProjectDto>
        get() = customProjects.filter {
            it.status.uppercase().replace("-", "_").replace(" ", "_") == "ON_HOLD"
        }

    val online: List<UserDto>
        get() = users.filter { u ->
            val seen = parseInstant(u.lastSeen) ?: return@filter false
            System.currentTimeMillis() - seen.toEpochMilli() < 5 * 60 * 1000
        }

    val avgProgress: Int
        get() = if (customProjects.isEmpty()) 0
        else (customProjects.sumOf { it.progress }.toDouble() / customProjects.size).roundToInt()

    val wsHealth: Int
        get() = if (customProjects.isEmpty()) 0 else (
            (done.size.toDouble() / customProjects.size) * 40 +
                (avgProgress / 100.0) * 40 +
                min(online.size.toDouble() / maxOf(users.size, 1), 1.0) * 20
            ).roundToInt()

    val overdue: Int
        get() = customProjects.count { p ->
            val d = daysUntil(p.dueDate)
            d != null && d < 0 && p.status.uppercase() != "COMPLETED"
        }

    val completionRate: Int
        get() = if (customProjects.isEmpty()) 0
        else (done.size * 100.0 / customProjects.size).roundToInt()

    val aiPassRate: Int
        get() = min(95, (completionRate * 0.55 + wsHealth * 0.45).roundToInt())

    val productivityScore: Int
        get() = min(
            100,
            (
                (active.size.toDouble() / maxOf(customProjects.size, 1)) * 30 +
                    (completionRate / 100.0) * 40 +
                    (online.size.toDouble() / maxOf(users.size, 1)) * 30
                ).roundToInt()
        )

    // Heatmap grid
    val heatmap: List<List<Int>>
        get() {
            val grid = List(7) { IntArray(HEAT_WEEKS) }
            auditLogs.forEach { log ->
                val instant = parseInstant(log.time) ?: return@forEach
                val days = ((System.currentTimeMillis() - instant.toEpochMilli()) / 86_400_000L).toInt()
                if (days >= HEAT_WEEKS * 7 || days < 0) return@forEach
                val week = HEAT_WEEKS - 1 - days / 7
                val day = java.time.Instant.ofEpochMilli(instant.toEpochMilli())
                    .atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value % 7
                if (week in 0 until HEAT_WEEKS) grid[day][week]++
            }
            return grid.map { it.toList() }
        }
}

// Days until
fun daysUntil(iso: String?): Int? {
    val instant = parseInstant(iso) ?: return null
    val diff = instant.toEpochMilli() - System.currentTimeMillis()
    return ceil(diff / 86_400_000.0).toInt()
}

// Admin VM
@HiltViewModel
class AdminDashboardViewModel @Inject constructor(
    private val repo: DashboardRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(AdminDashboardState())
    val state: StateFlow<AdminDashboardState> = _state

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
            val projectsDeferred = async { repo.projects() }
            val usersDeferred = async { repo.users() }
            val logsDeferred = async { repo.auditLogs(50) }

            val projectsResult = projectsDeferred.await()
            val usersResult = usersDeferred.await()
            val logsResult = logsDeferred.await()

            var projects = _state.value.projects
            var users = _state.value.users
            when (projectsResult) {
                is ApiResult.Success -> projects = projectsResult.data
                is ApiResult.Failure -> _state.update {
                    it.copy(error = projectsResult.error.toUiText())
                }
            }
            (usersResult as? ApiResult.Success)?.let { users = it.data }
            val logs = (logsResult as? ApiResult.Success)?.data ?: _state.value.auditLogs

            _state.update {
                it.copy(loading = false, projects = projects, users = users, auditLogs = logs)
            }

            // Auto seed
            val real = projects.filterNot { it.isSystem }
            if (!autoSeed && real.isEmpty() && users.size <= 2 &&
                projectsResult is ApiResult.Success
            ) {
                fetchAll(autoSeed = true)
            }
        }
    }
}
