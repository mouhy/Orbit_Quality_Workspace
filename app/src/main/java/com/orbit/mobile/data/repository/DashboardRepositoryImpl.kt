package com.orbit.mobile.data.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.data.api.AnalyticsApi
import com.orbit.mobile.data.api.ProjectsApi
import com.orbit.mobile.data.api.SystemApi
import com.orbit.mobile.data.api.UsersApi
import com.orbit.mobile.data.dto.AuditLogDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.UserDashboardDto
import com.orbit.mobile.data.dto.UserDto
import com.orbit.mobile.domain.repository.DashboardRepository
import javax.inject.Inject
import javax.inject.Singleton

// Dashboard impl
@Singleton
class DashboardRepositoryImpl @Inject constructor(
    private val projectsApi: ProjectsApi,
    private val usersApi: UsersApi,
    private val systemApi: SystemApi,
    private val analyticsApi: AnalyticsApi
) : DashboardRepository {

    override suspend fun projects(status: String?): ApiResult<List<ProjectDto>> =
        safeApiCall { projectsApi.list(status) }

    override suspend fun users(): ApiResult<List<UserDto>> =
        safeApiCall { usersApi.list() }

    override suspend fun auditLogs(limit: Int): ApiResult<List<AuditLogDto>> =
        safeApiCall { systemApi.auditLogs(limit) }

    override suspend fun userDashboard(userId: String): ApiResult<UserDashboardDto> =
        safeApiCall { analyticsApi.userDashboard(userId) }

    override suspend fun seedDemo(): ApiResult<Unit> =
        safeApiCall { analyticsApi.seedDemo() }
}
