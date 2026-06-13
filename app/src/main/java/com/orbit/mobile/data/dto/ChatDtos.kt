package com.orbit.mobile.data.dto

import kotlinx.serialization.Serializable

// Chat request
@Serializable
data class ChatRequestDto(val message: String)

// Chat response
@Serializable
data class ChatResponseDto(val reply: String = "")
