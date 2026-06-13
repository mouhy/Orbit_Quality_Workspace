package com.orbit.mobile.domain.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.data.dto.AnalyzeTaskRequest
import com.orbit.mobile.data.dto.CreateProjectRequest
import com.orbit.mobile.data.dto.CreateTaskRequest
import com.orbit.mobile.data.dto.CreateTeamRequest
import com.orbit.mobile.data.dto.MembershipDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.QualityAnalysisDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.data.dto.UpdateProjectRequest
import com.orbit.mobile.data.dto.UserAnalyticsDto

// Projects contract
interface ProjectsRepository {

    suspend fun list(status: String? = null): ApiResult<List<ProjectDto>>

    suspend fun create(body: CreateProjectRequest): ApiResult<Unit>

    suspend fun update(id: String, body: UpdateProjectRequest): ApiResult<Unit>

    suspend fun delete(id: String): ApiResult<Unit>
}

// Teams contract
interface TeamsRepository {

    suspend fun list(): ApiResult<List<TeamDto>>

    suspend fun get(id: String): ApiResult<TeamDto>

    suspend fun create(body: CreateTeamRequest): ApiResult<TeamDto>

    suspend fun update(id: String, name: String, description: String): ApiResult<Unit>

    suspend fun delete(id: String): ApiResult<Unit>

    suspend fun members(teamId: String): ApiResult<List<MembershipDto>>

    suspend fun addMember(teamId: String, userId: String, role: String): ApiResult<Unit>

    suspend fun removeMember(teamId: String, userId: String): ApiResult<Unit>
}

// Tasks contract
interface TasksRepository {

    suspend fun create(body: CreateTaskRequest): ApiResult<Unit>

    suspend fun analyze(taskId: String, body: AnalyzeTaskRequest): ApiResult<QualityAnalysisDto>
}

// Member analytics
interface MemberAnalyticsRepository {

    suspend fun userAnalytics(userId: String): ApiResult<UserAnalyticsDto>
}
