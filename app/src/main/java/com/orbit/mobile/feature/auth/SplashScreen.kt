package com.orbit.mobile.feature.auth

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.Wifi
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitLogo
import com.orbit.mobile.core.ui.components.OrbitLogoVariant
import kotlin.math.roundToInt

// Indigo accents
private val Indigo = Color(0xFF6366F1)
private val IndigoDark = Color(0xFF4F46E5)

// Boot screen
@Composable
fun SplashScreen(
    onNeedsSetup: () -> Unit,
    onReady: (loggedIn: Boolean, onboardingSeen: Boolean, role: String?) -> Unit,
    viewModel: SplashViewModel = hiltViewModel()
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    // Route out
    LaunchedEffect(state) {
        when (val s = state) {
            is BootState.NeedsSetup -> onNeedsSetup()
            is BootState.Ready -> onReady(s.loggedIn, s.onboardingSeen, s.role)
            else -> Unit
        }
    }

    when (state) {
        is BootState.BackendDown -> BackendDownContent(onRetry = viewModel::check)
        else -> StartingUpContent()
    }
}

// Starting spinner
@Composable
private fun StartingUpContent() {
    val colors = OrbitTheme.colors
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(colors.appBackground),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        OrbitLogo(
            variant = OrbitLogoVariant.Mark,
            modifier = Modifier.size(72.dp)
        )
        Spacer(Modifier.height(20.dp))
        CircularProgressIndicator(
            modifier = Modifier.size(28.dp),
            color = OrbitPrimary,
            strokeWidth = 2.5.dp,
            trackColor = OrbitPrimary.copy(alpha = 0.2f)
        )
        Spacer(Modifier.height(12.dp))
        Text(
            text = stringResource(R.string.boot_starting),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )
    }
}

// Down screen
@Composable
private fun BackendDownContent(onRetry: () -> Unit) {
    val colors = OrbitTheme.colors
    var countdown by remember { mutableIntStateOf(10) }
    // Ring track
    val trackColor = colors.textMuted.copy(alpha = 0.22f)

    // Auto retry
    LaunchedEffect(Unit) {
        countdown = 10
        while (countdown > 0) {
            kotlinx.coroutines.delay(1_000)
            countdown--
        }
        onRetry()
    }

    val transition = rememberInfiniteTransition(label = "down")

    // Float anim
    val floatY by transition.animateFloat(
        initialValue = 0f,
        targetValue = -8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1750, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // Ring anim
    val ringProgress by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(animation = tween(2000, easing = LinearEasing)),
        label = "ring"
    )

    // Theme background
    val bgBrush = if (colors.isDark) {
        Brush.linearGradient(listOf(colors.appBackground, colors.surface, colors.appBackground))
    } else {
        Brush.linearGradient(listOf(Color(0xFFEEF2FF), Color(0xFFF0F4FF), Color(0xFFEDE9FE)))
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(bgBrush)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Pulsing icon
        Box(contentAlignment = Alignment.Center) {
            Box(
                modifier = Modifier
                    .size(116.dp)
                    .scale(1f + 1.2f * ringProgress)
                    .background(
                        Indigo.copy(alpha = 0.35f * (1f - ringProgress)),
                        CircleShape
                    )
            )
            Box(
                modifier = Modifier
                    .offset { IntOffset(0, floatY.roundToInt()) }
                    .size(80.dp)
                    .background(colors.surface, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Rounded.Wifi,
                    contentDescription = null,
                    modifier = Modifier.size(36.dp),
                    tint = Indigo
                )
            }
        }
        Spacer(Modifier.height(36.dp))

        Text(
            text = stringResource(R.string.boot_connecting_title),
            style = MaterialTheme.typography.headlineLarge,
            color = colors.textPrimary
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = stringResource(R.string.boot_connecting_desc),
            style = MaterialTheme.typography.bodyMedium,
            color = colors.textSecondary,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(24.dp))

        // Bouncing dots
        Row(horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            listOf(0, 220, 440).forEach { delayMs ->
                val dotY by transition.animateFloat(
                    initialValue = 0f,
                    targetValue = -8f,
                    animationSpec = infiniteRepeatable(
                        animation = tween(700, delayMillis = delayMs, easing = LinearEasing),
                        repeatMode = RepeatMode.Reverse
                    ),
                    label = "dot$delayMs"
                )
                Box(
                    modifier = Modifier
                        .offset { IntOffset(0, dotY.roundToInt()) }
                        .size(9.dp)
                        .background(Indigo, CircleShape)
                )
            }
        }
        Spacer(Modifier.height(20.dp))

        // Countdown ring
        Box(modifier = Modifier.size(56.dp), contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(56.dp)) {
                val stroke = Stroke(width = 3.5.dp.toPx(), cap = StrokeCap.Round)
                drawCircle(
                    color = trackColor,
                    radius = size.minDimension / 2 - stroke.width,
                    style = stroke
                )
                drawArc(
                    color = Indigo,
                    startAngle = -90f,
                    sweepAngle = 360f * (countdown / 10f),
                    useCenter = false,
                    style = stroke,
                    topLeft = Offset(stroke.width, stroke.width),
                    size = androidx.compose.ui.geometry.Size(
                        size.width - stroke.width * 2,
                        size.height - stroke.width * 2
                    )
                )
            }
            Text(
                text = countdown.toString(),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.Bold,
                color = IndigoDark
            )
        }
        Spacer(Modifier.height(24.dp))

        // Retry button
        Surface(
            onClick = onRetry,
            shape = RoundedCornerShape(10.dp),
            color = Color.Transparent
        ) {
            Box(
                modifier = Modifier
                    .background(
                        Brush.linearGradient(listOf(Indigo, IndigoDark)),
                        RoundedCornerShape(10.dp)
                    )
                    .padding(horizontal = 30.dp, vertical = 10.dp)
            ) {
                Text(
                    text = stringResource(R.string.boot_retry_now),
                    style = MaterialTheme.typography.labelLarge,
                    color = Color.White
                )
            }
        }
    }
}
