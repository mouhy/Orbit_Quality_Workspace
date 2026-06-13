package com.orbit.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitShapeTokens

// Pill badge
@Composable
fun OrbitBadge(
    text: String,
    modifier: Modifier = Modifier,
    color: Color = OrbitPrimary,
    filled: Boolean = false,
    showDot: Boolean = false
) {
    val bg = if (filled) color else color.copy(alpha = 0.14f)
    val fg = if (filled) Color.White else color

    Row(
        modifier = modifier
            .background(bg, OrbitShapeTokens.chip)
            .padding(horizontal = 10.dp, vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (showDot) {
            Box(
                modifier = Modifier
                    .padding(end = 6.dp)
                    .size(6.dp)
                    .background(fg, CircleShape)
            )
        }
        Text(
            text = text,
            style = MaterialTheme.typography.labelMedium,
            color = fg,
            maxLines = 1
        )
    }
}
