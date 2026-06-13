package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Notification item
@Serializable
data class NotificationDto(
    @SerialName("_id") val id: String,
    val type: String? = null,
    val payload: Map<String, String>? = null,
    @SerialName("is_read") val isRead: Boolean = false,
    @SerialName("created_at") val createdAt: String? = null
)
