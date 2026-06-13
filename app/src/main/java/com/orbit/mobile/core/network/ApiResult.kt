package com.orbit.mobile.core.network

import androidx.annotation.StringRes
import com.orbit.mobile.R
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import retrofit2.HttpException
import java.io.IOException
import java.net.SocketTimeoutException

// Error kinds
sealed interface NetworkError {
    data object Unauthorized : NetworkError
    data object Forbidden : NetworkError
    data object NotFound : NetworkError
    data class Validation(val message: String) : NetworkError
    data class Http(val code: Int, val message: String) : NetworkError
    data object Timeout : NetworkError
    data object NoConnection : NetworkError
    data class Unknown(val message: String) : NetworkError
}

// Message resource
@StringRes
fun NetworkError.messageRes(): Int = when (this) {
    NetworkError.Unauthorized -> R.string.error_unauthorized
    NetworkError.Forbidden -> R.string.error_forbidden
    NetworkError.NotFound -> R.string.error_not_found
    NetworkError.Timeout -> R.string.error_timeout
    NetworkError.NoConnection -> R.string.error_no_connection
    is NetworkError.Http -> R.string.error_server
    is NetworkError.Validation, is NetworkError.Unknown -> R.string.state_error_generic
}

// Server text
fun NetworkError.serverMessage(): String? = when (this) {
    is NetworkError.Validation -> message
    is NetworkError.Http -> message.ifBlank { null }
    is NetworkError.Unknown -> message.ifBlank { null }
    else -> null
}

// Result wrapper
sealed interface ApiResult<out T> {
    data class Success<T>(val data: T) : ApiResult<T>
    data class Failure(val error: NetworkError) : ApiResult<Nothing>
}

// Map data
inline fun <T, R> ApiResult<T>.map(transform: (T) -> R): ApiResult<R> = when (this) {
    is ApiResult.Success -> ApiResult.Success(transform(data))
    is ApiResult.Failure -> this
}

// Success hook
inline fun <T> ApiResult<T>.onSuccess(block: (T) -> Unit): ApiResult<T> {
    if (this is ApiResult.Success) block(data)
    return this
}

// Failure hook
inline fun <T> ApiResult<T>.onFailure(block: (NetworkError) -> Unit): ApiResult<T> {
    if (this is ApiResult.Failure) block(error)
    return this
}

// Data or null
fun <T> ApiResult<T>.getOrNull(): T? = (this as? ApiResult.Success)?.data

// Lenient parser
private val errorJson = Json { ignoreUnknownKeys = true; isLenient = true }

// FastAPI detail
fun parseErrorDetail(body: String?): String? {
    if (body.isNullOrBlank()) return null
    return try {
        val root = errorJson.parseToJsonElement(body).jsonObject
        val detail = root["detail"] ?: root["message"] ?: return null
        when (detail) {
            is JsonPrimitive -> detail.content
            is JsonArray -> detail
                .mapNotNull { item ->
                    runCatching { item.jsonObject["msg"]?.jsonPrimitive?.content }.getOrNull()
                }
                .joinToString("\n")
                .ifBlank { null }
            else -> detail.toString()
        }
    } catch (_: Exception) {
        null
    }
}

// HTTP mapping
fun mapHttpException(e: HttpException): NetworkError {
    val detail = parseErrorDetail(
        runCatching { e.response()?.errorBody()?.string() }.getOrNull()
    )
    return when (e.code()) {
        401 -> NetworkError.Unauthorized
        403 -> NetworkError.Forbidden
        404 -> NetworkError.NotFound
        422 -> NetworkError.Validation(detail ?: e.message())
        else -> NetworkError.Http(e.code(), detail ?: e.message())
    }
}

// Safe call
suspend inline fun <T> safeApiCall(crossinline block: suspend () -> T): ApiResult<T> {
    return try {
        ApiResult.Success(block())
    } catch (e: CancellationException) {
        throw e
    } catch (e: HttpException) {
        ApiResult.Failure(mapHttpException(e))
    } catch (e: SocketTimeoutException) {
        ApiResult.Failure(NetworkError.Timeout)
    } catch (e: IOException) {
        ApiResult.Failure(NetworkError.NoConnection)
    } catch (e: Exception) {
        ApiResult.Failure(NetworkError.Unknown(e.message ?: ""))
    }
}
