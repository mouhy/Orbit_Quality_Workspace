package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Board overview
@Serializable
data class BoardOverviewDto(
    val title: String = "",
    val description: String = "",
    @SerialName("status_badge") val statusBadge: String = "",
    val progress: Int = 0
)

// Board task
@Serializable
data class BoardTaskDto(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val assignee: String = "Unassigned",
    @SerialName("assignee_ids") val assigneeIds: List<String> = emptyList(),
    val due: String = "TBD",
    val done: Boolean = false,
    val status: String? = null,
    val priority: String? = null,
    val visibility: String? = "team",
    @SerialName("report_type") val reportType: String? = null,
    @SerialName("completed_by") val completedBy: List<String> = emptyList(),
    @SerialName("completed_by_names") val completedByNames: List<String> = emptyList(),
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("submitted_by") val submittedBy: String? = null,
    @SerialName("submitted_by_name") val submittedByName: String? = null
) {
    val statusKey: String get() = status ?: if (done) "done" else "todo"
}

// Comment attachment
@Serializable
data class BoardAttachmentDto(
    @SerialName("_id") val id: String,
    @SerialName("file_name") val fileName: String = "",
    val path: String? = null,
    val size: Long? = null,
    @SerialName("download_url") val downloadUrl: String? = null
)

// Board comment
@Serializable
data class BoardCommentDto(
    @SerialName("_id") val id: String,
    @SerialName("user_id") val userId: String? = null,
    @SerialName("user_name") val userName: String = "",
    @SerialName("user_avatar") val userAvatar: String = "",
    val message: String = "",
    @SerialName("created_at") val createdAt: String? = null,
    val attachments: List<BoardAttachmentDto> = emptyList()
)

// Board member
@Serializable
data class BoardMemberDto(
    @SerialName("user_id") val userId: String? = null,
    val name: String = "",
    val role: String = "",
    val avatar: String = "",
    val email: String? = null,
    val responsibility: String? = null,
    val online: Boolean = false
)

// Board resource
@Serializable
data class BoardResourceDto(
    @SerialName("_id") val id: String,
    @SerialName("file_name") val fileName: String = "",
    val path: String = "",
    @SerialName("uploaded_by") val uploadedBy: String = "",
    @SerialName("uploader_role") val uploaderRole: String = "",
    @SerialName("uploader_name") val uploaderName: String? = null,
    @SerialName("visible_to") val visibleTo: String? = "all",
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("download_url") val downloadUrl: String? = null
)

// Board payload
@Serializable
data class BoardDto(
    @SerialName("_id") val id: String? = null,
    @SerialName("project_id") val projectId: String = "",
    val overview: BoardOverviewDto = BoardOverviewDto(),
    val tasks: List<BoardTaskDto> = emptyList(),
    val members: List<BoardMemberDto> = emptyList(),
    val resources: List<BoardResourceDto> = emptyList(),
    val comments: List<BoardCommentDto> = emptyList(),
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// Create todo
@Serializable
data class BoardTaskCreateRequest(
    val title: String,
    val description: String? = null,
    val assignee: String = "Unassigned",
    @SerialName("assignee_ids") val assigneeIds: List<String> = emptyList(),
    val due: String = "TBD",
    val done: Boolean = false,
    val visibility: String = "team",
    @SerialName("report_type") val reportType: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    val status: String = "todo",
    val priority: String = "medium"
)

// Patch todo
@Serializable
data class BoardTaskPatchRequest(
    val title: String? = null,
    val description: String? = null,
    val assignee: String? = null,
    @SerialName("assignee_ids") val assigneeIds: List<String>? = null,
    val due: String? = null,
    val done: Boolean? = null,
    val visibility: String? = null,
    @SerialName("report_type") val reportType: String? = null,
    @SerialName("completed_by") val completedBy: List<String>? = null,
    @SerialName("completed_by_names") val completedByNames: List<String>? = null,
    val status: String? = null,
    val priority: String? = null
)

// Add member
@Serializable
data class BoardMemberRequest(
    val email: String? = null,
    @SerialName("user_id") val userId: String? = null
)

// Patch member
@Serializable
data class BoardMemberPatchRequest(
    val action: String,
    val responsibility: String? = null,
    val role: String? = null
)

// Available member
@Serializable
data class AvailableMemberDto(
    @SerialName("user_id") val userId: String,
    val name: String = "",
    val role: String = "",
    val avatar: String = "",
    val email: String? = null
)

// Available list
@Serializable
data class AvailableMembersResponse(
    val members: List<AvailableMemberDto> = emptyList()
)
