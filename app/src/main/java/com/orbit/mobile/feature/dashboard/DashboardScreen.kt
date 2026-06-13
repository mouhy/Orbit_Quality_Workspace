package com.orbit.mobile.feature.dashboard

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.Sparkline
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.time.format.TextStyle as JavaTextStyle
import java.util.Locale
import kotlin.math.abs

// Indigo accent
val DashIndigo = Color(0xFF6366F1)

// Role switcher
@Composable
fun DashboardScreen(role: String?, onNavigate: (String) -> Unit) {
    if (role == "staff") {
        StaffDashboardContent(onNavigate = onNavigate)
    } else {
        AdminDashboardContent(onNavigate = onNavigate)
    }
}

// Staff palettes
fun dashPriorityColor(priority: String?): Color = when (priority?.lowercase()) {
    "high", "urgent" -> OrbitDanger
    "medium" -> OrbitWarning
    "low" -> OrbitSuccess
    else -> DashIndigo
}

fun dashStatusColor(status: String?): Color = when (status?.uppercase()) {
    "TODO" -> DashIndigo
    "IN_PROGRESS" -> OrbitWarning
    "REVIEW" -> Color(0xFF8B5CF6)
    "DONE" -> OrbitSuccess
    else -> DashIndigo
}

// Health helpers
fun healthColor(progress: Int, status: String): Color = when {
    status.uppercase().contains("HOLD") -> OrbitDanger
    progress >= 70 -> OrbitSuccess
    progress >= 40 -> OrbitWarning
    else -> OrbitDanger
}

@Composable
fun healthLabel(progress: Int, status: String): String = when {
    status.uppercase().contains("HOLD") -> stringResource(R.string.health_blocked)
    progress >= 70 -> stringResource(R.string.health_healthy)
    progress >= 40 -> stringResource(R.string.health_at_risk)
    else -> stringResource(R.string.health_blocked)
}

// Today label
fun todayLabel(): String {
    val now = LocalDate.now()
    val dow = now.dayOfWeek.getDisplayName(JavaTextStyle.FULL, Locale.getDefault())
    return "$dow, ${now.format(DateTimeFormatter.ofPattern("d MMMM yyyy"))}"
}

// Days label
@Composable
fun deadlineShortLabel(days: Int): String = when {
    days < 0 -> stringResource(R.string.dash_days_late, abs(days))
    days == 0 -> stringResource(R.string.dash_today)
    else -> stringResource(R.string.dash_days_short, days)
}

// Hash avatar
@Composable
fun HashAvatar(name: String, size: androidx.compose.ui.unit.Dp = 28.dp) {
    val safe = name.ifBlank { "U" }
    val hue = ((safe[0].code * 47 + (safe.getOrNull(1)?.code ?: 65) * 13) % 360).toFloat()
    val initials = safe.trim().split(Regex("\\s+")).take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }.joinToString("")
    Box(
        modifier = Modifier
            .size(size)
            .background(
                Brush.linearGradient(
                    listOf(
                        Color.hsl(hue, 0.55f, 0.44f),
                        Color.hsl((hue + 40f) % 360f, 0.48f, 0.32f)
                    )
                ),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            fontSize = androidx.compose.ui.unit.TextUnit(
                size.value * 0.35f,
                androidx.compose.ui.unit.TextUnitType.Sp
            ),
            fontWeight = FontWeight.Bold,
            color = Color.White
        )
    }
}

// Section card
@Composable
fun DashCard(
    modifier: Modifier = Modifier,
    contentPadding: androidx.compose.foundation.layout.PaddingValues =
        androidx.compose.foundation.layout.PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = OrbitTheme.colors
    Column(
        modifier = modifier
            .fillMaxWidth()
            .background(colors.surface, RoundedCornerShape(16.dp))
            .border(1.dp, colors.border, RoundedCornerShape(16.dp))
            .padding(contentPadding),
        content = content
    )
}

// Card header
@Composable
fun DashCardHeader(
    title: String,
    subtitle: String? = null,
    action: (@Composable () -> Unit)? = null
) {
    val colors = OrbitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                color = colors.textPrimary
            )
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
        }
        action?.invoke()
    }
}

// View all
@Composable
fun ViewAllLink(onClick: () -> Unit) {
    androidx.compose.material3.Surface(onClick = onClick, color = Color.Transparent) {
        Text(
            text = stringResource(R.string.dash_view_all),
            style = MaterialTheme.typography.labelMedium,
            color = DashIndigo,
            modifier = Modifier.padding(4.dp)
        )
    }
}

// Tiny pill
@Composable
fun TinyPill(text: String, color: Color) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelSmall,
        fontWeight = FontWeight.Bold,
        color = color,
        modifier = Modifier
            .background(color.copy(alpha = 0.08f), RoundedCornerShape(20.dp))
            .padding(horizontal = 7.dp, vertical = 2.dp)
    )
}

// KPI card
@Composable
fun KpiCard(
    label: String,
    value: String,
    color: Color,
    spark: List<Int>,
    modifier: Modifier = Modifier,
    loading: Boolean = false
) {
    val colors = OrbitTheme.colors
    Column(
        modifier = modifier
            .background(colors.surface, RoundedCornerShape(12.dp))
            .border(1.dp, colors.border, RoundedCornerShape(12.dp))
            .padding(horizontal = 14.dp, vertical = 12.dp)
    ) {
        Text(
            text = label.uppercase(),
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = colors.textMuted,
            maxLines = 1
        )
        Spacer(Modifier.size(8.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom
        ) {
            Text(
                text = if (loading) "—" else value,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.ExtraBold,
                color = color,
                modifier = Modifier.weight(1f)
            )
            if (!loading) {
                Sparkline(
                    data = spark,
                    color = color,
                    modifier = Modifier.size(44.dp, 24.dp)
                )
            }
        }
    }
}

// Error banner
@Composable
fun ErrorBanner(message: String, onRetry: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(OrbitDanger.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .border(1.dp, OrbitDanger.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = OrbitDanger,
            modifier = Modifier.weight(1f)
        )
        androidx.compose.material3.Surface(
            onClick = onRetry,
            color = OrbitDanger,
            shape = RoundedCornerShape(6.dp)
        ) {
            Text(
                text = stringResource(R.string.action_retry),
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = Color.White,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
            )
        }
    }
}

// Seeding banner
@Composable
fun SeedingBanner(text: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(DashIndigo.copy(alpha = 0.1f), RoundedCornerShape(10.dp))
            .border(1.dp, DashIndigo.copy(alpha = 0.22f), RoundedCornerShape(10.dp))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(13.dp),
            color = DashIndigo,
            strokeWidth = 2.dp
        )
        Spacer(Modifier.size(10.dp))
        Text(
            text = text,
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.SemiBold,
            color = DashIndigo
        )
    }
}
