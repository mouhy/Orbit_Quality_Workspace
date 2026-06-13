package com.orbit.mobile.core.util

import com.orbit.mobile.R
import java.time.Instant
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter

// Parse ISO
fun parseInstant(value: String?): Instant? {
    if (value.isNullOrBlank()) return null
    return runCatching { Instant.parse(value) }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(value).toInstant(ZoneOffset.UTC)
        }.getOrNull()
        ?: runCatching {
            LocalDateTime.parse(value, DateTimeFormatter.ISO_DATE_TIME).toInstant(ZoneOffset.UTC)
        }.getOrNull()
}

// Relative label
fun timeAgo(value: String?): UiText {
    val instant = parseInstant(value) ?: return UiText.Res(R.string.notif_just_now)
    val mins = (System.currentTimeMillis() - instant.toEpochMilli()) / 60_000
    return when {
        mins < 2 -> UiText.Res(R.string.notif_just_now)
        mins < 60 -> UiText.Raw("${mins}m")
        mins < 1440 -> UiText.Raw("${mins / 60}h")
        else -> UiText.Raw(
            instant.atZone(ZoneId.systemDefault()).toLocalDate()
                .format(DateTimeFormatter.ofPattern("d MMM"))
        )
    }
}
