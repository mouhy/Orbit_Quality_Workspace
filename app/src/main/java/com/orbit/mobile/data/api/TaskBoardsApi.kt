package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.AvailableMembersResponse
import com.orbit.mobile.data.dto.BoardDto
import com.orbit.mobile.data.dto.BoardMemberPatchRequest
import com.orbit.mobile.data.dto.BoardMemberRequest
import com.orbit.mobile.data.dto.BoardTaskCreateRequest
import com.orbit.mobile.data.dto.BoardTaskPatchRequest
import okhttp3.MultipartBody
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

// Board endpoints
interface TaskBoardsApi {

    @GET("task-boards/{pid}")
    suspend fun getBoard(@Path("pid") projectId: String): BoardDto

    @POST("task-boards/{pid}/todos")
    suspend fun addTodo(
        @Path("pid") projectId: String,
        @Body body: BoardTaskCreateRequest
    ): BoardDto

    @PATCH("task-boards/{pid}/todos/{tid}")
    suspend fun patchTodo(
        @Path("pid") projectId: String,
        @Path("tid") taskId: String,
        @Body body: BoardTaskPatchRequest
    ): BoardDto

    @DELETE("task-boards/{pid}/todos/{tid}")
    suspend fun deleteTodo(
        @Path("pid") projectId: String,
        @Path("tid") taskId: String
    ): BoardDto

    @POST("task-boards/{pid}/members")
    suspend fun addMember(
        @Path("pid") projectId: String,
        @Body body: BoardMemberRequest
    ): BoardDto

    @PATCH("task-boards/{pid}/members/{mid}")
    suspend fun patchMember(
        @Path("pid") projectId: String,
        @Path("mid") memberId: String,
        @Body body: BoardMemberPatchRequest
    ): BoardDto

    @GET("task-boards/{pid}/members/available")
    suspend fun availableMembers(
        @Path("pid") projectId: String,
        @Query("search") search: String? = null
    ): AvailableMembersResponse

    @Multipart
    @POST("task-boards/{pid}/comments")
    suspend fun addComment(
        @Path("pid") projectId: String,
        @Part parts: List<MultipartBody.Part>
    ): BoardDto

    @DELETE("task-boards/{pid}/comments/{cid}")
    suspend fun deleteComment(
        @Path("pid") projectId: String,
        @Path("cid") commentId: String
    ): BoardDto

    @Multipart
    @POST("task-boards/{pid}/resources")
    suspend fun uploadResource(
        @Path("pid") projectId: String,
        @Part file: MultipartBody.Part
    ): BoardDto

    @DELETE("task-boards/{pid}/resources/{rid}")
    suspend fun deleteResource(
        @Path("pid") projectId: String,
        @Path("rid") resourceId: String
    ): BoardDto

    @Streaming
    @GET("task-boards/{pid}/resources/{rid}")
    suspend fun downloadResource(
        @Path("pid") projectId: String,
        @Path("rid") resourceId: String
    ): ResponseBody

    @Streaming
    @GET("task-boards/{pid}/comments/{cid}/attachments/{aid}")
    suspend fun downloadAttachment(
        @Path("pid") projectId: String,
        @Path("cid") commentId: String,
        @Path("aid") attachmentId: String
    ): ResponseBody
}
