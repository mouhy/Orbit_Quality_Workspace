package com.orbit.mobile.feature.founder

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.api.FounderApi
import com.orbit.mobile.data.dto.AssignItRequest
import com.orbit.mobile.data.dto.EntityCreateRequest
import com.orbit.mobile.data.dto.EntityDto
import com.orbit.mobile.data.dto.FounderAccountCreateRequest
import com.orbit.mobile.data.dto.FounderAccountDto
import com.orbit.mobile.data.dto.FounderAccountPatchRequest
import com.orbit.mobile.data.dto.FounderMetricsDto
import com.orbit.mobile.data.dto.FrameworkDto
import com.orbit.mobile.data.dto.FrameworkRequest
import com.orbit.mobile.data.dto.NewPasswordRequest
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

/** Aggregated state for the founder dashboard and the IT accounts page. */
data class FounderState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val toast: UiText? = null,
    val metrics: FounderMetricsDto? = null,
    val accounts: List<FounderAccountDto> = emptyList(),
    val frameworks: List<FrameworkDto> = emptyList(),
    val entities: List<EntityDto> = emptyList(),
    val search: String = "",
    val busy: Boolean = false
) {
    // Accounts filtered by the search box
    val filteredAccounts: List<FounderAccountDto>
        get() {
            val q = search.trim().lowercase()
            if (q.isEmpty()) return accounts
            return accounts.filter {
                it.name.lowercase().contains(q) || it.email.lowercase().contains(q)
            }
        }
}

@HiltViewModel
class FounderViewModel @Inject constructor(
    private val api: FounderApi
) : ViewModel() {

    private val _state = MutableStateFlow(FounderState())
    val state: StateFlow<FounderState> = _state

    init {
        refresh()
    }

    fun setSearch(value: String) = _state.update { it.copy(search = value) }

    /** Loads metrics, accounts, frameworks and entities concurrently. */
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val metricsDeferred = async { safeApiCall { api.metrics() } }
            val accountsDeferred = async { safeApiCall { api.accounts() } }
            val frameworksDeferred = async { safeApiCall { api.frameworks() } }
            val entitiesDeferred = async { safeApiCall { api.entities() } }

            val metricsResult = metricsDeferred.await()

            _state.update { current ->
                var next = current.copy(loading = false)
                when (metricsResult) {
                    is ApiResult.Success -> next = next.copy(metrics = metricsResult.data)
                    is ApiResult.Failure ->
                        next = next.copy(error = metricsResult.error.toUiText())
                }
                (accountsDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(accounts = it.data)
                }
                (frameworksDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(frameworks = it.data)
                }
                (entitiesDeferred.await() as? ApiResult.Success)?.let {
                    next = next.copy(entities = it.data)
                }
                next
            }
        }
    }

    // Generic helper: run a mutation, surface the result, refresh on success
    private fun mutate(
        onDone: ((Boolean, UiText?) -> Unit)? = null,
        block: suspend () -> ApiResult<*>
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            when (val result = block()) {
                is ApiResult.Success -> {
                    refresh()
                    onDone?.invoke(true, null)
                }
                is ApiResult.Failure -> {
                    onDone?.invoke(false, result.error.toUiText())
                        ?: _state.update { it.copy(toast = result.error.toUiText()) }
                }
            }
            _state.update { it.copy(busy = false) }
        }
    }

    fun createAccount(body: FounderAccountCreateRequest, onDone: (Boolean, UiText?) -> Unit) =
        mutate(onDone) { safeApiCall { api.createAccount(body) } }

    fun patchAccount(
        id: String,
        body: FounderAccountPatchRequest,
        onDone: (Boolean, UiText?) -> Unit
    ) = mutate(onDone) { safeApiCall { api.patchAccount(id, body) } }

    fun resetAccountPassword(id: String, password: String, onDone: (Boolean, UiText?) -> Unit) =
        mutate(onDone) {
            safeApiCall { api.resetAccountPassword(id, NewPasswordRequest(password)) }
        }

    fun toggleStatus(id: String) = mutate { safeApiCall { api.toggleAccountStatus(id) } }

    fun deleteAccount(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.deleteAccount(id) }
            _state.update { it.copy(busy = false) }
            refresh()
            onDone()
        }
    }

    fun saveFramework(
        editingId: String?,
        body: FrameworkRequest,
        onDone: (Boolean, UiText?) -> Unit
    ) = mutate(onDone) {
        if (editingId == null) safeApiCall { api.createFramework(body) }
        else safeApiCall { api.updateFramework(editingId, body) }
    }

    fun createEntity(body: EntityCreateRequest, onDone: (Boolean, UiText?) -> Unit) =
        mutate(onDone) { safeApiCall { api.createEntity(body) } }

    fun assignIt(entityId: String, itStaffIds: List<String>, onDone: (Boolean, UiText?) -> Unit) =
        mutate(onDone) { safeApiCall { api.assignIt(entityId, AssignItRequest(itStaffIds)) } }
}
