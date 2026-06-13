package com.orbit.mobile.data.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.safeApiCall
import com.orbit.mobile.data.api.QualityApi
import com.orbit.mobile.data.dto.AiHistoryItemDto
import com.orbit.mobile.data.dto.EvaluateRequest
import com.orbit.mobile.data.dto.EvaluationResultDto
import com.orbit.mobile.data.dto.ReportTypeDto
import com.orbit.mobile.data.dto.TaskDetailDto
import com.orbit.mobile.domain.repository.QualityRepository
import okhttp3.ResponseBody
import javax.inject.Inject
import javax.inject.Singleton

// Quality impl
@Singleton
class QualityRepositoryImpl @Inject constructor(
    private val api: QualityApi
) : QualityRepository {

    override suspend fun reportTypes(): ApiResult<List<ReportTypeDto>> =
        safeApiCall { api.reportTypes() }

    override suspend fun evaluate(body: EvaluateRequest): ApiResult<EvaluationResultDto> =
        safeApiCall { api.evaluate(body) }

    override suspend fun history(): ApiResult<List<AiHistoryItemDto>> =
        safeApiCall { api.history() }

    override suspend fun downloadTemplate(key: String, lang: String): ApiResult<ResponseBody> =
        safeApiCall { api.downloadTemplate(key, lang) }

    override suspend fun task(taskId: String): ApiResult<TaskDetailDto> =
        safeApiCall { api.task(taskId) }
}
