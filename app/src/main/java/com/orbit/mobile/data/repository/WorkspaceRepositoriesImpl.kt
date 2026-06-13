package com.orbit.mobile.data.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.map
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.data.api.AnalyticsApi
import com.orbit.mobile.data.api.ProjectsApi
import com.orbit.mobile.data.api.QcApi
import com.orbit.mobile.data.api.TasksApi
import com.orbit.mobile.data.api.TeamsApi
import com.orbit.mobile.data.dto.AddMemberRequest
import com.orbit.mobile.data.dto.AnalyzeTaskRequest
import com.orbit.mobile.data.dto.CreateProjectRequest
import com.orbit.mobile.data.dto.CreateTaskRequest
import com.orbit.mobile.data.dto.CreateTeamRequest
import com.orbit.mobile.data.dto.MembershipDto
import com.orbit.mobile.data.dto.ProjectDto
import com.orbit.mobile.data.dto.QualityAnalysisDto
import com.orbit.mobile.data.dto.TeamDto
import com.orbit.mobile.data.dto.UpdateProjectRequest
import com.orbit.mobile.data.dto.UpdateTeamRequest
import com.orbit.mobile.data.dto.UserAnalyticsDto
import com.orbit.mobile.domain.repository.MemberAnalyticsRepository
import com.orbit.mobile.domain.repository.ProjectsRepository
import com.orbit.mobile.domain.repository.TasksRepository
import com.orbit.mobile.domain.repository.TeamsRepository
import javax.inject.Inject
import javax.inject.Singleton

// Projects impl
@Singleton
class ProjectsRepositoryImpl @Inject constructor(
    private val api: ProjectsApi
) : ProjectsRepository {

    override suspend fun list(status: String?): ApiResult<List<ProjectDto>> =
        safeApiCall { api.list(status) }

    override suspend fun create(body: CreateProjectRequest): ApiResult<Unit> =
        safeApiCall { api.create(body) }.map { }

    override suspend fun update(id: String, body: UpdateProjectRequest): ApiResult<Unit> =
        safeApiCall { api.update(id, body) }.map { }

    override suspend fun delete(id: String): ApiResult<Unit> =
        safeApiCall { api.delete(id) }
}

// Teams impl
@Singleton
class TeamsRepositoryImpl @Inject constructor(
    private val api: TeamsApi
) : TeamsRepository {

    override suspend fun list(): ApiResult<List<TeamDto>> =
        safeApiCall { api.list() }

    override suspend fun get(id: String): ApiResult<TeamDto> =
        safeApiCall { api.get(id) }

    override suspend fun create(body: CreateTeamRequest): ApiResult<TeamDto> =
        safeApiCall { api.create(body) }

    override suspend fun update(id: String, name: String, description: String): ApiResult<Unit> =
        safeApiCall { api.update(id, UpdateTeamRequest(name, description)) }.map { }

    override suspend fun delete(id: String): ApiResult<Unit> =
        safeApiCall { api.delete(id) }

    override suspend fun members(teamId: String): ApiResult<List<MembershipDto>> =
        safeApiCall { api.members(teamId) }

    override suspend fun addMember(teamId: String, userId: String, role: String): ApiResult<Unit> =
        safeApiCall { api.addMember(teamId, AddMemberRequest(userId, role)) }.map { }

    override suspend fun removeMember(teamId: String, userId: String): ApiResult<Unit> =
        safeApiCall { api.removeMember(teamId, userId) }
}

// Tasks impl
@Singleton
class TasksRepositoryImpl @Inject constructor(
    private val tasksApi: TasksApi,
    private val qcApi: QcApi
) : TasksRepository {

    override suspend fun create(body: CreateTaskRequest): ApiResult<Unit> =
        safeApiCall { tasksApi.create(body) }.map { }

    override suspend fun analyze(
        taskId: String,
        body: AnalyzeTaskRequest
    ): ApiResult<QualityAnalysisDto> = safeApiCall {
        val parts = mutableListOf<okhttp3.MultipartBody.Part>()
        parts += okhttp3.MultipartBody.Part.createFormData(
            "project_id",
            body.projectId ?: "public-group"
        )
        parts += okhttp3.MultipartBody.Part.createFormData("task_title", body.taskTitle)
        parts += okhttp3.MultipartBody.Part.createFormData(
            "task_description",
            body.taskDescription
        )
        qcApi.analyzeTask(taskId, parts)
    }
}

// Analytics impl
@Singleton
class MemberAnalyticsRepositoryImpl @Inject constructor(
    private val api: AnalyticsApi
) : MemberAnalyticsRepository {

    override suspend fun userAnalytics(userId: String): ApiResult<UserAnalyticsDto> =
        safeApiCall { api.userAnalytics(userId) }
}
