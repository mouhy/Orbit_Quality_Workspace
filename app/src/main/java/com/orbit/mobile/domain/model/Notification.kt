package com.orbit.mobile.domain.model

// Notification kinds
enum class NotificationType { INFO, SUCCESS, WARNING, ERROR, MENTION;

    companion object {
        // Parse backend
        fun from(value: String?): NotificationType = when (value) {
            "success" -> SUCCESS
            "warning" -> WARNING
            "error" -> ERROR
            "mention" -> MENTION
            else -> INFO
        }
    }
}

// Notification model
data class Notification(
    val id: String,
    val type: NotificationType,
    val title: String,
    val body: String,
    val isRead: Boolean,
    val createdAt: String?,
    // Routing refs
    val projectId: String? = null,
    val taskId: String? = null,
    val teamId: String? = null
)
