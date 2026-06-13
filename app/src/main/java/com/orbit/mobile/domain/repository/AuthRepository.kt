package com.orbit.mobile.domain.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.domain.model.AuthUser

// Auth contract
interface AuthRepository {

    suspend fun checkInitialized(): ApiResult<Boolean>

    suspend fun login(email: String, password: String): ApiResult<AuthUser>

    suspend fun setupFounder(fullName: String, email: String, password: String): ApiResult<AuthUser>

    suspend fun logout(): ApiResult<Unit>
}
