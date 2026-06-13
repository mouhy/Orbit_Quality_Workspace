package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Rule item
@Serializable
data class RuleDto(
    @SerialName("rule_id") val ruleId: String? = null,
    val label: String = "",
    val instructions: String? = null,
    val weight: Double = 1.0
)

// Standard item
@Serializable
data class StandardDto(
    @SerialName("_id") val mongoId: String? = null,
    val id: String? = null,
    val title: String = "",
    val description: String? = null,
    val type: String = "text",
    val rules: List<RuleDto> = emptyList(),
    val status: String = "active",
    @SerialName("created_at") val createdAt: String? = null
) {
    val standardId: String get() = id ?: mongoId ?: ""
}

// Standard payload
@Serializable
data class StandardCreateRequest(
    val title: String,
    val description: String? = null,
    val type: String = "text",
    val rules: List<RuleDto> = emptyList(),
    val status: String = "active"
)

// Trend point
@Serializable
data class ScoreTrendDto(
    val date: String = "",
    val avgScore: Double = 0.0,
    val evaluations: Int = 0
)

// Bottleneck item
@Serializable
data class BottleneckDto(
    val name: String = "",
    val avgHours: Double = 0.0
)

// Project score
@Serializable
data class ProjectScoreDto(
    val name: String? = null,
    val projectName: String? = null,
    val avgScore: Double? = null,
    val score: Double? = null,
    val evaluations: Int? = null
) {
    val displayName: String get() = name ?: projectName ?: ""
    val displayScore: Double get() = avgScore ?: score ?: 0.0
}

// QC overview
@Serializable
data class QcOverviewDto(
    val scoreTrend: List<ScoreTrendDto> = emptyList(),
    val projectScores: List<ProjectScoreDto> = emptyList(),
    val bottlenecks: List<BottleneckDto> = emptyList()
)

// Evaluation item
@Serializable
data class EvaluationItemDto(
    @SerialName("_id") val id: String = "",
    @SerialName("task_title") val taskTitle: String = "",
    @SerialName("compliance_score") val complianceScore: Double = 0.0,
    @SerialName("report_type") val reportType: String? = null,
    @SerialName("rules_count") val rulesCount: Int = 0,
    @SerialName("created_at") val createdAt: String? = null
)
