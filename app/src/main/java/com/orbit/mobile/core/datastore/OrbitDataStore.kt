package com.orbit.mobile.core.datastore

import android.content.Context
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.orbit.mobile.core.security.CryptoManager
import com.orbit.mobile.core.theme.ThemeMode
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

// Prefs file
private val Context.orbitPrefs by preferencesDataStore(name = "orbit_prefs")

// Local storage
@Singleton
class OrbitDataStore @Inject constructor(
    @ApplicationContext context: Context
) {
    private val store = context.orbitPrefs

    // Keys
    object Keys {
        val TOKEN = stringPreferencesKey("token")
        val USER_ID = stringPreferencesKey("userId")
        val ROLE = stringPreferencesKey("role")
        val FULL_NAME = stringPreferencesKey("fullName")
        val EMAIL = stringPreferencesKey("email")
        val AVATAR = stringPreferencesKey("userAvatar")
        val THEME = stringPreferencesKey("theme")
        val BASE_URL = stringPreferencesKey("baseUrl")
        val ONBOARDING_SEEN = booleanPreferencesKey("onboarding_seen")
    }

    // Raw flow
    val data: Flow<Preferences> = store.data

    // Theme flow
    val themeFlow: Flow<ThemeMode> = store.data.map { ThemeMode.from(it[Keys.THEME]) }

    // One-shot read
    suspend fun snapshot(): Preferences = store.data.first()

    // Onboarding read
    suspend fun isOnboardingSeen(): Boolean = store.data.first()[Keys.ONBOARDING_SEEN] ?: false

    // Onboarding mark
    suspend fun setOnboardingSeen() {
        store.edit { it[Keys.ONBOARDING_SEEN] = true }
    }

    // Save session
    suspend fun saveSession(
        token: String,
        userId: String,
        role: String,
        fullName: String,
        email: String,
        avatar: String?
    ) {
        store.edit {
            // Encrypt token
            it[Keys.TOKEN] = CryptoManager.encrypt(token)
            it[Keys.USER_ID] = userId
            it[Keys.ROLE] = role
            it[Keys.FULL_NAME] = fullName
            it[Keys.EMAIL] = email
            if (avatar.isNullOrBlank()) it.remove(Keys.AVATAR) else it[Keys.AVATAR] = avatar
        }
    }

    // Clear session
    suspend fun clearSession() {
        store.edit {
            it.remove(Keys.TOKEN)
            it.remove(Keys.USER_ID)
            it.remove(Keys.ROLE)
            it.remove(Keys.FULL_NAME)
            it.remove(Keys.EMAIL)
            it.remove(Keys.AVATAR)
        }
    }

    // Setters
    suspend fun setTheme(mode: ThemeMode) {
        store.edit { it[Keys.THEME] = mode.storageValue }
    }

    suspend fun setBaseUrl(url: String?) {
        store.edit {
            if (url.isNullOrBlank()) it.remove(Keys.BASE_URL) else it[Keys.BASE_URL] = url
        }
    }

    suspend fun setAvatar(avatar: String?) {
        store.edit {
            if (avatar.isNullOrBlank()) it.remove(Keys.AVATAR) else it[Keys.AVATAR] = avatar
        }
    }

    suspend fun setFullName(name: String) {
        store.edit { it[Keys.FULL_NAME] = name }
    }

    // Generic boolean preference (notification toggles etc.)
    suspend fun setFlag(name: String, value: Boolean) {
        store.edit { it[booleanPreferencesKey(name)] = value }
    }

    // Observe one boolean preference with a default
    fun flagFlow(name: String, default: Boolean = true): Flow<Boolean> =
        store.data.map { it[booleanPreferencesKey(name)] ?: default }
}
