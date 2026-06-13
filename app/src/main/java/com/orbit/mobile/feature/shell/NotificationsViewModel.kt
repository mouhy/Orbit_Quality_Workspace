package com.orbit.mobile.feature.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.domain.model.Notification
import com.orbit.mobile.domain.repository.NotificationsRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject

// Poll interval
private const val POLL_MS = 30_000L

// Notifications VM
@HiltViewModel
class NotificationsViewModel @Inject constructor(
    private val repo: NotificationsRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<Notification>>(emptyList())
    val items: StateFlow<List<Notification>> = _items

    val unreadCount: Int get() = _items.value.count { !it.isRead }

    init {
        // Poll 30s
        viewModelScope.launch {
            while (isActive) {
                refresh()
                delay(POLL_MS)
            }
        }
    }

    // Fetch list
    fun refresh() {
        viewModelScope.launch {
            repo.list().onSuccess { _items.value = it }
        }
    }

    // Mark all
    fun markAllRead() {
        _items.value = _items.value.map { it.copy(isRead = true) }
        viewModelScope.launch { repo.markAllRead() }
    }

    // Dismiss one
    fun dismiss(id: String) {
        _items.value = _items.value.filterNot { it.id == id }
        viewModelScope.launch { repo.markRead(id) }
    }

    // Mark read
    fun markRead(id: String) {
        _items.value = _items.value.map { if (it.id == id) it.copy(isRead = true) else it }
        viewModelScope.launch { repo.markRead(id) }
    }
}
