package com.orbit.mobile.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.datastore.UserSession
import com.orbit.mobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Shell VM
@HiltViewModel
class ShellViewModel @Inject constructor(
    private val repo: AuthRepository,
    sessionManager: SessionManager
) : ViewModel() {

    val session: StateFlow<UserSession> = sessionManager.session

    private val _loggedOut = MutableStateFlow(false)
    val loggedOut: StateFlow<Boolean> = _loggedOut

    private val _loggingOut = MutableStateFlow(false)
    val loggingOut: StateFlow<Boolean> = _loggingOut

    // Logout blacklist
    fun logout() {
        if (_loggingOut.value) return
        _loggingOut.value = true
        viewModelScope.launch {
            repo.logout()
            _loggingOut.value = false
            _loggedOut.value = true
        }
    }
}
