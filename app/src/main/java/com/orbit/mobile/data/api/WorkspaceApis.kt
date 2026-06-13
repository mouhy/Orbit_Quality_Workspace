package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.AddMemberRequest
import com.orbit.mobile.data.dto.AnalyzeTaskRequest
import com.orbit.mobile.data.dto.AuditLogDto
import com.orbit.mobile.data.dto.CreateProjectRequest
import com.orbit.mobile.data.dto.CreateTaskRequest
import com.orbit.mobile.data.dto.CreateTeamRequest
import com.orbit.mobile.data.dto.MembershipDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.QualityAnalysisDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.data.dto.UpdateProjectRequest
import com.orbit.mobile.data.dto.UpdateTeamRequest
import com.orbit.mobile.data.dto.UserAnalyticsDto
import com.orbit.mobile.data.dto.UserDashboardDto
import com.orbit.mobile.data.dto.UserDto
import kotlinx.serialization.json.JsonObject
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Projects endpoints
interface ProjectsApi {

    @GET("projects")
    suspend fun list(@Query("status") status: String? = null): List<ProjectDto>

    @POST("projects")
    suspend fun create(@Body body: CreateProjectRequest): JsonObject

    @PATCH("projects/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateProjectRequest): JsonObject

    @DELETE("projects/{id}")
    suspend fun delete(@Path("id") id: String)
}

// Teams endpoints
interface TeamsApi {

    @GET("teams")
    suspend fun list(): List<TeamDto>

    @GET("teams/{id}")
    suspend fun get(@Path("id") id: String): TeamDto

    @POST("teams")
    suspend fun create(@Body body: CreateTeamRequest): TeamDto

    @PUT("teams/{id}")
    suspend fun update(@Path("id") id: String, @Body body: UpdateTeamRequest): TeamDto

    @DELETE("teams/{id}")
    suspend fun delete(@Path("id") id: String)

    @GET("teams/{id}/members")
    suspend fun members(@Path("id") id: String): List<MembershipDto>

    @POST("teams/{id}/members")
    suspend fun addMember(@Path("id") id: String, @Body body: AddMemberRequest): MembershipDto

    @DELETE("teams/{id}/members/{userId}")
    suspend fun removeMember(@Path("id") id: String, @Path("userId") userId: String)
}

// Tasks endpoints
interface TasksApi {

    @POST("tasks")
    suspend fun create(@Body body: CreateTaskRequest): JsonObject
}

// QC endpoints
interface QcApi {

    @retrofit2.http.Multipart
    @POST("qc/tasks/{id}/analyze")
    suspend fun analyzeTask(
        @Path("id") taskId: String,
        @retrofit2.http.Part parts: List<okhttp3.MultipartBody.Part>
    ): QualityAnalysisDto
}

// Users endpoints
interface UsersApi {

    @GET("users")
    suspend fun list(@Query("role") role: String? = null): List<UserDto>
}

// System endpoints
interface SystemApi {

    @GET("system/audit-logs")
    suspend fun auditLogs(@Query("limit") limit: Int = 50): List<AuditLogDto>
}

// Analytics endpoints
interface AnalyticsApi {

    @POST("analytics/seed-demo")
    suspend fun seedDemo()

    @GET("analytics/user/{id}/dashboard")
    suspend fun userDashboard(@Path("id") userId: String): UserDashboardDto

    @GET("analytics/user/{id}")
    suspend fun userAnalytics(@Path("id") userId: String): UserAnalyticsDto

    @GET("analytics/platform-overview")
    suspend fun platformOverview(): com.orbit.mobile.data.dto.PlatformOverviewDto

    @GET("analytics/subadmin/dashboard")
    suspend fun subadminDashboard(): JsonObject
}

// Profile endpoints
interface ProfileApi {

    @GET("profile/{userId}")
    suspend fun profile(@Path("userId") userId: String): JsonObject

    @GET("profile/{userId}/portfolio")
    suspend fun portfolio(@Path("userId") userId: String): JsonObject
}
