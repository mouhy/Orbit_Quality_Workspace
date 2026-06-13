package com.orbit.mobile.feature.channels

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.ChatSocket
import com.orbit.mobile.core.network.ChatSocketFactory
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.BoardCommentDto
import com.orbit.mobile.data.dto.BoardDto
import com.orbit.mobile.data.dto.BoardTaskPatchRequest
import com.orbit.mobile.domain.repository.BoardRepository
import com.orbit.mobile.domain.repository.UploadPart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import javax.inject.Inject

// Post kinds
enum class PostKind { ANNOUNCEMENT, TASK, NOTE, PLAIN, REPLY }

// Parsed post
data class ParsedPost(
    val kind: PostKind,
    val title: String?,
    val content: String,
    val replyToId: String? = null
)

// Parse message
fun parseMessage(message: String): ParsedPost {
    val postMatch = Regex("^\\[POST:(announcement|task|note)]\\s?(.*)", RegexOption.DOT_MATCHES_ALL)
        .find(message)
    if (postMatch != null) {
        val kind = when (postMatch.groupValues[1]) {
            "announcement" -> PostKind.ANNOUNCEMENT
            "task" -> PostKind.TASK
            else -> PostKind.NOTE
        }
        val rest = postMatch.groupValues[2]
        val title = rest.substringBefore("\n").trim()
        val content = rest.substringAfter("\n", "").trim()
        return ParsedPost(kind, title.ifBlank { null }, content)
    }
    val replyMatch = Regex("^\\[REPLY:([^]]+)]\\n?(.*)", RegexOption.DOT_MATCHES_ALL)
        .find(message)
    if (replyMatch != null) {
        return ParsedPost(
            kind = PostKind.REPLY,
            title = null,
            content = replyMatch.groupValues[2].trim(),
            replyToId = replyMatch.groupValues[1]
        )
    }
    return ParsedPost(PostKind.PLAIN, null, message)
}

// Activity entry
data class ActivityEntry(
    val isFile: Boolean,
    val actor: String,
    val text: String,
    val time: String?
)

// Channel state
data class ChannelState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val toast: UiText? = null,
    val board: BoardDto? = null,
    val sending: Boolean = false,
    val busy: Boolean = false,
    val reactions: Map<String, Map<String, Int>> = emptyMap()
) {
    // Top posts
    val posts: List<BoardCommentDto>
        get() = board?.comments?.filter {
            parseMessage(it.message).kind != PostKind.REPLY
        }?.reversed() ?: emptyList()

    // Replies map
    val replies: Map<String, List<BoardCommentDto>>
        get() = board?.comments
            ?.mapNotNull { c ->
                val parsed = parseMessage(c.message)
                if (parsed.kind == PostKind.REPLY && parsed.replyToId != null) {
                    parsed.replyToId to c
                } else null
            }
            ?.groupBy({ it.first }, { it.second })
            ?: emptyMap()

    // Feed activity
    val activity: List<ActivityEntry>
        get() {
            val comments = (board?.comments ?: emptyList()).takeLast(20).map {
                ActivityEntry(
                    isFile = false,
                    actor = it.userName,
                    text = parseMessage(it.message).let { p ->
                        p.title ?: p.content.take(60)
                    },
                    time = it.createdAt
                )
            }
            val files = (board?.resources ?: emptyList()).takeLast(5).map {
                ActivityEntry(
                    isFile = true,
                    actor = it.uploaderName ?: it.uploaderRole,
                    text = it.fileName,
                    time = it.createdAt
                )
            }
            return (comments + files).sortedByDescending { it.time ?: "" }
        }

    val onlineCount: Int get() = board?.members?.count { it.online } ?: 0
}

