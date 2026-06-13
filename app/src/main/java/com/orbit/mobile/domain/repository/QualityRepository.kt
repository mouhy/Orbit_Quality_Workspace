package com.orbit.mobile.domain.repository

import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.data.dto.AiHistoryItemDto
import com.orbit.mobile.data.dto.EvaluateRequest
import com.orbit.mobile.data.dto.EvaluationResultDto
import com.orbit.mobile.data.dto.ReportTypeDto
import com.orbit.mobile.data.dto.TaskDetailDto
import okhttp3.ResponseBody

// Quality contract
interface QualityRepository {

    suspend fun reportTypes(): ApiResult<List<ReportTypeDto>>

    suspend fun evaluate(body: EvaluateRequest): ApiResult<EvaluationResultDto>

    suspend fun history(): ApiResult<List<AiHistoryItemDto>>

    suspend fun downloadTemplate(key: String, lang: String): ApiResult<ResponseBody>

    suspend fun task(taskId: String): ApiResult<TaskDetailDto>
}
