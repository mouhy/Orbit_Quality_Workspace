package com.orbit.mobile.core.datastore

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import javax.inject.Inject
import javax.inject.Singleton

// Session snapshot
data class UserSession(
    val token: String? = null,
    val userId: String? = null,
    val role: String? = null,
    val fullName: String? = null,
    val email: String? = null,
    val avatar: String? = null
) {
    val isLoggedIn: Boolean get() = !token.isNullOrBlank() && !userId.isNullOrBlank()
}

// Session holder
@Singleton
class SessionManager @Inject constructor(
    private val store: OrbitDataStore
) {
    private val _session = MutableStateFlow(UserSession())
    val session: StateFlow<UserSession> = _session

    // Quick access
    val token: String? get() = _session.value.token
    val userId: String? get() = _session.value.userId
    val role: String? get() = _session.value.role
    val isLoggedIn: Boolean get() = _session.value.isLoggedIn

    // Restore stored
    suspend fun restore() {
        val prefs = store.snapshot()
        _session.value = UserSession(
            token = prefs[OrbitDataStore.Keys.TOKEN],
            userId = prefs[OrbitDataStore.Keys.USER_ID],
            role = prefs[OrbitDataStore.Keys.ROLE],
            fullName = prefs[OrbitDataStore.Keys.FULL_NAME],
            email = prefs[OrbitDataStore.Keys.EMAIL],
            avatar = prefs[OrbitDataStore.Keys.AVATAR]
        )
    }

    // Save login
    suspend fun save(
        token: String,
        userId: String,
        role: String,
        fullName: String,
        email: String,
        avatar: String? = null
    ) {
        _session.value = UserSession(token, userId, role, fullName, email, avatar)
        store.saveSession(token, userId, role, fullName, email, avatar)
    }

    // Update avatar
    suspend fun updateAvatar(avatar: String?) {
        _session.value = _session.value.copy(avatar = avatar)
        store.setAvatar(avatar)
    }

    // Update name
    suspend fun updateName(name: String) {
        _session.value = _session.value.copy(fullName = name)
        store.setFullName(name)
    }

    // Clear all
    suspend fun clear() {
        _session.value = UserSession()
        store.clearSession()
    }
}
