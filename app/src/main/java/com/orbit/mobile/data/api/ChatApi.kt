package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.ChatRequestDto
import com.orbit.mobile.data.dto.ChatResponseDto
import retrofit2.http.Body
import retrofit2.http.POST

// Chatbot endpoint
interface ChatApi {

    @POST("chat")
    suspend fun send(@Body body: ChatRequestDto): ChatResponseDto
}
