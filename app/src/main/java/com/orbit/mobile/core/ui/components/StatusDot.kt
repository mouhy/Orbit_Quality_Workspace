package com.orbit.mobile.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.orbit.mobile.core.theme.OnlineDot
import com.orbit.mobile.core.theme.OrbitSlate

// Presence dot
@Composable
fun StatusDot(
    online: Boolean = true,
    modifier: Modifier = Modifier,
    size: Dp = 10.dp
) {
    val color = if (online) OnlineDot else OrbitSlate
    val glow = if (online) OnlineDot.copy(alpha = 0.3f) else Color.Transparent

    Box(
        modifier = modifier
            .size(size)
            .background(glow, CircleShape),
        contentAlignment = Alignment.Center
    ) {
        Box(
            modifier = Modifier
                .size(size * 0.62f)
                .background(color, CircleShape)
        )
    }
}
