package com.orbit.mobile.domain.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.data.dto.AvailableMemberDto
import com.orbit.mobile.data.dto.BoardDto
import com.orbit.mobile.data.dto.BoardMemberPatchRequest
import com.orbit.mobile.data.dto.BoardTaskCreateRequest
import com.orbit.mobile.data.dto.BoardTaskPatchRequest
import okhttp3.ResponseBody

// Upload file
data class UploadPart(
    val fileName: String,
    val bytes: ByteArray,
    val mimeType: String = "application/octet-stream"
)

// Reply meta
data class ReplyMeta(
    val id: String,
    val preview: String,
    val author: String
)

// Board contract
interface BoardRepository {

    suspend fun board(projectId: String): ApiResult<BoardDto>

    suspend fun addTodo(projectId: String, body: BoardTaskCreateRequest): ApiResult<BoardDto>

    suspend fun patchTodo(
        projectId: String,
        taskId: String,
        body: BoardTaskPatchRequest
    ): ApiResult<BoardDto>

    suspend fun deleteTodo(projectId: String, taskId: String): ApiResult<BoardDto>

    suspend fun addMember(projectId: String, userId: String): ApiResult<BoardDto>

    suspend fun patchMember(
        projectId: String,
        memberId: String,
        body: BoardMemberPatchRequest
    ): ApiResult<BoardDto>

    suspend fun availableMembers(
        projectId: String,
        search: String? = null
    ): ApiResult<List<AvailableMemberDto>>

    suspend fun sendComment(
        projectId: String,
        message: String?,
        attachments: List<UploadPart> = emptyList(),
        mentionedUserIds: List<String> = emptyList(),
        replyTo: ReplyMeta? = null
    ): ApiResult<BoardDto>

    suspend fun deleteComment(projectId: String, commentId: String): ApiResult<BoardDto>

    suspend fun uploadResource(projectId: String, file: UploadPart): ApiResult<BoardDto>

    suspend fun deleteResource(projectId: String, resourceId: String): ApiResult<BoardDto>

    suspend fun downloadResource(projectId: String, resourceId: String): ApiResult<ResponseBody>

    suspend fun downloadAttachment(
        projectId: String,
        commentId: String,
        attachmentId: String
    ): ApiResult<ResponseBody>
}
