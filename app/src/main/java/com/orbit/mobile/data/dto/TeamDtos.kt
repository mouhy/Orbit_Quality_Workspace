package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonPrimitive

// Team item
@Serializable
data class TeamDto(
    val id: String? = null,
    @SerialName("_id") val mongoId: String? = null,
    val name: String = "",
    val description: String? = null,
    @SerialName("created_by") val createdBy: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    val memberCount: Int? = null,
    @SerialName("user_role") val userRole: String? = null,
    @SerialName("manager_name") val managerName: String? = null,
    @SerialName("active_project_count") val activeProjectCount: Int? = null,
    val status: String? = null
) {
    val teamId: String get() = id ?: mongoId ?: ""
}

// Membership item
@Serializable
data class MembershipDto(
    val id: String? = null,
    @SerialName("_id") val mongoId: String? = null,
    @SerialName("user_id") val userId: String = "",
    @SerialName("team_id") val teamId: String = "",
    val role: String = "member",
    @SerialName("managed_by") val managedBy: String? = null,
    @SerialName("joined_at") val joinedAt: String? = null,
    @SerialName("user_email") val userEmail: String? = null,
    @SerialName("user_full_name") val userFullName: String? = null
)

// Create team
@Serializable
data class CreateTeamRequest(
    val name: String,
    val description: String = "",
    val managerIds: List<String> = emptyList(),
    val subManagerIds: List<String> = emptyList(),
    val staffIds: List<String> = emptyList()
)

// Update team
@Serializable
data class UpdateTeamRequest(
    val name: String,
    val description: String
)

// Add member
@Serializable
data class AddMemberRequest(
    @SerialName("user_id") val userId: String,
    val role: String
)

// Create project
@Serializable
data class CreateProjectRequest(
    val title: String,
    val description: String = "",
    val status: String = "ACTIVE",
    val progress: Int = 0,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("sub_admin_ids") val subAdminIds: List<String> = emptyList(),
    @SerialName("staff_ids") val staffIds: List<String> = emptyList(),
    @SerialName("team_id") val teamId: String? = null
)

// Update project
@Serializable
data class UpdateProjectRequest(
    val title: String? = null,
    val description: String? = null,
    val status: String? = null,
    val progress: Int? = null,
    @SerialName("due_date") val dueDate: String? = null,
    @SerialName("sub_admin_ids") val subAdminIds: List<String>? = null,
    @SerialName("staff_ids") val staffIds: List<String>? = null
)

// Create task
@Serializable
data class CreateTaskRequest(
    val title: String,
    val description: String = "",
    @SerialName("team_id") val teamId: String,
    @SerialName("assigned_to") val assignedTo: String,
    val assignees: List<String>,
    val visibility: String = "team",
    val priority: String = "medium",
    val status: String = "todo",
    @SerialName("report_type") val reportType: String? = null
)

// User analytics
@Serializable
data class UserAnalyticsDto(
    @SerialName("tasks_total") val tasksTotal: Int = 0,
    @SerialName("tasks_done") val tasksDone: Int = 0,
    @SerialName("tasks_in_progress") val tasksInProgress: Int = 0,
    @SerialName("tasks_in_review") val tasksInReview: Int = 0,
    @SerialName("avg_accuracy") val avgAccuracy: Double = 0.0
)

// QC analyze
@Serializable
data class AnalyzeTaskRequest(
    @SerialName("task_title") val taskTitle: String,
    @SerialName("task_description") val taskDescription: String,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("project_id") val projectId: String? = null
)

// Analysis result
@Serializable
data class QualityAnalysisDto(
    @SerialName("compliance_score") val complianceScore: Double = 0.0,
    @SerialName("passed_standards") val passedStandards: List<JsonElement> = emptyList(),
    @SerialName("failed_standards") val failedStandards: List<JsonElement> = emptyList(),
    val suggestions: List<String> = emptyList(),
    val verdict: String? = null,
    val summary: String? = null
)

// Standard text
fun standardText(element: JsonElement): String = when (element) {
    is JsonPrimitive -> element.content
    is JsonObject -> listOfNotNull(
        runCatching { element["rule"]?.jsonPrimitive?.content }.getOrNull(),
        runCatching { element["reason"]?.jsonPrimitive?.content }.getOrNull()
            ?: runCatching { element["result"]?.jsonPrimitive?.content }.getOrNull()
    ).joinToString(" — ").ifBlank { element.toString() }
    else -> element.toString()
}
