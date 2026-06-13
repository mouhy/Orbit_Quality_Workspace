package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.AiHistoryItemDto
import com.orbit.mobile.data.dto.EvaluateRequest
import com.orbit.mobile.data.dto.EvaluationResultDto
import com.orbit.mobile.data.dto.ReportTypeDto
import com.orbit.mobile.data.dto.TaskDetailDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

// Quality endpoints
interface QualityApi {

    @GET("quality/report-types")
    suspend fun reportTypes(): List<ReportTypeDto>

    @POST("quality/evaluate")
    suspend fun evaluate(@Body body: EvaluateRequest): EvaluationResultDto

    @GET("ai/history")
    suspend fun history(): List<AiHistoryItemDto>

    @Streaming
    @GET("quality/templates/{key}")
    suspend fun downloadTemplate(
        @Path("key") reportTypeKey: String,
        @Query("lang") lang: String = "ar"
    ): ResponseBody

    @GET("tasks/{id}")
    suspend fun task(@Path("id") taskId: String): TaskDetailDto
}
