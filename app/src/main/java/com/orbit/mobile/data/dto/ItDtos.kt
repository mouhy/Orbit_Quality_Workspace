package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Register payload sent by IT/Admin when creating accounts
@Serializable
data class RegisterUserRequest(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val password: String,
    val role: String = "staff",
    val status: String = "active"
)

// Partial user update — explicitNulls=false drops null fields from the body
@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val role: String? = null,
    val status: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null
)

// Admin password reset uses the same change-password endpoint with a target user_id
@Serializable
data class AdminPasswordResetRequest(
    @SerialName("user_id") val userId: String,
    @SerialName("new_password") val newPassword: String
)

// Single login session row from users/sessions
@Serializable
data class SessionLogDto(
    @SerialName("_id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    @SerialName("login_time") val loginTime: String? = null,
    @SerialName("last_active") val lastActive: String? = null,
    @SerialName("duration_minutes") val durationMinutes: Double = 0.0,
    @SerialName("user_agent") val userAgent: String? = null,
    @SerialName("ip_address") val ipAddress: String? = null
)

// System health snapshot for the IT dashboard
@Serializable
data class SystemHealthDto(
    val status: String = "",
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("active_sessions") val activeSessions: Int = 0,
    @SerialName("error_rate") val errorRate: String = "",
    @SerialName("storage_usage_mb") val storageUsageMb: Int = 0,
    @SerialName("maintenance_mode") val maintenanceMode: Boolean = false,
    @SerialName("max_upload_size_mb") val maxUploadSizeMb: Int = 5
)

// Global settings patch — backend accepts a partial dict
@Serializable
data class SettingsUpdateRequest(
    @SerialName("maintenance_mode") val maintenanceMode: Boolean? = null,
    @SerialName("max_upload_size") val maxUploadSize: Int? = null
)
