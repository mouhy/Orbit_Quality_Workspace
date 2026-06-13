package com.orbit.mobile.feature.aireview

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.data.dto.AiHistoryItemDto
import com.orbit.mobile.domain.repository.QualityRepository
import com.orbit.mobile.feature.dashboard.TinyPill
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.math.roundToInt

// History VM
@HiltViewModel
class AiHistoryViewModel @Inject constructor(
    private val repo: QualityRepository
) : ViewModel() {

    private val _items = MutableStateFlow<List<AiHistoryItemDto>>(emptyList())
    val items: StateFlow<List<AiHistoryItemDto>> = _items

    init {
        refresh()
    }

    // Load history
    fun refresh() {
        viewModelScope.launch {
            repo.history().onSuccess { _items.value = it }
        }
    }
}

// AI tab
@Composable
fun AiReviewTab(
    taskId: String?,
    taskTitle: String,
    taskDescription: String,
    reportType: String?,
    historyViewModel: AiHistoryViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val history by historyViewModel.items.collectAsStateWithLifecycle()
    var showSubmit by remember { mutableStateOf(false) }

    LaunchedEffect(showSubmit) {
        if (!showSubmit) historyViewModel.refresh()
    }

    Column {
        OrbitButton(
            text = stringResource(R.string.ai_submit_button),
            onClick = { showSubmit = true },
            fullWidth = true
        )
        Spacer(Modifier.height(14.dp))

        Text(
            text = stringResource(R.string.ai_history_title).uppercase(),
            style = OrbitTextStyles.techLabel,
            color = colors.textMuted
        )
        Spacer(Modifier.height(6.dp))
        if (history.isEmpty()) {
            Text(
                text = stringResource(R.string.ai_no_history),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 10.dp)
            )
        }
        history.forEach { item ->
            val score = item.score.roundToInt()
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                ProgressRing(
                    pct = score,
                    color = if (score >= UI_PASS_SCORE) OrbitSuccess else OrbitDanger,
                    size = 38.dp,
                    thick = 3.dp
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = item.taskTitle,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = timeAgo(item.timestamp).asString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                TinyPill(item.status.replace("_", " "), colors.textSecondary)
            }
        }
    }

    if (showSubmit) {
        TaskSubmitSheet(
            taskTitle = taskTitle,
            taskDescription = taskDescription,
            taskId = taskId,
            initialReportType = reportType,
            onDismiss = { showSubmit = false },
            onAutoSubmitted = { showSubmit = false }
        )
    }
}
