package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Current user's full profile (GET users/me)
@Serializable
data class MeDto(
    @SerialName("_id") val id: String = "",
    val email: String = "",
    val name: String = "",
    val role: String = "",
    val avatar: String? = null,
    val bio: String? = null,
    val department: String? = null,
    @SerialName("job_title") val jobTitle: String? = null,
    val country: String? = null,
    val timezone: String? = null
)

// Partial profile update; avatar carries a base64 data-URI
@Serializable
data class ProfileUpdateRequest(
    val name: String? = null,
    val avatar: String? = null,
    val bio: String? = null,
    val department: String? = null,
    @SerialName("job_title") val jobTitle: String? = null,
    val country: String? = null,
    val timezone: String? = null
)
