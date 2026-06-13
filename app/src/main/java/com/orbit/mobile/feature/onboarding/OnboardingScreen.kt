package com.orbit.mobile.feature.onboarding

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.ChivoMonoFamily
import com.orbit.mobile.core.theme.SpaceGroteskFamily
import com.orbit.mobile.core.ui.components.OrbitLogo
import com.orbit.mobile.core.ui.components.OrbitLogoVariant
import kotlinx.coroutines.launch

// Landing palette
private val LandingBg = Color(0xFF05050D)
private val Blue = Color(0xFF1A6FFF)
private val Blue2 = Color(0xFF00AAFF)
private val BlueRing = Color(0xFF4488FF)
private val LaunchEnd = Color(0xFF0A3FFF)
private val Purple = Color(0xFF8B5CF6)
private val Amber = Color(0xFFF59E0B)
private val Slate400 = Color(0xFF94A3B8)
private val Slate600 = Color(0xFF475569)

// Hero gradient
private val HeroBrush = Brush.linearGradient(listOf(Blue, Blue2, Purple))

// Hero display
private val HeroStyle = TextStyle(
    fontFamily = SpaceGroteskFamily,
    fontWeight = FontWeight.Bold,
    fontSize = 38.sp,
    lineHeight = 42.sp,
    letterSpacing = (-0.02).em
)

// Feature model
private data class FeaturePage(
    val emoji: String,
    val accent: Color,
    val tagRes: Int,
    val titleRes: Int,
    val descRes: Int
)

private val featurePages = listOf(
    FeaturePage("🌐", Blue, R.string.onb_f1_tag, R.string.onb_f1_title, R.string.onb_f1_desc),
    FeaturePage("🧠", Purple, R.string.onb_f2_tag, R.string.onb_f2_title, R.string.onb_f2_desc),
    FeaturePage("📊", Amber, R.string.onb_f3_tag, R.string.onb_f3_title, R.string.onb_f3_desc)
)

private const val PAGE_COUNT = 4

// Onboarding flow
@Composable
fun OnboardingScreen(
    onFinish: () -> Unit,
    viewModel: OnboardingViewModel = hiltViewModel()
) {
    val pagerState = rememberPagerState(pageCount = { PAGE_COUNT })
    val scope = rememberCoroutineScope()
    val lastPage = pagerState.currentPage == PAGE_COUNT - 1

    // Finish flow
    fun finish() = viewModel.finish(onFinish)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(LandingBg)
    ) {
        // Glow top-left
        Box(
            modifier = Modifier
                .size(420.dp)
                .offset(x = (-150).dp, y = (-120).dp)
                .background(
                    Brush.radialGradient(
                        listOf(Blue.copy(alpha = 0.10f), Color.Transparent)
                    ),
                    CircleShape
                )
        )
        // Glow bottom-right
        Box(
            modifier = Modifier
                .size(420.dp)
                .align(Alignment.BottomEnd)
                .offset(x = 150.dp, y = 120.dp)
                .background(
                    Brush.radialGradient(
                        listOf(Purple.copy(alpha = 0.08f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            // Top bar
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 22.dp, vertical = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Brand logo
                OrbitLogo(variant = OrbitLogoVariant.Mark, modifier = Modifier.size(26.dp))
                Spacer(Modifier.size(9.dp))
                // Brand word
                Text(
                    text = stringResource(R.string.app_name),
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.Bold,
                    fontSize = 19.sp,
                    color = Color.White,
                    letterSpacing = (-0.01).em
                )
                Spacer(Modifier.weight(1f))
                // Skip button
                if (!lastPage) {
                    Surface(onClick = { finish() }, color = Color.Transparent) {
                        Text(
                            text = stringResource(R.string.onb_skip),
                            fontFamily = ChivoMonoFamily,
                            fontSize = 12.sp,
                            color = Slate400,
                            letterSpacing = 0.08.em,
                            modifier = Modifier.padding(6.dp)
                        )
                    }
                }
            }

            // Pages pager
            HorizontalPager(
                state = pagerState,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
            ) { page ->
                if (page == 0) HeroPage() else FeatureContent(featurePages[page - 1])
            }

            // Page dots
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 22.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                repeat(PAGE_COUNT) { i ->
                    val selected = i == pagerState.currentPage
                    // Dot width
                    val dotWidth by animateDpAsState(
                        targetValue = if (selected) 26.dp else 7.dp,
                        animationSpec = tween(260),
                        label = "dot$i"
                    )
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 3.dp)
                            .height(7.dp)
                            .width(dotWidth)
                            .clip(CircleShape)
                            .background(if (selected) Blue else Color.White.copy(alpha = 0.18f))
                    )
                }
            }

            // Cta button
            Box(modifier = Modifier.padding(horizontal = 24.dp, vertical = 20.dp)) {
                LaunchButton(
                    text = if (lastPage) stringResource(R.string.onb_launch)
                    else stringResource(R.string.onb_next),
                    showArrow = lastPage,
                    onClick = {
                        if (lastPage) finish()
                        else scope.launch {
                            pagerState.animateScrollToPage(pagerState.currentPage + 1)
                        }
                    }
                )
            }
        }
    }
}

