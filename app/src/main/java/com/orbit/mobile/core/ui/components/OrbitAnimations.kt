package com.orbit.mobile.core.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.offset
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

// Staggered entrance
@Composable
fun StaggeredAppear(
    index: Int,
    modifier: Modifier = Modifier,
    delayPerItem: Int = 35,
    content: @Composable () -> Unit
) {
    // Appear flag
    var appeared by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        delay(index.toLong() * delayPerItem)
        appeared = true
    }
    // Fade value
    val alpha by animateFloatAsState(
        targetValue = if (appeared) 1f else 0f,
        animationSpec = tween(300),
        label = "stagAlpha"
    )
    // Slide value
    val offsetY by animateDpAsState(
        targetValue = if (appeared) 0.dp else 16.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "stagOffset"
    )
    Box(
        modifier = modifier
            .offset(y = offsetY)
            .alpha(alpha)
    ) {
        content()
    }
}
