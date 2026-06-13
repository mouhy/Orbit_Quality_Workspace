package com.orbit.mobile.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.api.EventsApi
import com.orbit.mobile.data.dto.EventCreateRequest
import com.orbit.mobile.data.dto.EventDto
import com.orbit.mobile.data.dto.RsvpRequest
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.domain.repository.DashboardRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.YearMonth
import javax.inject.Inject
import com.orbit.mobile.core.network.ApiResult

// Event consts
val EVENT_TYPES = listOf(
    "meeting", "project_deadline", "review_session", "training_session",
    "quality_audit", "team_event", "reminder", "personal_event",
    "task_deadline", "milestone"
)
val EVENT_VISIBILITIES = listOf("private", "team", "project_members", "workspace")
val EVENT_PRIORITIES = listOf("low", "medium", "high", "urgent")
val EVENT_RECURRENCES = listOf("none", "daily", "weekly", "monthly", "yearly")
val REMINDER_OPTIONS = listOf(15, 60, 720, 1440)

// Calendar state
data class CalendarState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val month: YearMonth = YearMonth.now(),
    val selectedDate: LocalDate = LocalDate.now(),
    val typeFilter: String? = null,
    val events: List<EventDto> = emptyList(),
    val users: List<UserDto> = emptyList(),
    val busy: Boolean = false
) {
    val filtered: List<EventDto>
        get() = if (typeFilter == null) events else events.filter { it.type == typeFilter }

    fun eventsOn(date: LocalDate): List<EventDto> {
        val key = date.toString()
        return filtered.filter { event ->
            val start = event.startDate.take(10)
            val end = (event.endDate ?: event.startDate).take(10)
            key >= start && key <= end
        }
    }
}

// Calendar VM
@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val api: EventsApi,
    private val dashRepo: DashboardRepository,
    private val session: SessionManager
) : ViewModel() {

    private val _state = MutableStateFlow(CalendarState())
    val state: StateFlow<CalendarState> = _state

    val myUserId: String? get() = session.userId

    init {
        refresh()
        loadUsers()
    }

    fun setMonth(month: YearMonth) {
        _state.update { it.copy(month = month) }
        refresh()
    }

    fun selectDate(date: LocalDate) = _state.update { it.copy(selectedDate = date) }

    fun setTypeFilter(type: String?) = _state.update { it.copy(typeFilter = type) }

    // Load month
    fun refresh() {
        val month = _state.value.month
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            safeApiCall {
                api.list(
                    startDate = month.atDay(1).toString(),
                    endDate = month.atEndOfMonth().toString()
                )
            }
                .onSuccess { list -> _state.update { it.copy(loading = false, events = list) } }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }

    private fun loadUsers() {
        viewModelScope.launch {
            (dashRepo.users() as? ApiResult.Success)?.let { result ->
                _state.update { it.copy(users = result.data) }
            }
        }
    }

    // Create event
    fun create(body: EventCreateRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.create(body) }
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    // Update event
    fun update(id: String, body: EventCreateRequest, onDone: (Boolean, UiText?) -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.update(id, body) }
                .onSuccess {
                    refresh()
                    onDone(true, null)
                }
                .onFailure { onDone(false, it.toUiText()) }
            _state.update { it.copy(busy = false) }
        }
    }

    // Delete event
    fun delete(id: String, onDone: () -> Unit) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            safeApiCall { api.delete(id) }
            _state.update { it.copy(busy = false) }
            refresh()
            onDone()
        }
    }

    // RSVP
    fun rsvp(id: String, status: String) {
        viewModelScope.launch {
            safeApiCall { api.rsvp(id, RsvpRequest(status)) }
            refresh()
        }
    }
}
