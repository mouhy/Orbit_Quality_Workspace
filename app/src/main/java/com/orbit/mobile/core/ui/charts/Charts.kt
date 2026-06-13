package com.orbit.mobile.core.ui.charts

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import kotlin.math.max

// Tiny sparkline
@Composable
fun Sparkline(
    data: List<Int>,
    color: Color,
    modifier: Modifier = Modifier
) {
    if (data.size < 2) {
        Box(modifier)
        return
    }
    Canvas(modifier = modifier) {
        val minV = data.min().toFloat()
        val maxV = data.max().toFloat()
        val range = (maxV - minV).takeIf { it != 0f } ?: 1f
        val stepX = size.width / (data.size - 1)
        val points = data.mapIndexed { i, v ->
            Offset(i * stepX, size.height - ((v - minV) / range) * (size.height - 4f) - 2f)
        }
        val line = Path().apply {
            moveTo(points[0].x, points[0].y)
            points.drop(1).forEach { lineTo(it.x, it.y) }
        }
        val area = Path().apply {
            addPath(line)
            lineTo(size.width, size.height)
            lineTo(0f, size.height)
            close()
        }
        drawPath(
            area,
            brush = Brush.verticalGradient(
                listOf(color.copy(alpha = 0.35f), Color.Transparent)
            )
        )
        drawPath(line, color, style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round))
        drawCircle(color, radius = 2.5.dp.toPx(), center = points.last())
    }
}

// Progress ring
@Composable
fun ProgressRing(
    pct: Int,
    color: Color,
    size: Dp = 44.dp,
    thick: Dp = 3.5.dp
) {
    val colors = OrbitTheme.colors
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val stroke = Stroke(width = thick.toPx(), cap = StrokeCap.Round)
            val radius = (this.size.minDimension - thick.toPx()) / 2
            drawCircle(
                color = colors.textMuted.copy(alpha = 0.15f),
                radius = radius,
                style = stroke
            )
            drawArc(
                color = color,
                startAngle = -90f,
                sweepAngle = 360f * (pct.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = stroke,
                topLeft = Offset(thick.toPx() / 2, thick.toPx() / 2),
                size = Size(
                    this.size.width - thick.toPx(),
                    this.size.height - thick.toPx()
                )
            )
        }
        Text(
            text = "$pct%",
            fontSize = (size.value * 0.2f).sp,
            fontWeight = FontWeight.ExtraBold,
            color = color
        )
    }
}

// Health gauge
@Composable
fun GaugeArc(
    score: Int,
    label: String,
    size: Dp = 120.dp
) {
    val colors = OrbitTheme.colors
    val tone = when {
        score >= 70 -> OrbitSuccess
        score >= 40 -> OrbitWarning
        else -> OrbitDanger
    }
    Box(
        modifier = Modifier.size(size, size * 0.75f),
        contentAlignment = Alignment.Center
    ) {
        Canvas(modifier = Modifier.size(size, size * 0.75f)) {
            val cx = this.size.width / 2
            val cy = this.size.width * 0.62f
            val r = this.size.width * 0.42f
            val thickPx = this.size.width * 0.09f
            // Track arc
            drawArc(
                color = colors.textMuted.copy(alpha = 0.15f),
                startAngle = 90f + 63f,
                sweepAngle = 360f - 126f,
                useCenter = false,
                style = Stroke(thickPx, cap = StrokeCap.Round),
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2)
            )
            // Score arc
            drawArc(
                color = tone,
                startAngle = 90f + 63f,
                sweepAngle = (360f - 126f) * (score.coerceIn(0, 100) / 100f),
                useCenter = false,
                style = Stroke(thickPx, cap = StrokeCap.Round),
                topLeft = Offset(cx - r, cy - r),
                size = Size(r * 2, r * 2)
            )
        }
        // Center gauge
        Column(
            modifier = Modifier.offset(y = size * 0.17f),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = score.toString(),
                fontSize = (size.value * 0.26f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = tone
            )
            Text(
                text = label,
                fontSize = (size.value * 0.085f).sp,
                fontWeight = FontWeight.SemiBold,
                color = colors.textMuted
            )
        }
    }
}

// Donut segment
data class DonutSegment(val value: Int, val color: Color)

// Mini donut
@Composable
fun MiniDonut(
    segments: List<DonutSegment>,
    centerLabel: String,
    centerSub: String,
    size: Dp = 100.dp
) {
    val colors = OrbitTheme.colors
    val total = max(segments.sumOf { it.value }, 1)
    Box(modifier = Modifier.size(size), contentAlignment = Alignment.Center) {
        Canvas(modifier = Modifier.size(size)) {
            val thickPx = this.size.width * 0.2f
            val r = (this.size.width - thickPx) / 2
            var startAngle = -90f
            segments.forEach { seg ->
                val sweep = 360f * (seg.value.toFloat() / total)
                if (sweep > 0f) {
                    drawArc(
                        color = seg.color,
                        startAngle = startAngle,
                        sweepAngle = max(sweep - 1f, 0.5f),
                        useCenter = false,
                        style = Stroke(thickPx),
                        topLeft = Offset(thickPx / 2, thickPx / 2),
                        size = Size(r * 2, r * 2)
                    )
                }
                startAngle += sweep
            }
        }
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                text = centerLabel,
                fontSize = (size.value * 0.17f).sp,
                fontWeight = FontWeight.ExtraBold,
                color = colors.textPrimary
            )
            Text(
                text = centerSub,
                fontSize = (size.value * 0.09f).sp,
                color = colors.textMuted
            )
        }
    }
}

