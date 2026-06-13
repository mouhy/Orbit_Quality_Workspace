package com.orbit.mobile.core.theme

// Theme modes
enum class ThemeMode { LIGHT, DARK, SYSTEM;

    companion object {
        // Parse stored
        fun from(value: String?): ThemeMode = when (value) {
            "light" -> LIGHT
            "dark" -> DARK
            "system" -> SYSTEM
            else -> DARK
        }
    }

    // Storage key
    val storageValue: String
        get() = name.lowercase()
}
