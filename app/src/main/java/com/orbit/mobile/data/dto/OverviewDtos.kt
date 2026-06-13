package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Platform KPIs
@Serializable
data class PlatformKpisDto(
    @SerialName("total_projects") val totalProjects: Int = 0,
    @SerialName("active_projects") val activeProjects: Int = 0,
    @SerialName("completed_projects") val completedProjects: Int = 0,
    @SerialName("on_hold_projects") val onHoldProjects: Int = 0,
    @SerialName("overdue_projects") val overdueProjects: Int = 0,
    @SerialName("total_tasks") val totalTasks: Int = 0,
    @SerialName("done_tasks") val doneTasks: Int = 0,
    @SerialName("overdue_tasks") val overdueTasks: Int = 0,
    @SerialName("tasks_due_today") val tasksDueToday: Int = 0,
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("online_users") val onlineUsers: Int = 0,
    @SerialName("total_comments") val totalComments: Int = 0,
    @SerialName("total_files") val totalFiles: Int = 0,
    @SerialName("total_events") val totalEvents: Int = 0,
    @SerialName("quality_reviews") val qualityReviews: Int = 0,
    @SerialName("quality_pass_rate") val qualityPassRate: Double = 0.0,
    @SerialName("avg_quality_score") val avgQualityScore: Double = 0.0
)

// Overview project
@Serializable
data class OverviewProjectDto(
    val id: String = "",
    val title: String = "",
    val status: String = "ACTIVE",
    val progress: Int = 0,
    @SerialName("health_label") val healthLabel: String? = null,
    @SerialName("health_color") val healthColor: String? = null,
    @SerialName("task_count") val taskCount: Int = 0,
    @SerialName("done_tasks") val doneTasks: Int = 0,
    @SerialName("ai_score") val aiScore: Double? = null,
    @SerialName("due_date") val dueDate: String? = null,
    val priority: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null
)

// Platform overview
@Serializable
data class PlatformOverviewDto(
    val kpis: PlatformKpisDto = PlatformKpisDto(),
    val projects: List<OverviewProjectDto> = emptyList()
)
