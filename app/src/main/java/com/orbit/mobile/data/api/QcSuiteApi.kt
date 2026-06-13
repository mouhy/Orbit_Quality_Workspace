package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.EvaluationItemDto
import com.orbit.mobile.data.dto.QcOverviewDto
import com.orbit.mobile.data.dto.StandardCreateRequest
import com.orbit.mobile.data.dto.StandardDto
import okhttp3.ResponseBody
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query
import retrofit2.http.Streaming

// QC suite
interface QcSuiteApi {

    @GET("qc/standards")
    suspend fun standards(
        @Query("status") status: String? = "all"
    ): List<StandardDto>

    @POST("qc/standards")
    suspend fun createStandard(@Body body: StandardCreateRequest): StandardDto

    @PUT("qc/standards/{id}")
    suspend fun updateStandard(
        @Path("id") id: String,
        @Body body: StandardCreateRequest
    ): StandardDto

    @DELETE("qc/standards/{id}")
    suspend fun archiveStandard(@Path("id") id: String)

    @GET("qc/reports/overview")
    suspend fun overview(
        @Query("days") days: Int? = null,
        @Query("project_id") projectId: String? = null
    ): QcOverviewDto

    @Streaming
    @GET("qc/reports/export")
    suspend fun export(@Query("format") format: String): ResponseBody

    @GET("quality/evaluations")
    suspend fun evaluations(
        @Query("skip") skip: Int = 0,
        @Query("limit") limit: Int = 50
    ): List<EvaluationItemDto>
}
