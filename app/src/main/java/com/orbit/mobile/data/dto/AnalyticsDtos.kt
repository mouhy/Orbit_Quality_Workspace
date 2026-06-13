package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Status counts
@Serializable
data class StatusBreakdownDto(
    val todo: Int = 0,
    val inProgress: Int = 0,
    val review: Int = 0,
    val done: Int = 0
)

// Task lite
@Serializable
data class TaskLiteDto(
    val id: String = "",
    val title: String = "",
    val status: String = "TODO",
    val priority: String = "medium",
    val deadline: String? = null,
    @SerialName("project_name") val projectName: String? = null
)

// User dashboard
@Serializable
data class UserDashboardDto(
    @SerialName("user_id") val userId: String? = null,
    val onTimeRate: Double = 0.0,
    val onTimeTasks: Int = 0,
    val totalWithDeadline: Int = 0,
    val totalCompleted: Int = 0,
    val activeTasks: Int = 0,
    val statusBreakdown: StatusBreakdownDto = StatusBreakdownDto(),
    val totalTasks: Int = 0,
    val averageCompletionTime: Double? = null,
    val activeTasksList: List<TaskLiteDto> = emptyList(),
    val rejectionRate: Double? = null,
    val burnoutRisk: String? = null
)
