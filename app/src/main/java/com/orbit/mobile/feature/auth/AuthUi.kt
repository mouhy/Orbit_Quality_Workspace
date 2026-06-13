package com.orbit.mobile.feature.auth

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.VisibilityOff
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSky
import com.orbit.mobile.core.theme.OrbitTheme

// Brand gradient
private val SubmitGradient = Brush.linearGradient(listOf(OrbitPrimary, OrbitSky))

// Gradient submit
@Composable
fun AuthGradientButton(
    text: String,
    loadingText: String,
    loading: Boolean,
    enabled: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val active = enabled && !loading
    Surface(
        onClick = onClick,
        modifier = modifier
            .fillMaxWidth()
            .height(44.dp),
        enabled = active,
        shape = RoundedCornerShape(8.dp),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .background(SubmitGradient, RoundedCornerShape(8.dp))
                .background(
                    if (active) Color.Transparent
                    else OrbitTheme.colors.appBackground.copy(alpha = 0.45f),
                    RoundedCornerShape(8.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (loading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .padding(end = 8.dp)
                            .size(14.dp),
                        color = Color.White,
                        strokeWidth = 2.dp
                    )
                }
                Text(
                    text = if (loading) loadingText else text,
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}

// Error banner
@Composable
fun AuthErrorBox(message: String, modifier: Modifier = Modifier) {
    val colors = OrbitTheme.colors
    val bg = if (colors.isDark) OrbitDanger.copy(alpha = 0.08f) else Color(0xFFFEF2F2)
    val border = if (colors.isDark) OrbitDanger.copy(alpha = 0.2f) else Color(0xFFFECACA)
    val textColor = if (colors.isDark) Color(0xFFF87171) else Color(0xFFDC2626)

    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(bg, RoundedCornerShape(7.dp))
            .border(1.dp, border, RoundedCornerShape(7.dp))
            .padding(horizontal = 12.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = Icons.Outlined.ErrorOutline,
            contentDescription = null,
            modifier = Modifier
                .padding(end = 8.dp)
                .size(14.dp),
            tint = textColor
        )
        Text(
            text = message,
            style = MaterialTheme.typography.bodySmall,
            color = textColor
        )
    }
}

// Eye toggle
@Composable
fun PasswordEyeButton(
    visible: Boolean,
    onToggle: () -> Unit,
    contentDescriptionShow: String,
    contentDescriptionHide: String
) {
    val colors = OrbitTheme.colors
    IconButton(onClick = onToggle, modifier = Modifier.size(28.dp)) {
        Icon(
            imageVector = if (visible) Icons.Outlined.VisibilityOff else Icons.Outlined.Visibility,
            contentDescription = if (visible) contentDescriptionHide else contentDescriptionShow,
            modifier = Modifier.size(18.dp),
            tint = colors.textMuted
        )
    }
}
