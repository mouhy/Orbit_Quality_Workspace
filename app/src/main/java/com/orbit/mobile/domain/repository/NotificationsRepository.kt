package com.orbit.mobile.domain.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.domain.model.Notification

// Notifications contract
interface NotificationsRepository {

    suspend fun list(): ApiResult<List<Notification>>

    suspend fun markRead(id: String): ApiResult<Unit>

    suspend fun markAllRead(): ApiResult<Unit>
}
