package com.orbit.mobile.feature.settings

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.OrbitDataStore
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.core.theme.ThemeMode
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.api.ItApi
import com.orbit.mobile.data.dto.AdminPasswordResetRequest
import com.orbit.mobile.data.dto.MeDto
import com.orbit.mobile.data.dto.ProfileUpdateRequest
import com.orbit.mobile.data.dto.SessionLogDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import com.orbit.mobile.core.network.ApiResult

// DataStore keys for the three local notification toggles
const val PREF_NOTIF_PUSH = "notif_push"
const val PREF_NOTIF_EMAIL = "notif_email"
const val PREF_NOTIF_MENTIONS = "notif_mentions"

/** Settings page state: profile fields, security data and async flags. */
data class SettingsState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val toast: UiText? = null,
    val me: MeDto? = null,
    val mySessions: List<SessionLogDto> = emptyList(),
    val busy: Boolean = false,
    val passwordSaved: Boolean = false
)

@HiltViewModel
class SettingsViewModel @Inject constructor(
    private val api: ItApi,
    private val session: SessionManager,
    private val dataStore: OrbitDataStore
) : ViewModel() {

    private val _state = MutableStateFlow(SettingsState())
    val state: StateFlow<SettingsState> = _state

    // Live theme + notification preferences from DataStore
    val themeMode: StateFlow<ThemeMode> = dataStore.themeFlow
        .stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.DARK)
    val notifPush: StateFlow<Boolean> = dataStore.flagFlow(PREF_NOTIF_PUSH)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifEmail: StateFlow<Boolean> = dataStore.flagFlow(PREF_NOTIF_EMAIL)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)
    val notifMentions: StateFlow<Boolean> = dataStore.flagFlow(PREF_NOTIF_MENTIONS)
        .stateIn(viewModelScope, SharingStarted.Eagerly, true)

    init {
        refresh()
    }

    /** Loads profile + own sessions in parallel. */
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val meDeferred = async { safeApiCall { api.me() } }
            val sessionsDeferred = async { safeApiCall { api.mySessions() } }

            val meResult = meDeferred.await()
            _state.update { current ->
                var next = current.copy(loading = false)
                when (meResult) {
                    is ApiResult.Success -> next = next.copy(me = meResult.data)
                    is ApiResult.Failure -> next = next.copy(error = meResult.error.toUiText())
                }
                (sessionsDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(mySessions = it.data)
                }
                next
            }
        }
    }

    /** Saves profile fields; keeps the in-memory session name in sync. */
    fun saveProfile(body: ProfileUpdateRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.updateProfile(body) }
                .onSuccess { me ->
                    body.name?.let { session.updateName(it) }
                    me.avatar?.let { session.updateAvatar(it) }
                    _state.update { it.copy(me = me) }
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Uploads an avatar as a base64 data-URI through the profile PATCH. */
    fun uploadAvatar(bytes: ByteArray, mime: String) {
        val dataUri = "data:$mime;base64," +
            android.util.Base64.encodeToString(bytes, android.util.Base64.NO_WRAP)
        saveProfile(ProfileUpdateRequest(avatar = dataUri)) { _, err ->
            err?.let { e -> _state.update { it.copy(toast = e) } }
        }
    }

    /** Removes the avatar, reverting to the initials fallback. */
    fun deleteAvatar() {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.deleteAvatar() }
                .onSuccess {
                    session.updateAvatar(null)
                    refresh()
                }
                .onFailure { e -> _state.update { it.copy(toast = e.toUiText()) } }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Changes the signed-in user's own password. */
    fun changePassword(newPassword: String, onDone: (Boolean, UiText?) -> Unit) {
        val userId = session.userId ?: return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.resetPassword(AdminPasswordResetRequest(userId, newPassword)) }
                .onSuccess { onDone(true, null) }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    /** Persists the theme; MainActivity recomposes instantly via themeFlow. */
    fun setTheme(mode: ThemeMode) {
        viewModelScope.launch { dataStore.setTheme(mode) }
    }

    /** Persists one local notification toggle. */
    fun setFlag(name: String, value: Boolean) {
        viewModelScope.launch { dataStore.setFlag(name, value) }
    }
}
