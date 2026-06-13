package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Report type
@Serializable
data class ReportTypeDto(
    val key: String,
    @SerialName("name_ar") val nameAr: String = "",
    @SerialName("name_en") val nameEn: String = "",
    val description: String = "",
    @SerialName("required_elements") val requiredElements: List<String> = emptyList()
)

// Evaluate file
@Serializable
data class EvaluateFileDto(
    @SerialName("file_name") val fileName: String,
    val content: String,
    @SerialName("file_type") val fileType: String
)

// Evaluate request
@Serializable
data class EvaluateRequest(
    @SerialName("task_title") val taskTitle: String,
    @SerialName("task_description") val taskDescription: String = "",
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("report_type") val reportType: String? = null,
    val files: List<EvaluateFileDto> = emptyList(),
    @SerialName("image_base64") val imageBase64: List<String> = emptyList(),
    @SerialName("submission_notes") val submissionNotes: String? = null
)

// Passed standard
@Serializable
data class PassedStandardDto(
    val rule: String = "",
    val result: String? = null
)

// Failed standard
@Serializable
data class FailedStandardDto(
    val rule: String = "",
    val reason: String? = null
)

// Type compliance
@Serializable
data class ReportTypeComplianceDto(
    @SerialName("is_compliant") val isCompliant: Boolean? = null,
    @SerialName("missing_elements") val missingElements: List<String> = emptyList(),
    @SerialName("compliance_note") val complianceNote: String? = null
)

// Evaluation result
@Serializable
data class EvaluationResultDto(
    @SerialName("compliance_score") val complianceScore: Double = 0.0,
    @SerialName("passed_standards") val passedStandards: List<PassedStandardDto> = emptyList(),
    @SerialName("failed_standards") val failedStandards: List<FailedStandardDto> = emptyList(),
    val suggestions: List<String> = emptyList(),
    @SerialName("report_type_key") val reportTypeKey: String? = null,
    @SerialName("report_type_name_ar") val reportTypeNameAr: String? = null,
    @SerialName("report_type_name_en") val reportTypeNameEn: String? = null,
    @SerialName("report_type_compliance")
    val reportTypeCompliance: ReportTypeComplianceDto? = null,
    @SerialName("evaluation_id") val evaluationId: String? = null
)

// History item
@Serializable
data class AiHistoryItemDto(
    val taskId: String = "",
    val taskTitle: String = "",
    val score: Double = 0.0,
    val status: String = "",
    val timestamp: String? = null
)

// Task detail
@Serializable
data class TaskDetailDto(
    @SerialName("_id") val id: String,
    val title: String = "",
    val description: String = "",
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("team_id") val teamId: String? = null,
    @SerialName("assigned_to") val assignedTo: String? = null,
    val assignees: List<String> = emptyList(),
    val visibility: String = "team",
    @SerialName("created_by") val createdBy: String = "",
    val status: String = "",
    val priority: String = "",
    val deadline: String? = null,
    @SerialName("report_type") val reportType: String? = null,
    @SerialName("creator_name") val creatorName: String? = null,
    @SerialName("created_at") val createdAt: String? = null,
    @SerialName("updated_at") val updatedAt: String? = null,
    @SerialName("assigned_to_name") val assignedToName: String? = null,
    @SerialName("assignees_names") val assigneesNames: List<String> = emptyList()
)
