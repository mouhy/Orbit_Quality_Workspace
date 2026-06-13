package com.orbit.mobile.core.util

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive

// Safe string
fun JsonObject.str(key: String): String? =
    runCatching { this[key]?.jsonPrimitive?.content }.getOrNull()

// Safe int
fun JsonObject.int(key: String): Int? =
    runCatching { this[key]?.jsonPrimitive?.content?.toDouble()?.toInt() }.getOrNull()

// Safe double
fun JsonObject.dbl(key: String): Double? =
    runCatching { this[key]?.jsonPrimitive?.content?.toDouble() }.getOrNull()

// Safe object
fun JsonObject.obj(key: String): JsonObject? =
    runCatching { this[key]?.jsonObject }.getOrNull()

// Safe array
fun JsonObject.arr(key: String): JsonArray? =
    runCatching { this[key]?.jsonArray }.getOrNull()