// Hero page
@Composable
private fun HeroPage() {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 30.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Spacer(Modifier.height(8.dp))
        // Orbit core
        OrbitCore(dimen = 188.dp)
        Spacer(Modifier.height(34.dp))

        // Status badge
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Pulse dot
            val t = rememberInfiniteTransition(label = "badge")
            val ping by t.animateFloat(
                initialValue = 0.4f,
                targetValue = 1f,
                animationSpec = infiniteRepeatable(tween(1400)),
                label = "ping"
            )
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(Blue.copy(alpha = ping), CircleShape)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = stringResource(R.string.onb_badge).uppercase(),
                fontFamily = ChivoMonoFamily,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                color = Slate600,
                letterSpacing = 0.16.em
            )
        }
        Spacer(Modifier.height(18.dp))

        // Hero line 1
        Text(
            text = stringResource(R.string.onb_hero_line1),
            style = HeroStyle,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        // Hero line 2
        Text(
            text = stringResource(R.string.onb_hero_line2),
            style = HeroStyle.copy(brush = HeroBrush),
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(16.dp))

        // Hero desc
        Text(
            text = stringResource(R.string.onb_hero_desc),
            fontFamily = SpaceGroteskFamily,
            fontSize = 15.sp,
            lineHeight = 24.sp,
            color = Slate400,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(28.dp))

        // Stats row
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            StatItem(stringResource(R.string.onb_stat_teams_val), stringResource(R.string.onb_stat_teams))
            StatItem(stringResource(R.string.onb_stat_projects_val), stringResource(R.string.onb_stat_projects))
            StatItem(stringResource(R.string.onb_stat_uptime_val), stringResource(R.string.onb_stat_uptime))
            StatItem(stringResource(R.string.onb_stat_ai_val), stringResource(R.string.onb_stat_ai))
        }
        Spacer(Modifier.height(8.dp))
    }
}

// Stat cell
@Composable
private fun StatItem(value: String, label: String) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        Text(
            text = value,
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 18.sp,
            color = Color.White
        )
        Spacer(Modifier.height(3.dp))
        Text(
            text = label,
            fontSize = 10.sp,
            color = Slate600,
            letterSpacing = 0.03.em
        )
    }
}

// Feature page
@Composable
private fun FeatureContent(page: FeaturePage) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 36.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // Glow icon
        Box(contentAlignment = Alignment.Center) {
            // Soft glow
            Box(
                modifier = Modifier
                    .size(168.dp)
                    .background(
                        Brush.radialGradient(listOf(page.accent.copy(alpha = 0.18f), Color.Transparent)),
                        CircleShape
                    )
            )
            // Glass disk
            Box(
                modifier = Modifier
                    .size(110.dp)
                    .background(Color.White.copy(alpha = 0.03f), CircleShape)
                    .border(1.dp, page.accent.copy(alpha = 0.35f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = page.emoji, fontSize = 46.sp)
            }
        }
        Spacer(Modifier.height(40.dp))

        // Tag pill
        Text(
            text = stringResource(page.tagRes),
            fontFamily = ChivoMonoFamily,
            fontSize = 10.sp,
            fontWeight = FontWeight.SemiBold,
            color = page.accent,
            letterSpacing = 0.1.em,
            modifier = Modifier
                .background(page.accent.copy(alpha = 0.15f), RoundedCornerShape(5.dp))
                .padding(horizontal = 9.dp, vertical = 4.dp)
        )
        Spacer(Modifier.height(18.dp))

        // Feature title
        Text(
            text = stringResource(page.titleRes),
            fontFamily = SpaceGroteskFamily,
            fontWeight = FontWeight.Bold,
            fontSize = 27.sp,
            lineHeight = 32.sp,
            letterSpacing = (-0.01).em,
            color = Color.White,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(14.dp))

        // Feature desc
        Text(
            text = stringResource(page.descRes),
            fontFamily = SpaceGroteskFamily,
            fontSize = 15.sp,
            lineHeight = 25.sp,
            color = Slate400,
            textAlign = TextAlign.Center
        )
    }
}

