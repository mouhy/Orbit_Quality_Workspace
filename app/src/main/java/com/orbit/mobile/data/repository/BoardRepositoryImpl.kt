package com.orbit.mobile.data.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.data.api.TaskBoardsApi
import com.orbit.mobile.data.dto.AvailableMemberDto
import com.orbit.mobile.data.dto.BoardDto
import com.orbit.mobile.data.dto.BoardMemberPatchRequest
import com.orbit.mobile.data.dto.BoardMemberRequest
import com.orbit.mobile.data.dto.BoardTaskCreateRequest
import com.orbit.mobile.data.dto.BoardTaskPatchRequest
import com.orbit.mobile.core.network.map
import com.orbit.mobile.domain.repository.BoardRepository
import com.orbit.mobile.domain.repository.ReplyMeta
import com.orbit.mobile.domain.repository.UploadPart
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

// Board impl
@Singleton
class BoardRepositoryImpl @Inject constructor(
    private val api: TaskBoardsApi
) : BoardRepository {

    override suspend fun board(projectId: String): ApiResult<BoardDto> =
        safeApiCall { api.getBoard(projectId) }

    override suspend fun addTodo(
        projectId: String,
        body: BoardTaskCreateRequest
    ): ApiResult<BoardDto> = safeApiCall { api.addTodo(projectId, body) }

    override suspend fun patchTodo(
        projectId: String,
        taskId: String,
        body: BoardTaskPatchRequest
    ): ApiResult<BoardDto> = safeApiCall { api.patchTodo(projectId, taskId, body) }

    override suspend fun deleteTodo(projectId: String, taskId: String): ApiResult<BoardDto> =
        safeApiCall { api.deleteTodo(projectId, taskId) }

    override suspend fun addMember(projectId: String, userId: String): ApiResult<BoardDto> =
        safeApiCall { api.addMember(projectId, BoardMemberRequest(userId = userId)) }

    override suspend fun patchMember(
        projectId: String,
        memberId: String,
        body: BoardMemberPatchRequest
    ): ApiResult<BoardDto> = safeApiCall { api.patchMember(projectId, memberId, body) }

    override suspend fun availableMembers(
        projectId: String,
        search: String?
    ): ApiResult<List<AvailableMemberDto>> =
        safeApiCall { api.availableMembers(projectId, search) }.map { it.members }

    override suspend fun sendComment(
        projectId: String,
        message: String?,
        attachments: List<UploadPart>,
        mentionedUserIds: List<String>,
        replyTo: ReplyMeta?
    ): ApiResult<BoardDto> = safeApiCall {
        val parts = mutableListOf<MultipartBody.Part>()
        if (!message.isNullOrBlank()) {
            parts += MultipartBody.Part.createFormData("message", message)
        }
        mentionedUserIds.forEach {
            parts += MultipartBody.Part.createFormData("mentioned_user_ids", it)
        }
        replyTo?.let {
            parts += MultipartBody.Part.createFormData("reply_to_id", it.id)
            parts += MultipartBody.Part.createFormData("reply_to_preview", it.preview)
            parts += MultipartBody.Part.createFormData("reply_to_author", it.author)
        }
        attachments.forEach { file ->
            parts += MultipartBody.Part.createFormData(
                "attachments",
                file.fileName,
                file.bytes.toRequestBody(file.mimeType.toMediaTypeOrNull())
            )
        }
        api.addComment(projectId, parts)
    }

    override suspend fun deleteComment(projectId: String, commentId: String): ApiResult<BoardDto> =
        safeApiCall { api.deleteComment(projectId, commentId) }

    override suspend fun uploadResource(
        projectId: String,
        file: UploadPart
    ): ApiResult<BoardDto> = safeApiCall {
        val part = MultipartBody.Part.createFormData(
            "file",
            file.fileName,
            file.bytes.toRequestBody(file.mimeType.toMediaTypeOrNull())
        )
        api.uploadResource(projectId, part)
    }

    override suspend fun deleteResource(
        projectId: String,
        resourceId: String
    ): ApiResult<BoardDto> = safeApiCall { api.deleteResource(projectId, resourceId) }

    override suspend fun downloadResource(
        projectId: String,
        resourceId: String
    ): ApiResult<ResponseBody> = safeApiCall { api.downloadResource(projectId, resourceId) }

    override suspend fun downloadAttachment(
        projectId: String,
        commentId: String,
        attachmentId: String
    ): ApiResult<ResponseBody> =
        safeApiCall { api.downloadAttachment(projectId, commentId, attachmentId) }
}
