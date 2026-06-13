package com.orbit.mobile.feature.workspace

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
import com.orbit.mobile.data.dto.AvailableMemberDto
import com.orbit.mobile.data.dto.BoardDto
import com.orbit.mobile.data.dto.BoardMemberPatchRequest
import com.orbit.mobile.data.dto.BoardTaskCreateRequest
import com.orbit.mobile.data.dto.BoardTaskDto
import com.orbit.mobile.data.dto.BoardTaskPatchRequest
import com.orbit.mobile.domain.repository.BoardRepository
import com.orbit.mobile.domain.repository.ReplyMeta
import com.orbit.mobile.domain.repository.UploadPart
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import okhttp3.ResponseBody
import javax.inject.Inject

// Column keys
val BOARD_COLUMNS = listOf("todo", "in_progress", "review", "done")

// Board state
data class WorkspaceState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val toast: UiText? = null,
    val board: BoardDto? = null,
    val busy: Boolean = false,
    val sendingComment: Boolean = false,
    val availableMembers: List<AvailableMemberDto> = emptyList(),
    val selectedTaskId: String? = null
) {
    val selectedTask: BoardTaskDto?
        get() = board?.tasks?.firstOrNull { it.id == selectedTaskId }

    fun tasksFor(column: String): List<BoardTaskDto> =
        board?.tasks?.filter { it.statusKey == column } ?: emptyList()
}

// Workspace VM
@HiltViewModel
class WorkspaceViewModel @Inject constructor(
    private val repo: BoardRepository,
    private val session: SessionManager,
    socketFactory: ChatSocketFactory,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val projectId: String = savedStateHandle.get<String>("projectId") ?: ""

    private val _state = MutableStateFlow(WorkspaceState())
    val state: StateFlow<WorkspaceState> = _state

    val myUserId: String? get() = session.userId
    val myName: String get() = session.session.value.fullName ?: ""
    val myRole: String? get() = session.role

    // Delete rights
    fun canDelete(ownerId: String?): Boolean =
        ownerId == myUserId || myRole in setOf("admin", "sub_admin", "manager")

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
        if (projectId.isBlank()) return
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.board(projectId)
                .onSuccess { board ->
                    _state.update { it.copy(loading = false, board = board) }
                }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }

    // Silent reload
    private fun refreshSilent() {
        viewModelScope.launch {
            repo.board(projectId).onSuccess { board ->
                _state.update { it.copy(board = board) }
            }
        }
    }

    fun selectTask(taskId: String?) = _state.update { it.copy(selectedTaskId = taskId) }

    fun clearToast() = _state.update { it.copy(toast = null) }

    // Apply result
    private fun applyBoard(result: ApiResult<BoardDto>, rollback: BoardDto? = null) {
        when (result) {
            is ApiResult.Success ->
                _state.update { it.copy(board = result.data, busy = false) }
            is ApiResult.Failure -> _state.update {
                it.copy(
                    board = rollback ?: it.board,
                    busy = false,
                    toast = result.error.toUiText()
                )
            }
        }
    }

    // Inline create
    fun addTask(column: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(
                repo.addTodo(
                    projectId,
                    BoardTaskCreateRequest(
                        title = title.trim(),
                        status = column,
                        createdBy = myUserId
                    )
                )
            )
        }
    }

    // Move status
    fun moveTask(task: BoardTaskDto, newStatus: String) {
        val before = _state.value.board ?: return
        // Optimistic move
        _state.update { s ->
            s.copy(board = before.copy(tasks = before.tasks.map {
                if (it.id == task.id) {
                    it.copy(status = newStatus, done = newStatus == "done")
                } else it
            }))
        }
        viewModelScope.launch {
            applyBoard(
                repo.patchTodo(
                    projectId,
                    task.id,
                    BoardTaskPatchRequest(status = newStatus, done = newStatus == "done")
                ),
                rollback = before
            )
        }
    }

    // Patch task
    fun patchTask(taskId: String, body: BoardTaskPatchRequest) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(repo.patchTodo(projectId, taskId, body))
        }
    }

    // Delete task
    fun deleteTask(taskId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true, selectedTaskId = null) }
            applyBoard(repo.deleteTodo(projectId, taskId))
        }
    }

    // Members ops
    fun loadAvailableMembers(search: String? = null) {
        viewModelScope.launch {
            repo.availableMembers(projectId, search).onSuccess { list ->
                _state.update { it.copy(availableMembers = list) }
            }
        }
    }

    fun addMember(userId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(repo.addMember(projectId, userId))
        }
    }

    fun updateResponsibility(memberId: String, responsibility: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(
                repo.patchMember(
                    projectId,
                    memberId,
                    BoardMemberPatchRequest(action = "update", responsibility = responsibility)
                )
            )
        }
    }

    fun removeMember(memberId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(
                repo.patchMember(
                    projectId,
                    memberId,
                    BoardMemberPatchRequest(action = "remove")
                )
            )
        }
    }

    // Chat ops
    fun sendComment(
        message: String?,
        attachments: List<UploadPart>,
        mentionedUserIds: List<String>,
        replyTo: ReplyMeta?
    ) {
        if (message.isNullOrBlank() && attachments.isEmpty()) return
        viewModelScope.launch {
            _state.update { it.copy(sendingComment = true) }
            val result = repo.sendComment(projectId, message, attachments, mentionedUserIds, replyTo)
            when (result) {
                is ApiResult.Success ->
                    _state.update { it.copy(board = result.data, sendingComment = false) }
                is ApiResult.Failure -> _state.update {
                    it.copy(sendingComment = false, toast = result.error.toUiText())
                }
            }
        }
    }

    fun deleteComment(commentId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(repo.deleteComment(projectId, commentId))
        }
    }

    // Resource ops
    fun uploadResource(file: UploadPart) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(repo.uploadResource(projectId, file))
        }
    }

    fun deleteResource(resourceId: String) {
        viewModelScope.launch {
            _state.update { it.copy(busy = true) }
            applyBoard(repo.deleteResource(projectId, resourceId))
        }
    }

    // Downloads
    suspend fun downloadResourceBody(resourceId: String): ResponseBody? =
        (repo.downloadResource(projectId, resourceId) as? ApiResult.Success)?.data

    suspend fun downloadAttachmentBody(commentId: String, attachmentId: String): ResponseBody? =
        (repo.downloadAttachment(projectId, commentId, attachmentId) as? ApiResult.Success)?.data
}
