package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.EventCreateRequest
import com.orbit.mobile.data.dto.EventDto
import com.orbit.mobile.data.dto.RsvpRequest
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Path
import retrofit2.http.Query

// Events endpoints
interface EventsApi {

    @GET("events")
    suspend fun list(
        @Query("start_date") startDate: String? = null,
        @Query("end_date") endDate: String? = null
    ): List<EventDto>

    @POST("events")
    suspend fun create(@Body body: EventCreateRequest): EventDto

    @PUT("events/{id}")
    suspend fun update(@Path("id") id: String, @Body body: EventCreateRequest): EventDto

    @DELETE("events/{id}")
    suspend fun delete(@Path("id") id: String)

    @POST("events/{id}/rsvp")
    suspend fun rsvp(@Path("id") id: String, @Body body: RsvpRequest)
}
