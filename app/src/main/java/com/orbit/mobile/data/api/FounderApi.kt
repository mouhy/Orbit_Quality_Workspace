package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.AssignItRequest
import com.orbit.mobile.data.dto.EntityCreateRequest
import com.orbit.mobile.data.dto.EntityDto
import com.orbit.mobile.data.dto.FounderAccountCreateRequest
import com.orbit.mobile.data.dto.FounderAccountDto
import com.orbit.mobile.data.dto.FounderAccountPatchRequest
import com.orbit.mobile.data.dto.FounderMetricsDto
import com.orbit.mobile.data.dto.FrameworkDto
import com.orbit.mobile.data.dto.FrameworkRequest
import com.orbit.mobile.data.dto.NewPasswordRequest
import kotlinx.serialization.json.JsonElement
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.PATCH
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

// Founder command-center endpoints (require_founder on the backend)
interface FounderApi {

    // KPIs, financials, AI trends and alerts in one payload
    @GET("founder/metrics")
    suspend fun metrics(): FounderMetricsDto

    // IT staff account directory with optional search
    @GET("founder/accounts")
    suspend fun accounts(@Query("search") search: String? = null): List<FounderAccountDto>

    @POST("founder/accounts")
    suspend fun createAccount(@Body body: FounderAccountCreateRequest): JsonElement

    @PATCH("founder/accounts/{id}")
    suspend fun patchAccount(
        @Path("id") id: String,
        @Body body: FounderAccountPatchRequest
    ): JsonElement

    @POST("founder/accounts/{id}/reset-password")
    suspend fun resetAccountPassword(
        @Path("id") id: String,
        @Body body: NewPasswordRequest
    ): JsonElement

    // Flips active/suspended for one IT account
    @POST("founder/accounts/{id}/toggle-status")
    suspend fun toggleAccountStatus(@Path("id") id: String): JsonElement

    @DELETE("founder/accounts/{id}")
    suspend fun deleteAccount(@Path("id") id: String)

    // Quality frameworks CRUD (no delete endpoint exists)
    @GET("frameworks")
    suspend fun frameworks(): List<FrameworkDto>

    @POST("frameworks")
    suspend fun createFramework(@Body body: FrameworkRequest): FrameworkDto

    @PATCH("frameworks/{id}")
    suspend fun updateFramework(
        @Path("id") id: String,
        @Body body: FrameworkRequest
    ): FrameworkDto

    // Tenant entities + IT assignment
    @GET("entities")
    suspend fun entities(): List<EntityDto>

    @POST("entities")
    suspend fun createEntity(@Body body: EntityCreateRequest): EntityDto

    @POST("entities/{id}/assign-it")
    suspend fun assignIt(@Path("id") id: String, @Body body: AssignItRequest): JsonElement
}
