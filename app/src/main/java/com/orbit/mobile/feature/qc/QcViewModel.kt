package com.orbit.mobile.feature.qc

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.parseInstant
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.api.QcSuiteApi
import com.orbit.mobile.data.dto.EvaluationItemDto
import com.orbit.mobile.data.dto.QcOverviewDto
import com.orbit.mobile.data.dto.StandardCreateRequest
import com.orbit.mobile.data.dto.StandardDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import javax.inject.Inject

// Heat weeks
const val QC_HEAT_WEEKS = 16

// QC state
data class QcState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val overview: QcOverviewDto? = null,
    val standards: List<StandardDto> = emptyList(),
    val evaluations: List<EvaluationItemDto> = emptyList(),
    val busy: Boolean = false
) {
    // Activity heatmap
    val heatmap: List<List<Int>>
        get() {
            val grid = List(7) { IntArray(QC_HEAT_WEEKS) }
            evaluations.forEach { item ->
                val instant = parseInstant(item.createdAt) ?: return@forEach
                val days = ((System.currentTimeMillis() - instant.toEpochMilli()) / 86_400_000L)
                    .toInt()
                if (days >= QC_HEAT_WEEKS * 7 || days < 0) return@forEach
                val week = QC_HEAT_WEEKS - 1 - days / 7
                val day = java.time.Instant.ofEpochMilli(instant.toEpochMilli())
                    .atZone(java.time.ZoneId.systemDefault()).dayOfWeek.value % 7
                if (week in 0 until QC_HEAT_WEEKS) grid[day][week]++
            }
            return grid.map { it.toList() }
        }

    val avgScore: Int
        get() = if (evaluations.isEmpty()) 0
        else (evaluations.sumOf { it.complianceScore } / evaluations.size).toInt()
}

// QC VM
@HiltViewModel
class QcViewModel @Inject constructor(
    private val api: QcSuiteApi
) : ViewModel() {

    private val _state = MutableStateFlow(QcState())
    val state: StateFlow<QcState> = _state

    init {
        refresh()
    }

    // Load all
    fun refresh(days: Int? = null) {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            val overviewDeferred = async { safeApiCall { api.overview(days = days) } }
            val standardsDeferred = async { safeApiCall { api.standards() } }
            val evaluationsDeferred = async { safeApiCall { api.evaluations() } }

            val overviewResult = overviewDeferred.await()
            val standardsResult = standardsDeferred.await()
            val evaluationsResult = evaluationsDeferred.await()

            _state.update { current ->
                var next = current.copy(loading = false)
                when (overviewResult) {
                    is ApiResult.Success -> next = next.copy(overview = overviewResult.data)
                    is ApiResult.Failure ->
                        next = next.copy(error = overviewResult.error.toUiText())
                }
                (standardsResult as? ApiResult.Success)?.let {
                    next = next.copy(standards = it.data)
                }
                (evaluationsResult as? ApiResult.Success)?.let {
                    next = next.copy(evaluations = it.data)
                }
                next
            }
        }
    }

    // Save standard
    fun saveStandard(
        editingId: String?,
        body: StandardCreateRequest,
        onDone: (Boolean, UiText?) -> Unit
    ) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            val result = if (editingId == null) {
                safeApiCall { api.createStandard(body) }
            } else {
                safeApiCall { api.updateStandard(editingId, body) }
            }
            result
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    // Archive standard
    fun archiveStandard(id: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.archiveStandard(id) }
            _state.update { it.copy(busy = false) }
            refresh()
        }
    }

    // Export body
    suspend fun export(format: String): ResponseBody? =
        (safeApiCall { api.export(format) } as? ApiResult.Success)?.data
}
