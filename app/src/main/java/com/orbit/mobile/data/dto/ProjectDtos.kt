package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Person ref
@Serializable
data class PersonDto(
    @SerialName("_id") val id: String? = null,
    val name: String? = null,
    val email: String? = null,
    val initials: String? = null
)

// Project item
@Serializable
data class ProjectDto(
    val id: String? = null,
    @SerialName("_id") val mongoId: String? = null,
    val title: String = "",
    val description: String? = null,
    val status: String = "ACTIVE",
    val progress: Int = 0,
    @SerialName("owner_id") val ownerId: String? = null,
    @SerialName("team_id") val teamId: String? = null,
    @SerialName("comments_enabled") val commentsEnabled: Boolean = true,
    @SerialName("uploads_enabled") val uploadsEnabled: Boolean = true,
    @SerialName("is_system_card") val isSystemCard: Boolean = false,
    @SerialName("due_date") val dueDate: String? = null,
    val priority: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("sub_admins") val subAdmins: List<PersonDto> = emptyList(),
    val staff: List<PersonDto> = emptyList(),
    @SerialName("staff_initials") val staffInitials: List<String> = emptyList()
) {
    // Effective id
    val projectId: String get() = id ?: mongoId ?: ""

    // System channel
    val isSystem: Boolean
        get() = isSystemCard || projectId == "public-group" || projectId == "all-sub-admin" ||
            title.lowercase().trim() == "public" ||
            title.lowercase().replace(Regex("[\\s_]"), "") == "allsubadmin"
}

// User item
@Serializable
data class UserDto(
    @SerialName("_id") val id: String,
    val name: String = "",
    val email: String = "",
    val role: String = "staff",
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("last_seen") val lastSeen: String? = null,
    val status: String? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// Audit log
@Serializable
data class AuditLogDto(
    @SerialName("_id") val id: String? = null,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("action_type") val actionType: String? = null,
    val action: String? = null,
    @SerialName("entity_type") val entityType: String? = null,
    val timestamp: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("user_name") val userName: String? = null
) {
    val actionLabel: String get() = (actionType ?: action ?: "").replace("_", " ").lowercase()
    val time: String? get() = createdAt ?: timestamp
}
