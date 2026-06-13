package com.orbit.mobile.feature.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.NetworkError
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.domain.repository.AuthRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Strength level
data class PasswordStrength(val score: Int, val labelRes: Int?, val tone: StrengthTone)

enum class StrengthTone { NONE, DANGER, WARNING, SUCCESS }

// Strength calc
fun passwordStrength(p: String): PasswordStrength {
    if (p.isEmpty()) return PasswordStrength(0, null, StrengthTone.NONE)
    if (p.length < 8) return PasswordStrength(1, R.string.setup_pw_too_short, StrengthTone.DANGER)
    var s = 1
    if (p.any { it.isUpperCase() }) s++
    if (p.any { it.isDigit() }) s++
    if (p.any { !it.isLetterOrDigit() }) s++
    if (p.length >= 12) s++
    return when {
        s <= 2 -> PasswordStrength(s, R.string.setup_pw_weak, StrengthTone.DANGER)
        s <= 3 -> PasswordStrength(s, R.string.setup_pw_fair, StrengthTone.WARNING)
        s <= 4 -> PasswordStrength(s, R.string.setup_pw_strong, StrengthTone.SUCCESS)
        else -> PasswordStrength(s, R.string.setup_pw_very_strong, StrengthTone.SUCCESS)
    }
}

// Setup state
data class SetupUiState(
    val fullName: String = "",
    val email: String = "",
    val password: String = "",
    val confirm: String = "",
    val showPassword: Boolean = false,
    val showConfirm: Boolean = false,
    val loading: Boolean = false,
    val error: UiText? = null,
    val done: Boolean = false,
    val goLogin: Boolean = false
) {
    val strength: PasswordStrength get() = passwordStrength(password)
    val passwordOk: Boolean get() = password.length >= 8
    val confirmMismatch: Boolean get() = confirm.isNotEmpty() && confirm != password
    val canSubmit: Boolean
        get() = fullName.isNotBlank() && email.isNotBlank() && passwordOk &&
            confirm == password && !loading
}

// Setup VM
@HiltViewModel
class SetupViewModel @Inject constructor(
    private val repo: AuthRepository
) : ViewModel() {

    private val _state = MutableStateFlow(SetupUiState())
    val state: StateFlow<SetupUiState> = _state

    fun onNameChange(v: String) = _state.update { it.copy(fullName = v, error = null) }
    fun onEmailChange(v: String) = _state.update { it.copy(email = v, error = null) }
    fun onPasswordChange(v: String) = _state.update { it.copy(password = v, error = null) }
    fun onConfirmChange(v: String) = _state.update { it.copy(confirm = v, error = null) }
    fun togglePassword() = _state.update { it.copy(showPassword = !it.showPassword) }
    fun toggleConfirm() = _state.update { it.copy(showConfirm = !it.showConfirm) }

    // Submit setup
    fun submit() {
        val s = _state.value
        if (!s.passwordOk) {
            _state.update { it.copy(error = UiText.Res(R.string.setup_pw_min_error)) }
            return
        }
        if (s.password != s.confirm) {
            _state.update { it.copy(error = UiText.Res(R.string.setup_mismatch)) }
            return
        }
        if (!s.canSubmit) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.setupFounder(s.fullName.trim(), s.email.trim(), s.password)
                .onSuccess {
                    _state.update { it.copy(loading = false, done = true) }
                    // Redirect delay
                    delay(2_200)
                    _state.update { it.copy(goLogin = true) }
                }
                .onFailure { error ->
                    val text = when {
                        error is NetworkError.Http && error.code == 409 ->
                            UiText.Res(R.string.setup_exists)
                        else -> error.toUiText()
                    }
                    _state.update { it.copy(loading = false, error = text) }
                }
        }
    }
}
