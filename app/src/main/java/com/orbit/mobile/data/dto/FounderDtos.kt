package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Revenue block inside founder metrics
@Serializable
data class FinancialsDto(
    val mrr: Double = 0.0,
    @SerialName("tier_distribution") val tierDistribution: Map<String, Int> = emptyMap()
)

// One day of AI accept/reject volume
@Serializable
data class AiTrendDto(
    val day: String = "",
    val accepted: Int = 0,
    val rejected: Int = 0
)

// Operational alert raised by the platform
@Serializable
data class SystemAlertDto(
    val id: String? = null,
    val type: String = "info",
    val message: String = "",
    val time: String? = null
)

// Full founder command-center payload
@Serializable
data class FounderMetricsDto(
    // Health string
    @SerialName("platform_health") val platformHealth: String = "",
    @SerialName("total_entities") val totalEntities: Int = 0,
    @SerialName("total_teams") val totalTeams: Int = 0,
    @SerialName("total_users") val totalUsers: Int = 0,
    @SerialName("active_now") val activeNow: Int = 0,
    @SerialName("pending_ai_evals") val pendingAiEvals: Int = 0,
    @SerialName("avg_quality_score") val avgQualityScore: Double = 0.0,
    @SerialName("total_reports") val totalReports: Int = 0,
    @SerialName("pass_rate") val passRate: Double = 0.0,
    val financials: FinancialsDto = FinancialsDto(),
    @SerialName("ai_trends") val aiTrends: List<AiTrendDto> = emptyList(),
    @SerialName("system_alerts") val systemAlerts: List<SystemAlertDto> = emptyList(),
    @SerialName("total_tasks_processed") val totalTasksProcessed: Int = 0
) {
    // Health number
    val platformHealthScore: Int
        get() = platformHealth.takeWhile { it.isDigit() }.toIntOrNull() ?: 0
}

// IT account row managed by the founder
@Serializable
data class FounderAccountDto(
    @SerialName("_id") val id: String,
    val name: String = "",
    val email: String = "",
    val role: String = "it_staff",
    val status: String? = null,
    @SerialName("is_active") val isActive: Boolean? = null,
    @SerialName("created_at") val createdAt: String? = null
)

// Create a new IT staff account
@Serializable
data class FounderAccountCreateRequest(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val password: String
)

// Rename / re-email an existing IT account
@Serializable
data class FounderAccountPatchRequest(
    @SerialName("full_name") val fullName: String? = null,
    val email: String? = null
)

// Password reset payload for founder accounts
@Serializable
data class NewPasswordRequest(
    @SerialName("new_password") val newPassword: String
)

// Quality framework definition (prompt + threshold)
@Serializable
data class FrameworkDto(
    @SerialName("_id") val id: String = "",
    val name: String = "",
    val description: String? = "",
    @SerialName("ai_prompt_template") val aiPromptTemplate: String? = "",
    @SerialName("acceptance_threshold") val acceptanceThreshold: Int? = 70,
    @SerialName("is_active") val isActive: Boolean? = true
)

// Create/update payload for frameworks
@Serializable
data class FrameworkRequest(
    val name: String,
    val description: String = "",
    @SerialName("ai_prompt_template") val aiPromptTemplate: String,
    @SerialName("acceptance_threshold") val acceptanceThreshold: Int = 70,
    @SerialName("is_active") val isActive: Boolean = true
)

// Tenant entity managed by the founder
@Serializable
data class EntityDto(
    @SerialName("_id") val id: String = "",
    val name: String = "",
    val description: String? = "",
    @SerialName("it_staff_ids") val itStaffIds: List<String> = emptyList(),
    @SerialName("subscription_tier") val subscriptionTier: String? = "Basic",
    @SerialName("max_teams") val maxTeams: Int? = 5,
    @SerialName("user_count") val userCount: Int? = 0,
    val status: String? = "active"
)

// Create payload for entities
@Serializable
data class EntityCreateRequest(
    val name: String,
    val description: String = "",
    @SerialName("subscription_tier") val subscriptionTier: String = "Basic"
)

// Assign IT staff to an entity
@Serializable
data class AssignItRequest(
    @SerialName("it_staff_ids") val itStaffIds: List<String>
)
