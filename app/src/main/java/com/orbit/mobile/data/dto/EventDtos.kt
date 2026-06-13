package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Event item
@Serializable
data class EventDto(
    val id: String,
    val title: String = "",
    val description: String? = null,
    val type: String = "meeting",
    @SerialName("start_date") val startDate: String = "",
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val priority: String = "medium",
    val location: String? = null,
    @SerialName("meeting_link") val meetingLink: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("owner_id") val ownerId: String = "",
    @SerialName("attendee_ids") val attendeeIds: List<String> = emptyList(),
    val visibility: String = "workspace",
    @SerialName("reminder_minutes") val reminderMinutes: Int? = null,
    val recurrence: String = "none",
    val color: String? = null,
    val status: String = "active",
    @SerialName("created_by") val createdBy: String = ""
)

// Create event
@Serializable
data class EventCreateRequest(
    val title: String,
    val description: String? = null,
    val type: String = "meeting",
    @SerialName("start_date") val startDate: String,
    @SerialName("end_date") val endDate: String? = null,
    @SerialName("start_time") val startTime: String? = null,
    @SerialName("end_time") val endTime: String? = null,
    val priority: String = "medium",
    val location: String? = null,
    @SerialName("meeting_link") val meetingLink: String? = null,
    @SerialName("project_id") val projectId: String? = null,
    @SerialName("task_id") val taskId: String? = null,
    @SerialName("attendee_ids") val attendeeIds: List<String> = emptyList(),
    val visibility: String = "workspace",
    @SerialName("reminder_minutes") val reminderMinutes: Int? = null,
    val recurrence: String = "none",
    val color: String? = null
)

// RSVP body
@Serializable
data class RsvpRequest(val status: String)
