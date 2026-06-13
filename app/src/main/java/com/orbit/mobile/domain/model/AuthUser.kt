package com.orbit.mobile.domain.model

// Logged user
data class AuthUser(
    val id: String,
    val email: String,
    val name: String,
    val role: String,
    val token: String
)
