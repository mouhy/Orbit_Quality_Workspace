package com.orbit.mobile.data.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.map
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.data.api.NotificationsApi
import com.orbit.mobile.domain.model.Notification
import com.orbit.mobile.domain.model.NotificationType
import com.orbit.mobile.domain.repository.NotificationsRepository
import javax.inject.Inject
import javax.inject.Singleton

// Notifications impl
@Singleton
class NotificationsRepositoryImpl @Inject constructor(
    private val api: NotificationsApi
) : NotificationsRepository {

    override suspend fun list(): ApiResult<List<Notification>> =
        safeApiCall { api.list() }.map { items ->
            items.map { dto ->
                Notification(
                    id = dto.id,
                    type = NotificationType.from(dto.type),
                    title = dto.payload?.get("title") ?: "",
                    body = dto.payload?.get("body") ?: "",
                    isRead = dto.isRead,
                    createdAt = dto.createdAt,
                    // Routing refs
                    projectId = dto.payload?.get("project_id"),
                    taskId = dto.payload?.get("task_id"),
                    teamId = dto.payload?.get("team_id")
                )
            }
        }

    override suspend fun markRead(id: String): ApiResult<Unit> =
        safeApiCall { api.markRead(id) }

    override suspend fun markAllRead(): ApiResult<Unit> =
        safeApiCall { api.markAllRead() }
}
