package com.orbit.mobile.data.dto

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

// Init status
@Serializable
data class InitStatusDto(
    val initialized: Boolean = true
)

// Login body
@Serializable
data class LoginRequestDto(
    val email: String,
    val password: String
)

// Setup body
@Serializable
data class SetupRequestDto(
    @SerialName("full_name") val fullName: String,
    val email: String,
    val password: String
)

// Auth user
@Serializable
data class AuthUserDto(
    @SerialName("_id") val id: String,
    val email: String? = null,
    val name: String? = null,
    val role: String? = null,
    val token: String? = null
)

// Status reply
@Serializable
data class StatusDto(
    val status: String? = null,
    val success: Boolean? = null
)
