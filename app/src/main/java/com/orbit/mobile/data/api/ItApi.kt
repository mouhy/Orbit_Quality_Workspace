package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.AdminPasswordResetRequest
import com.orbit.mobile.data.dto.AuthUserDto
import com.orbit.mobile.data.dto.RegisterUserRequest
import com.orbit.mobile.data.dto.SessionLogDto
import com.orbit.mobile.data.dto.SettingsUpdateRequest
import com.orbit.mobile.data.dto.SystemHealthDto
import com.orbit.mobile.data.dto.UpdateUserRequest
import kotlinx.serialization.json.JsonElement
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

// IT console endpoints: user management, sessions, system settings, credentials
interface ItApi {

    // Create a platform account on behalf of IT/Founder
    @POST("auth/register")
    suspend fun register(@Body body: RegisterUserRequest): AuthUserDto

    // Partial account edit (admin only)
    @PATCH("users/{id}")
    suspend fun updateUser(@Path("id") id: String, @Body body: UpdateUserRequest): JsonElement

    // Permanent account removal (admin / it_staff)
    @DELETE("users/{id}")
    suspend fun deleteUser(@Path("id") id: String)

    // Admin reset of another user's password
    @POST("auth/change-password")
    suspend fun resetPassword(@Body body: AdminPasswordResetRequest): JsonElement

    // All workspace sessions, newest first
    @GET("users/sessions")
    suspend fun sessions(@Query("limit") limit: Int = 50): List<SessionLogDto>

    // Current user's own sessions (profile section)
    @GET("users/me/sessions")
    suspend fun mySessions(): List<SessionLogDto>

    // Current user's recent activity feed
    @GET("users/me/activity")
    suspend fun myActivity(): JsonElement

    // Own profile fetch + edit + avatar removal (Settings page)
    @GET("users/me")
    suspend fun me(): com.orbit.mobile.data.dto.MeDto

    @PATCH("users/me/profile")
    suspend fun updateProfile(
        @Body body: com.orbit.mobile.data.dto.ProfileUpdateRequest
    ): com.orbit.mobile.data.dto.MeDto

    @DELETE("users/me/avatar")
    suspend fun deleteAvatar()

    // Health stats + current settings snapshot
    @GET("system/health")
    suspend fun health(): SystemHealthDto

    // Toggle maintenance / max upload size
    @PATCH("system/settings")
    suspend fun updateSettings(@Body body: SettingsUpdateRequest): JsonElement

    // Suspend one account and end its sessions
    @POST("system/force-logout/{userId}")
    suspend fun forceLogout(@Path("userId") userId: String): JsonElement

    // Emergency: enable maintenance, logging everyone out
    @POST("system/logout-all")
    suspend fun logoutAll(): JsonElement

    // Plain-text credentials registry (founder + it_staff)
    @GET("founder/credentials")
    suspend fun credentials(): ResponseBody

    // Same registry as a downloadable file
    @Streaming
    @GET("founder/credentials/download")
    suspend fun credentialsDownload(): ResponseBody
}