// Orbit visual
@Composable
private fun OrbitCore(dimen: Dp) {
    val t = rememberInfiniteTransition(label = "orbitCore")
    val a1 by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(28000, easing = LinearEasing)), label = "a1"
    )
    val a2 by t.animateFloat(
        360f, 0f,
        infiniteRepeatable(tween(18000, easing = LinearEasing)), label = "a2"
    )
    val a3 by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(12000, easing = LinearEasing)), label = "a3"
    )
    val arc by t.animateFloat(
        0f, 360f,
        infiniteRepeatable(tween(6000, easing = LinearEasing)), label = "arc"
    )

    Box(modifier = Modifier.size(dimen), contentAlignment = Alignment.Center) {
        // Core glow
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.radialGradient(listOf(Blue.copy(alpha = 0.15f), Color.Transparent)),
                    CircleShape
                )
        )
        // Outer ring
        OrbitRing(dimen, Blue, 0.18f, a1)
        // Middle ring
        OrbitRing(dimen * 0.74f, BlueRing, 0.22f, a2)
        // Inner ring
        OrbitRing(dimen * 0.5f, Purple, 0.28f, a3)

        // Logo core
        Box(contentAlignment = Alignment.Center) {
            Canvas(modifier = Modifier.size(70.dp)) {
                val sw = 1.5.dp.toPx()
                val r = size.minDimension / 2 - sw
                drawCircle(color = Blue.copy(alpha = 0.4f), radius = r, style = Stroke(sw))
                drawArc(
                    color = Blue.copy(alpha = 0.9f),
                    startAngle = arc,
                    sweepAngle = 90f,
                    useCenter = false,
                    style = Stroke(sw, cap = StrokeCap.Round),
                    topLeft = Offset(sw, sw),
                    size = Size(size.width - sw * 2, size.height - sw * 2)
                )
            }
            OrbitLogo(variant = OrbitLogoVariant.Mark, modifier = Modifier.size(54.dp))
        }
    }
}

// Spinning ring
@Composable
private fun OrbitRing(diameter: Dp, color: Color, ringAlpha: Float, angle: Float) {
    Box(
        modifier = Modifier
            .size(diameter)
            .rotate(angle),
        contentAlignment = Alignment.TopCenter
    ) {
        // Ring line
        Box(
            modifier = Modifier
                .fillMaxSize()
                .border(1.dp, color.copy(alpha = ringAlpha), CircleShape)
        )
        // Node glow
        Box(
            modifier = Modifier
                .offset(y = (-6).dp)
                .size(13.dp)
                .background(color.copy(alpha = 0.3f), CircleShape),
            contentAlignment = Alignment.Center
        ) {
            // Node dot
            Box(
                modifier = Modifier
                    .size(7.dp)
                    .background(color, CircleShape)
            )
        }
    }
}

// Gradient cta
@Composable
private fun LaunchButton(text: String, showArrow: Boolean, onClick: () -> Unit) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(52.dp),
        shape = RoundedCornerShape(50),
        color = Color.Transparent
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.linearGradient(listOf(Blue, LaunchEnd)),
                    RoundedCornerShape(50)
                ),
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = text,
                    fontFamily = SpaceGroteskFamily,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 15.sp,
                    color = Color.White
                )
                if (showArrow) {
                    Spacer(Modifier.size(8.dp))
                    Text(
                        text = "→",
                        fontSize = 15.sp,
                        color = Color.White.copy(alpha = 0.85f)
                    )
                }
            }
        }
    }
}
