package com.orbit.mobile.feature.chatbot

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.data.api.ChatApi
import com.orbit.mobile.data.dto.ChatRequestDto
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Chat message
data class BotMessage(
    val fromUser: Boolean,
    val text: String
)

// Chatbot state
data class ChatbotState(
    val open: Boolean = false,
    val typing: Boolean = false,
    val unread: Int = 0,
    val messages: List<BotMessage> = emptyList()
)

// Chatbot VM
@HiltViewModel
class ChatbotViewModel @Inject constructor(
    private val api: ChatApi
) : ViewModel() {

    private val _state = MutableStateFlow(ChatbotState())
    val state: StateFlow<ChatbotState> = _state

    fun open() = _state.update { it.copy(open = true, unread = 0) }
    fun close() = _state.update { it.copy(open = false) }

    // Send message
    fun send(text: String) {
        if (text.isBlank() || _state.value.typing) return
        _state.update {
            it.copy(
                messages = it.messages + BotMessage(fromUser = true, text = text.trim()),
                typing = true
            )
        }
        viewModelScope.launch {
            val result = safeApiCall { api.send(ChatRequestDto(message = text.trim())) }
            val reply = when (result) {
                is ApiResult.Success -> result.data.reply
                is ApiResult.Failure -> ""
            }
            _state.update { s ->
                s.copy(
                    typing = false,
                    messages = if (reply.isNotBlank()) {
                        s.messages + BotMessage(fromUser = false, text = reply)
                    } else s.messages,
                    unread = if (!s.open && reply.isNotBlank()) s.unread + 1 else s.unread
                )
            }
        }
    }
}
