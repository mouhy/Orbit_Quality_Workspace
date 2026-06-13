package com.orbit.mobile.core.theme

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Brand
val OrbitPrimary = Color(0xFF1D6EF5)
val OrbitSky = Color(0xFF0EA5E9)
val OrbitSuccess = Color(0xFF10B981)
val OrbitWarning = Color(0xFFF59E0B)
val OrbitDanger = Color(0xFFEF4444)
val OrbitPurple = Color(0xFF8B5CF6)
val OrbitTeal = Color(0xFF06B6D4)
val OrbitOrange = Color(0xFFF97316)
val OrbitSlate = Color(0xFF64748B)

// Kanban
val KanbanTodo = OrbitSlate
val KanbanInProgress = OrbitPrimary
val KanbanReview = OrbitWarning
val KanbanDone = OrbitSuccess

// Priority
val PriorityUrgent = OrbitDanger
val PriorityHigh = OrbitOrange
val PriorityMedium = OrbitWarning
val PriorityLow = OrbitSlate

// Online
val OnlineDot = OrbitSuccess

// Extended tokens
@Immutable
data class OrbitExtendedColors(
    val isDark: Boolean,
    val appBackground: Color,
    val pageBackground: Color,
    val surface: Color,
    val surface2: Color,
    val surface3: Color,
    val sidebarBg: Color,
    val headerBg: Color,
    val popupBg: Color,
    val menuBg: Color,
    val border: Color,
    val borderStrong: Color,
    val textPrimary: Color,
    val textBody: Color,
    val textSecondary: Color,
    val textSecondaryStrong: Color,
    val textMuted: Color,
    val authPanelBg: Color,
    val focusRing: Color
)

// Light tokens
val LightOrbitColors = OrbitExtendedColors(
    isDark = false,
    appBackground = Color(0xFFF5F6FA),
    pageBackground = Color(0xFFF0F4F8),
    surface = Color(0xFFFFFFFF),
    surface2 = Color(0xFFF8FAFC),
    surface3 = Color(0xFFEDF2F7),
    sidebarBg = Color(0xFFFFFFFF),
    headerBg = Color(0xF0FFFFFF),
    popupBg = Color(0xFFFFFFFF),
    menuBg = Color(0xFFFFFFFF),
    border = Color(0x12000000),
    borderStrong = Color(0x14000000),
    textPrimary = Color(0xFF0F172A),
    textBody = Color(0xFF0F172A),
    textSecondary = Color(0xFF475569),
    textSecondaryStrong = Color(0xFF334155),
    textMuted = Color(0xFF94A3B8),
    authPanelBg = Color(0xFF0F172A),
    focusRing = Color(0x1F1D6EF5)
)

// Dark tokens
val DarkOrbitColors = OrbitExtendedColors(
    isDark = true,
    appBackground = Color(0xFF0A0C14),
    pageBackground = Color(0xFF0B0D14),
    surface = Color(0xFF111420),
    surface2 = Color(0xFF161924),
    surface3 = Color(0xFF1C2030),
    sidebarBg = Color(0xFF0F1220),
    headerBg = Color(0xEB0A0C14),
    popupBg = Color(0xFF070A1A),
    menuBg = Color(0xFF131724),
    border = Color(0x12FFFFFF),
    borderStrong = Color(0x14FFFFFF),
    textPrimary = Color(0xFFF0F4F9),
    textBody = Color(0xFFE5E7EB),
    textSecondary = Color(0x9EFFFFFF),
    textSecondaryStrong = Color(0x9EFFFFFF),
    textMuted = Color(0x5CFFFFFF),
    authPanelBg = Color(0xFF0F172A),
    focusRing = Color(0x1F1D6EF5)
)

// Composition local
val LocalOrbitColors = staticCompositionLocalOf { DarkOrbitColors }

// Gradients
object OrbitGradients {
    val avatarFallback = Brush.linearGradient(
        listOf(OrbitPrimary, OrbitPurple)
    )
    val channelPublic = Brush.linearGradient(
        listOf(Color(0xFF6D28D9), Color(0xFF4F46E5), Color(0xFF2563EB))
    )
    val channelOps = Brush.linearGradient(
        listOf(Color(0xFFC2410C), Color(0xFFEA580C), Color(0xFFF59E0B))
    )
    val brandAuthPanel = Brush.linearGradient(
        listOf(Color(0xFF0C1A38), Color(0xFF071028), Color(0xFF04091A))
    )
}

// Role color
fun roleColor(role: String): Color = when (role) {
    "founder" -> OrbitWarning
    "it_staff" -> OrbitTeal
    "admin" -> OrbitPrimary
    "sub_admin" -> OrbitPurple
    "manager" -> OrbitSky
    "quality_control", "quality_manager" -> OrbitSuccess
    else -> OrbitSlate
}

// Kanban color
fun kanbanColor(status: String): Color = when (status) {
    "todo" -> KanbanTodo
    "in_progress" -> KanbanInProgress
    "review" -> KanbanReview
    "done" -> KanbanDone
    else -> OrbitSlate
}

// Priority color
fun priorityColor(priority: String): Color = when (priority.lowercase()) {
    "urgent" -> PriorityUrgent
    "high" -> PriorityHigh
    "medium" -> PriorityMedium
    "low" -> PriorityLow
    else -> OrbitSlate
}

// Status color
fun projectStatusColor(status: String): Color = when (status.uppercase()) {
    "COMPLETED" -> OrbitSuccess
    "ON HOLD", "ON_HOLD" -> OrbitWarning
    else -> OrbitPrimary
}
