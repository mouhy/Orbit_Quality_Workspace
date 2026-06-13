package com.orbit.mobile.data.repository

import com.orbit.mobile.core.datastore.SessionManager
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.NetworkError
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.data.api.AuthApi
import com.orbit.mobile.data.dto.LoginRequestDto
import com.orbit.mobile.data.dto.SetupRequestDto
import com.orbit.mobile.domain.model.AuthUser
import com.orbit.mobile.domain.repository.AuthRepository
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.withTimeout
import javax.inject.Inject
import javax.inject.Singleton

// Boot timeout
private const val INIT_TIMEOUT_MS = 8_000L

// Auth impl
@Singleton
class AuthRepositoryImpl @Inject constructor(
    private val api: AuthApi,
    private val session: SessionManager
) : AuthRepository {

    override suspend fun checkInitialized(): ApiResult<Boolean> = try {
        withTimeout(INIT_TIMEOUT_MS) {
            safeApiCall { api.initStatus().initialized }
        }
    } catch (e: TimeoutCancellationException) {
        ApiResult.Failure(NetworkError.Timeout)
    }

    override suspend fun login(email: String, password: String): ApiResult<AuthUser> {
        val result = safeApiCall {
            val dto = api.login(LoginRequestDto(email = email, password = password))
            AuthUser(
                id = dto.id,
                email = dto.email ?: email,
                name = dto.name ?: "",
                role = dto.role ?: "staff",
                token = dto.token ?: ""
            )
        }
        if (result is ApiResult.Success) {
            session.save(
                token = result.data.token,
                userId = result.data.id,
                role = result.data.role,
                fullName = result.data.name,
                email = result.data.email
            )
        }
        return result
    }

    override suspend fun setupFounder(
        fullName: String,
        email: String,
        password: String
    ): ApiResult<AuthUser> = safeApiCall {
        val dto = api.setup(
            SetupRequestDto(fullName = fullName, email = email, password = password)
        )
        AuthUser(
            id = dto.id,
            email = dto.email ?: email,
            name = dto.name ?: fullName,
            role = dto.role ?: "founder",
            token = dto.token ?: ""
        )
    }

    override suspend fun logout(): ApiResult<Unit> {
        // Blacklist token
        val result = safeApiCall { api.logout() }
        // Always clear
        session.clear()
        return when (result) {
            is ApiResult.Success -> ApiResult.Success(Unit)
            is ApiResult.Failure -> ApiResult.Success(Unit)
        }
    }
}
