package com.orbit.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.OrbitDataStore
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.NetworkError
import com.orbit.mobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

// Boot states
sealed interface BootState {
    data object Loading : BootState
    data object BackendDown : BootState
    data object NeedsSetup : BootState
    data class Ready(
        val loggedIn: Boolean,
        val role: String?,
        val onboardingSeen: Boolean
    ) : BootState
}

// Boot check
@HiltViewModel
class SplashViewModel @Inject constructor(
    private val repo: AuthRepository,
    private val session: SessionManager,
    private val dataStore: OrbitDataStore
) : ViewModel() {

    private val _state = MutableStateFlow<BootState>(BootState.Loading)
    val state: StateFlow<BootState> = _state

    init {
        check()
    }

    // Init status
    fun check() {
        _state.value = BootState.Loading
        viewModelScope.launch {
            when (val result = repo.checkInitialized()) {
                is ApiResult.Success -> {
                    if (!result.data) {
                        _state.value = BootState.NeedsSetup
                    } else {
                        ready()
                    }
                }
                is ApiResult.Failure -> {
                    // Timeout only
                    if (result.error is NetworkError.Timeout) {
                        _state.value = BootState.BackendDown
                    } else {
                        ready()
                    }
                }
            }
        }
    }

    // Route home
    private suspend fun ready() {
        _state.value = BootState.Ready(
            loggedIn = session.isLoggedIn,
            role = session.role,
            onboardingSeen = dataStore.isOnboardingSeen()
        )
    }
}
