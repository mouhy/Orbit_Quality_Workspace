package com.orbit.mobile.feature.itportal

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.api.ItApi
import com.orbit.mobile.data.api.SystemApi
import com.orbit.mobile.data.dto.AdminPasswordResetRequest
import com.orbit.mobile.data.dto.AuditLogDto
import com.orbit.mobile.data.dto.RegisterUserRequest
import com.orbit.mobile.data.dto.SessionLogDto
import com.orbit.mobile.data.dto.SettingsUpdateRequest
import com.orbit.mobile.data.dto.SystemHealthDto
import com.orbit.mobile.data.dto.UpdateUserRequest
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import javax.inject.Inject

/**
 * Shared state for the IT console, Configuration page,
 * Session Logs and Credentials screens. One ViewModel keeps
 * users/sessions/health in sync after every admin action.
 */
data class ItState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val toast: UiText? = null,
    val users: List<UserDto> = emptyList(),
    val sessions: List<SessionLogDto> = emptyList(),
    val mySessions: List<SessionLogDto> = emptyList(),
    val auditLogs: List<AuditLogDto> = emptyList(),
    val health: SystemHealthDto? = null,
    val credentialsText: String? = null,
    val search: String = "",
    val busy: Boolean = false
) {
    // Users filtered by the search box (name or email)
    val filteredUsers: List<UserDto>
        get() {
            val q = search.trim().lowercase()
            if (q.isEmpty()) return users
            return users.filter {
                it.name.lowercase().contains(q) || it.email.lowercase().contains(q)
            }
        }
}

@HiltViewModel
class ItViewModel @Inject constructor(
    private val api: ItApi,
    private val systemApi: SystemApi,
    private val dashRepo: DashboardRepository
) : ViewModel() {

    private val _state = MutableStateFlow(ItState())
    val state: StateFlow<ItState> = _state

    init {
        refresh()
    }

    fun setSearch(value: String) = _state.update { it.copy(search = value) }

    fun clearToast() = _state.update { it.copy(toast = null) }

    /** Loads every IT data source in parallel; failures degrade gracefully per-section. */
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val usersDeferred = async { dashRepo.users() }
            val sessionsDeferred = async { safeApiCall { api.sessions(100) } }
            val mySessionsDeferred = async { safeApiCall { api.mySessions() } }
            val auditDeferred = async { safeApiCall { systemApi.auditLogs(50) } }
            val healthDeferred = async { safeApiCall { api.health() } }
            val credsDeferred = async { safeApiCall { api.credentials().string() } }

            val usersResult = usersDeferred.await()

            _state.update { current ->
                var next = current.copy(loading = false)
                when (usersResult) {
                    is ApiResult.Success -> next = next.copy(users = usersResult.data)
                    is ApiResult.Failure ->
                        next = next.copy(error = usersResult.error.toUiText())
                }
                (sessionsDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(sessions = it.data)
                }
                (mySessionsDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(mySessions = it.data)
                }
                (auditDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(auditLogs = it.data)
                }
                (healthDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(health = it.data)
                }
                (credsDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(credentialsText = it.data)
                }
                next
            }
        }
    }

    /** Creates an account via auth/register, then reloads the directory. */
    fun createUser(body: RegisterUserRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.register(body) }
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Applies a partial PATCH to one user account. */
    fun updateUser(id: String, body: UpdateUserRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.updateUser(id, body) }
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Admin reset of another user's password. */
    fun resetPassword(userId: String, newPassword: String, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall {
                api.resetPassword(AdminPasswordResetRequest(userId, newPassword))
            }
                .onSuccess { onDone(true, null) }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Suspends the account, ending its access immediately. */
    fun forceLogout(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.forceLogout(userId) }
                .onFailure { e -> _state.update { it.copy(toast = e.toUiText()) } }
            _state.update { it.copy(busy = false) }
            refresh()
        }
    }

    /** Emergency switch: maintenance ON, everyone logged out. */
    fun logoutAll() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.logoutAll() }
                .onFailure { e -> _state.update { it.copy(toast = e.toUiText()) } }
            _state.update { it.copy(busy = false) }
            refresh()
        }
    }

    /** Permanently removes an account (founders are protected server-side). */
    fun deleteUser(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.deleteUser(id) }
            _state.update { it.copy(busy = false) }
            refresh()
            onDone()
        }
    }

    /** Persists maintenance mode / max upload size, then refreshes the health card. */
    fun updateSettings(maintenance: Boolean? = null, maxUpload: Int? = null) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall {
                api.updateSettings(
                    SettingsUpdateRequest(
                        maintenanceMode = maintenance,
                        maxUploadSize = maxUpload
                    )
                )
            }.onFailure { e -> _state.update { it.copy(toast = e.toUiText()) } }
            _state.update { it.copy(busy = false) }
            refresh()
        }
    }

    /** Streams the credentials file for saving/opening on device. */
    suspend fun credentialsFile(): ResponseBody? =
        (safeApiCall { api.credentialsDownload() } as? ApiResult.Success)?.data
}