// Channel VM
@HiltViewModel
class TaskFlowViewModel @Inject constructor(
    private val repo: BoardRepository,
    private val session: SessionManager,
    socketFactory: ChatSocketFactory,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: String = savedStateHandle.get<String>("projectId") ?: "public-group"

    private val _state = MutableStateFlow(ChannelState())
    val state: StateFlow<ChannelState> = _state

    val myUserId: String? get() = session.userId

    fun canDelete(ownerId: String?): Boolean =
        ownerId == myUserId || session.role in setOf("admin", "sub_admin", "manager")

    private val socket: ChatSocket? =
        if (projectId.isNotBlank()) socketFactory.create(projectId) else null

    init {
        refresh()
        // Live updates
        socket?.open()
        viewModelScope.launch {
            socket?.events?.collect { refreshSilent() }
        }
    }

    override fun onCleared() {
        socket?.close()
        super.onCleared()
    }

    // Full load
    fun refresh() {
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.board(projectId)
                .onSuccess { b -> _state.update { it.copy(loading = false, board = b) } }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }

    private fun refreshSilent() {
        viewModelScope.launch {
            repo.board(projectId).onSuccess { b -> _state.update { it.copy(board = b) } }
        }
    }

    // Apply result
    private fun apply(result: ApiResult<BoardDto>) {
        when (result) {
            is ApiResult.Success ->
                _state.update { it.copy(board = result.data, busy = false, sending = false) }
            is ApiResult.Failure -> _state.update {
                it.copy(busy = false, sending = false, toast = result.error.toUiText())
            }
        }
    }

    // Send post
    fun sendPost(
        type: String,
        title: String,
        content: String,
        attachments: List<UploadPart>,
        mentionedUserIds: List<String>
    ) {
        if (title.isBlank() && content.isBlank() && attachments.isEmpty()) return
        val message = "[POST:$type] ${title.trim()}\n${content.trim()}"
        viewModelScope.launch {
            _state.update { it.copy(sending = true) }
            apply(repo.sendComment(projectId, message, attachments, mentionedUserIds, null))
        }
    }

    // Send reply
    fun sendReply(commentId: String, text: String, mentionedUserIds: List<String>) {
        if (text.isBlank()) return
        val message = "[REPLY:$commentId]\n${text.trim()}"
        viewModelScope.launch {
            _state.update { it.copy(sending = true) }
            apply(repo.sendComment(projectId, message, emptyList(), mentionedUserIds, null))
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            apply(repo.deleteComment(projectId, commentId))
        }
    }

    // Client reactions
    fun react(commentId: String, emoji: String) {
        _state.update { s ->
            val current = s.reactions[commentId] ?: emptyMap()
            val count = current[emoji] ?: 0
            s.copy(
                reactions = s.reactions +
                    (commentId to (current + (emoji to count + 1)))
            )
        }
    }

    // Todo toggle
    fun toggleTodo(taskId: String, done: Boolean) {
        val before = _state.value.board ?: return
        // Optimistic flip
        _state.update { s ->
            s.copy(board = before.copy(tasks = before.tasks.map {
                if (it.id == taskId) it.copy(done = done) else it
            }))
        }
        viewModelScope.launch {
            val result = repo.patchTodo(projectId, taskId, BoardTaskPatchRequest(done = done))
            when (result) {
                is ApiResult.Success -> _state.update { it.copy(board = result.data) }
                is ApiResult.Failure -> _state.update {
                    it.copy(board = before, toast = result.error.toUiText())
                }
            }
        }
    }

    // Resources
    fun uploadResource(file: UploadPart) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            apply(repo.uploadResource(projectId, file))
        }
    }

    fun deleteResource(resourceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            apply(repo.deleteResource(projectId, resourceId))
        }
    }

    suspend fun downloadResourceBody(resourceId: String): ResponseBody? =
        (repo.downloadResource(projectId, resourceId) as? ApiResult.Success)?.data

    suspend fun downloadAttachmentBody(commentId: String, attachmentId: String): ResponseBody? =
        (repo.downloadAttachment(projectId, commentId, attachmentId) as? ApiResult.Success)?.data
}
