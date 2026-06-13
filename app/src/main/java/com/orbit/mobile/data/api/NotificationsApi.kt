package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.NotificationDto
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.Path

// Notification endpoints
interface NotificationsApi {

    @GET("notifications")
    suspend fun list(): List<NotificationDto>

    @PATCH("notifications/{id}/read")
    suspend fun markRead(@Path("id") id: String)

    @PATCH("notifications/read-all")
    suspend fun markAllRead()
}