// Bar data
data class BarEntry(val label: String, val value: Int, val color: Color)

// Vertical bars
@Composable
fun VerticalBarChart(
    bars: List<BarEntry>,
    modifier: Modifier = Modifier,
    height: Dp = 120.dp
) {
    val colors = OrbitTheme.colors
    if (bars.isEmpty()) {
        Box(modifier = modifier.height(height))
        return
    }
    val maxV = max(bars.maxOf { it.value }, 1)
    Row(
        modifier = modifier.height(height),
        horizontalArrangement = Arrangement.spacedBy(14.dp),
        verticalAlignment = Alignment.Bottom
    ) {
        bars.forEach { bar ->
            Column(
                modifier = Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = bar.value.toString(),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.ExtraBold,
                    color = bar.color
                )
                Box(
                    modifier = Modifier
                        .padding(top = 4.dp, bottom = 4.dp)
                        .fillMaxWidth()
                        .height(((height.value - 44) * bar.value / maxV).coerceAtLeast(2f).dp)
                        .background(bar.color.copy(alpha = 0.85f), RoundedCornerShape(6.dp))
                )
                Text(
                    text = bar.label,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}

// Area line
@Composable
fun AreaLineChart(
    data: List<Int>,
    labels: List<String>,
    color: Color,
    modifier: Modifier = Modifier,
    height: Dp = 100.dp
) {
    val colors = OrbitTheme.colors
    Column(modifier = modifier) {
        if (data.size >= 2) {
            Canvas(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(height)
            ) {
                val minV = data.min().toFloat()
                val maxV = data.max().toFloat()
                val range = (maxV - minV).takeIf { it != 0f } ?: 1f
                val stepX = size.width / (data.size - 1)
                val points = data.mapIndexed { i, v ->
                    Offset(
                        i * stepX,
                        size.height - ((v - minV) / range) * (size.height - 22f) - 4f
                    )
                }
                // Grid lines
                listOf(0.25f, 0.5f, 0.75f).forEach { f ->
                    val y = size.height - f * (size.height - 22f) - 4f
                    drawLine(
                        color = colors.textMuted.copy(alpha = 0.12f),
                        start = Offset(0f, y),
                        end = Offset(size.width, y),
                        strokeWidth = 1f
                    )
                }
                val line = Path().apply {
                    moveTo(points[0].x, points[0].y)
                    points.drop(1).forEach { lineTo(it.x, it.y) }
                }
                val area = Path().apply {
                    moveTo(0f, size.height)
                    points.forEach { lineTo(it.x, it.y) }
                    lineTo(size.width, size.height)
                    close()
                }
                drawPath(
                    area,
                    brush = Brush.verticalGradient(
                        listOf(color.copy(alpha = 0.28f), Color.Transparent)
                    )
                )
                drawPath(line, color, style = Stroke(2.5.dp.toPx(), cap = StrokeCap.Round))
                points.forEachIndexed { i, p ->
                    drawCircle(
                        color = color,
                        radius = if (i == points.lastIndex) 4.dp.toPx() else 2.5.dp.toPx(),
                        center = p,
                        alpha = if (i == points.lastIndex) 1f else 0.55f
                    )
                }
            }
        }
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            labels.forEach {
                Text(
                    text = it,
                    fontSize = 8.5.sp,
                    color = colors.textMuted
                )
            }
        }
    }
}

// Heat grid
@Composable
fun HeatGrid(
    data: List<List<Int>>,
    modifier: Modifier = Modifier,
    cellColor: Color = Color(0xFF6366F1),
    height: Dp = 110.dp
) {
    val colors = OrbitTheme.colors
    val maxV = max(data.flatten().maxOrNull() ?: 0, 1)
    val weeks = data.firstOrNull()?.size ?: 0

    Row(
        modifier = modifier
            .fillMaxWidth()
            .height(height),
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        repeat(weeks) { w ->
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(3.dp)
            ) {
                repeat(7) { d ->
                    val v = data.getOrNull(d)?.getOrNull(w) ?: 0
                    val alpha = if (v == 0) 0f else max(0.15f, v.toFloat() / maxV)
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .background(
                                if (v == 0) colors.textMuted.copy(alpha = 0.08f)
                                else cellColor.copy(alpha = alpha),
                                RoundedCornerShape(2.dp)
                            )
                    )
                }
            }
        }
    }
}

