package com.orbit.mobile.domain.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.data.dto.AuditLogDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.UserDashboardDto
import com.orbit.mobile.data.dto.UserDto

// Dashboard contract
interface DashboardRepository {

    suspend fun projects(status: String? = null): ApiResult<List<ProjectDto>>

    suspend fun users(): ApiResult<List<UserDto>>

    suspend fun auditLogs(limit: Int = 50): ApiResult<List<AuditLogDto>>

    suspend fun userDashboard(userId: String): ApiResult<UserDashboardDto>

    suspend fun seedDemo(): ApiResult<Unit>
}
