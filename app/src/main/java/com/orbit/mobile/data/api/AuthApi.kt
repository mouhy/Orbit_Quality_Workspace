package com.orbit.mobile.data.api

import com.orbit.mobile.data.dto.AuthUserDto
import com.orbit.mobile.data.dto.InitStatusDto
import com.orbit.mobile.data.dto.LoginRequestDto
import com.orbit.mobile.data.dto.SetupRequestDto
import com.orbit.mobile.data.dto.StatusDto
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.POST

// Auth endpoints
interface AuthApi {

    @GET("system/init-status")
    suspend fun initStatus(): InitStatusDto

    @POST("auth/login")
    suspend fun login(@Body body: LoginRequestDto): AuthUserDto

    @POST("auth/setup")
    suspend fun setup(@Body body: SetupRequestDto): AuthUserDto

    @POST("auth/logout")
    suspend fun logout(): StatusDto
}
