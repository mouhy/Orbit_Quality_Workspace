package com.orbit.mobile.core.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.orbit.mobile.core.network.NetworkError
import com.orbit.mobile.core.network.messageRes
import com.orbit.mobile.core.network.serverMessage

// Text holder
sealed interface UiText {
    data class Res(@StringRes val id: Int) : UiText
    data class Raw(val value: String) : UiText
}

// Resolve text
@Composable
fun UiText.asString(): String = when (this) {
    is UiText.Res -> stringResource(id)
    is UiText.Raw -> value
}

// Error text
fun NetworkError.toUiText(): UiText =
    serverMessage()?.let { UiText.Raw(it) } ?: UiText.Res(messageRes())
