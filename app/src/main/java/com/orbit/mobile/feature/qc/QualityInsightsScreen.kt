package com.orbit.mobile.feature.qc

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.AreaLineChart
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.util.Downloader
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.DashCardHeader
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import kotlinx.coroutines.launch
import kotlin.math.roundToInt

// Insights page
@Composable
fun QualityInsightsScreen(
    viewModel: QcViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()

    // 60-day window
    LaunchedEffect(Unit) { viewModel.refresh(days = 60) }

    fun export(format: String) {
        scope.launch {
            val body = viewModel.export(format) ?: return@launch
            val file = Downloader.saveToCache(context, body, "quality_insights.$format")
            Downloader.open(context, file)
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh(days = 60) } }

        Text(
            text = stringResource(R.string.qi_subtitle),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textMuted
        )

        when {
            state.loading -> LoadingHint()
            else -> {
                DashCard {
                    DashCardHeader(
                        title = stringResource(R.string.qc_trend_title),
                        subtitle = stringResource(R.string.qi_trend_sub)
                    )
                    val trend = state.overview?.scoreTrend ?: emptyList()
                    if (trend.size >= 2) {
                        AreaLineChart(
                            data = trend.map { it.avgScore.roundToInt() },
                            labels = trend.takeLast(6).map { it.date.takeLast(5) },
                            color = OrbitSuccess,
                            height = 110.dp
                        )
                    } else {
                        Text(
                            text = stringResource(R.string.tp_no_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                }

                DashCard {
                    DashCardHeader(title = stringResource(R.string.qc_bottlenecks_title))
                    val bottlenecks = state.overview?.bottlenecks ?: emptyList()
                    if (bottlenecks.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tp_no_data),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    bottlenecks.take(8).forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = item.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            TinyPill(
                                stringResource(R.string.qc_hours, item.avgHours.roundToInt()),
                                OrbitWarning
                            )
                        }
                    }
                }

                DashCard {
                    DashCardHeader(title = stringResource(R.string.qi_download_title))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OrbitButton(
                            text = stringResource(R.string.qc_export_csv),
                            onClick = { export("csv") },
                            variant = OrbitButtonVariant.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                        OrbitButton(
                            text = stringResource(R.string.qc_export_pdf),
                            onClick = { export("pdf") },
                            variant = OrbitButtonVariant.Secondary,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
    }
}
