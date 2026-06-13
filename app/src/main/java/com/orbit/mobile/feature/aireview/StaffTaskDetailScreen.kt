package com.orbit.mobile.feature.aireview

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.orbit.mobile.R
import com.orbit.mobile.core.network.ApiResult
import com.orbit.mobile.core.network.onFailure
import com.orbit.mobile.core.network.onSuccess
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.priorityColor
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.util.Downloader
import com.orbit.mobile.core.util.UiText
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.toUiText
import com.orbit.mobile.data.dto.TaskDetailDto
import com.orbit.mobile.domain.repository.QualityRepository
import com.orbit.mobile.feature.dashboard.DashCard
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.dashboard.dashStatusColor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject

// Staff task state
data class StaffTaskState(
    val loading: Boolean = true,
    val error: UiText? = null,
    val task: TaskDetailDto? = null
)

// Staff task VM
@HiltViewModel
class StaffTaskViewModel @Inject constructor(
    private val repo: QualityRepository,
    savedStateHandle: SavedStateHandle
) : ViewModel() {

    val taskId: String = savedStateHandle.get<String>("taskId") ?: ""

    private val _state = MutableStateFlow(StaffTaskState())
    val state: StateFlow<StaffTaskState> = _state

    init {
        refresh()
    }

    // Load task
    fun refresh() {
        if (taskId.isBlank()) {
            _state.update { it.copy(loading = false) }
            return
        }
        _state.update { it.copy(loading = true, error = null) }
        viewModelScope.launch {
            repo.task(taskId)
                .onSuccess { task -> _state.update { it.copy(loading = false, task = task) } }
                .onFailure { e ->
                    _state.update { it.copy(loading = false, error = e.toUiText()) }
                }
        }
    }

    // Template body
    suspend fun template(key: String, lang: String) =
        (repo.downloadTemplate(key, lang) as? ApiResult.Success)?.data
}

// Staff task page
@Composable
fun StaffTaskDetailScreen(
    viewModel: StaffTaskViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var showSubmit by remember { mutableStateOf(false) }

    fun downloadTemplate(key: String, lang: String) {
        scope.launch {
            val body = viewModel.template(key, lang) ?: return@launch
            val file = Downloader.saveToCache(context, body, "${key}_$lang.docx")
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
        state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

        when {
            state.loading -> LoadingHint()
            state.task != null -> {
                val task = state.task!!

                DashCard {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        TinyPill(task.status.replace("_", " "), dashStatusColor(task.status))
                        TinyPill(task.priority.uppercase(), priorityColor(task.priority))
                        task.reportType?.let { TinyPill(it, OrbitPrimary) }
                    }
                    Spacer(Modifier.height(10.dp))
                    Text(
                        text = task.description.ifBlank {
                            stringResource(R.string.pj_no_description)
                        },
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                    task.deadline?.let {
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "${stringResource(R.string.ws_meta_due)}: ${it.take(10)}",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }

                // Templates
                task.reportType?.let { key ->
                    DashCard {
                        Text(
                            text = stringResource(R.string.ai_template_title).uppercase(),
                            style = OrbitTextStyles.techLabel,
                            color = colors.textMuted
                        )
                        Spacer(Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.ai_template_sub),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textSecondary
                        )
                        Spacer(Modifier.height(10.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            OrbitButton(
                                text = stringResource(R.string.ai_template_ar),
                                onClick = { downloadTemplate(key, "ar") },
                                variant = OrbitButtonVariant.Secondary,
                                leadingIcon = Icons.Outlined.Download,
                                modifier = Modifier.weight(1f)
                            )
                            OrbitButton(
                                text = stringResource(R.string.ai_template_en),
                                onClick = { downloadTemplate(key, "en") },
                                variant = OrbitButtonVariant.Secondary,
                                leadingIcon = Icons.Outlined.Download,
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }

                // Verdict
                DashCard {
                    Text(
                        text = stringResource(R.string.ai_verdict_title).uppercase(),
                        style = OrbitTextStyles.techLabel,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    val statusUpper = task.status.uppercase()
                    when {
                        statusUpper == "QC_REVIEW" -> TinyPill(
                            stringResource(R.string.ai_verdict_pending),
                            OrbitPrimary
                        )
                        statusUpper == "DONE" || statusUpper == "COMPLETED" -> TinyPill(
                            stringResource(R.string.ai_verdict_accepted),
                            OrbitSuccess
                        )
                        else -> Text(
                            text = stringResource(R.string.ai_verdict_none),
                            style = MaterialTheme.typography.bodySmall,
                            color = colors.textMuted
                        )
                    }
                    Spacer(Modifier.height(12.dp))
                    OrbitButton(
                        text = stringResource(R.string.ai_submit_button),
                        onClick = { showSubmit = true },
                        fullWidth = true
                    )
                }
            }
        }
    }

    if (showSubmit && state.task != null) {
        TaskSubmitSheet(
            taskTitle = state.task!!.title,
            taskDescription = state.task!!.description,
            taskId = state.task!!.id,
            initialReportType = state.task!!.reportType,
            onDismiss = { showSubmit = false },
            onAutoSubmitted = {
                showSubmit = false
                viewModel.refresh()
            }
        )
    }
}
