package com.orbit.mobile.feature.aireview

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Image
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.charts.ProgressRing
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.data.dto.EvaluationResultDto
import com.orbit.mobile.data.dto.ReportTypeDto
import com.orbit.mobile.feature.auth.AuthErrorBox
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.tasks.AnalysisSection
import com.orbit.mobile.feature.workspace.readUploadPart
import kotlinx.coroutines.delay
import java.util.Locale
import kotlin.math.roundToInt

// Locale name
fun ReportTypeDto.localizedName(): String =
    if (Locale.getDefault().language == "ar") nameAr.ifBlank { nameEn } else nameEn.ifBlank { nameAr }

// Submit sheet
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskSubmitSheet(
    taskTitle: String,
    taskDescription: String,
    taskId: String?,
    initialReportType: String?,
    onDismiss: () -> Unit,
    onAutoSubmitted: () -> Unit,
    viewModel: TaskSubmitViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val context = LocalContext.current
    val state by viewModel.state.collectAsStateWithLifecycle()
    var typeListOpen by remember { mutableStateOf(false) }

    LaunchedEffect(initialReportType) {
        if (initialReportType != null && state.selectedType == null) {
            viewModel.selectType(initialReportType)
        }
    }

    val filePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addFiles(uris.mapNotNull { readUploadPart(context, it) })
    }
    val imagePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        viewModel.addImages(uris.mapNotNull { readUploadPart(context, it) })
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 18.dp)
                .padding(bottom = 26.dp)
                .heightIn(max = (LocalConfiguration.current.screenHeightDp * 0.85f).dp)
                .verticalScroll(rememberScrollState())
        ) {
            Text(
                text = stringResource(R.string.ai_submit_title),
                style = MaterialTheme.typography.headlineSmall,
                color = colors.textPrimary
            )
            Text(
                text = taskTitle,
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            Spacer(Modifier.height(14.dp))

            when {
                state.result != null -> ResultView(
                    result = state.result!!,
                    onRetry = viewModel::reset,
                    onAutoSubmitted = onAutoSubmitted
                )
                else -> {
                    // Report type
                    Text(
                        text = stringResource(R.string.ai_report_type).uppercase(),
                        style = OrbitTextStyles.techLabel,
                        color = colors.textMuted
                    )
                    Spacer(Modifier.height(6.dp))
                    val selected = state.reportTypes.firstOrNull { it.key == state.selectedType }
                    Surface(
                        onClick = { typeListOpen = !typeListOpen },
                        shape = RoundedCornerShape(10.dp),
                        color = if (colors.isDark) colors.surface2 else colors.surface,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            colors.borderStrong
                        )
                    ) {
                        Text(
                            text = selected?.localizedName()
                                ?: stringResource(R.string.ai_pick_type),
                            style = MaterialTheme.typography.bodyMedium,
                            color = if (selected == null) colors.textMuted else colors.textPrimary,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 12.dp, vertical = 12.dp)
                        )
                    }
                    if (typeListOpen) {
                        Column(
                            modifier = Modifier
                                .fillMaxWidth()
                                .heightIn(max = 240.dp)
                                .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
                                .padding(4.dp)
                                .verticalScroll(rememberScrollState())
                        ) {
                            state.reportTypes.forEach { type ->
                                Surface(
                                    onClick = {
                                        viewModel.selectType(type.key)
                                        typeListOpen = false
                                    },
                                    color = if (type.key == state.selectedType) {
                                        OrbitPrimary.copy(alpha = 0.08f)
                                    } else Color.Transparent,
                                    shape = RoundedCornerShape(8.dp)
                                ) {
                                    Column(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(horizontal = 10.dp, vertical = 7.dp)
                                    ) {
                                        Text(
                                            text = type.localizedName(),
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.SemiBold,
                                            color = colors.textPrimary
                                        )
                                        Text(
                                            text = type.description,
                                            style = MaterialTheme.typography.labelSmall,
                                            color = colors.textMuted,
                                            maxLines = 1,
                                            overflow = TextOverflow.Ellipsis
                                        )
                                    }
                                }
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // Files
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = stringResource(R.string.ai_files_label).uppercase(),
                            style = OrbitTextStyles.techLabel,
                            color = colors.textMuted,
                            modifier = Modifier.weight(1f)
                        )
                        Text(
                            text = "${state.files.size}/$MAX_AI_FILES",
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OrbitButton(
                            text = stringResource(R.string.ai_add_files),
                            onClick = { filePicker.launch("*/*") },
                            variant = OrbitButtonVariant.Secondary,
                            leadingIcon = Icons.Outlined.AttachFile,
                            enabled = state.files.size < MAX_AI_FILES,
                            modifier = Modifier.weight(1f)
                        )
                        OrbitButton(
                            text = stringResource(R.string.ai_add_images),
                            onClick = { imagePicker.launch("image/*") },
                            variant = OrbitButtonVariant.Secondary,
                            leadingIcon = Icons.Outlined.Image,
                            enabled = state.images.size < MAX_AI_IMAGES,
                            modifier = Modifier.weight(1f)
                        )
                    }
                    state.files.forEachIndexed { index, file ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 6.dp)
                                .border(1.dp, colors.border, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = null,
                                tint = OrbitPrimary,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = file.fileName,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary,
                                modifier = Modifier.weight(1f),
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            Text(
                                text = "%.1f MB".format(file.bytes.size / 1048576.0),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                            IconButton(
                                onClick = { viewModel.removeFile(index) },
                                modifier = Modifier.size(22.dp)
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Close,
                                    contentDescription = stringResource(R.string.action_remove),
                                    tint = colors.textMuted,
                                    modifier = Modifier.size(12.dp)
                                )
                            }
                        }
                    }
                    if (state.images.isNotEmpty()) {
                        Spacer(Modifier.height(6.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                            TinyPill(
                                stringResource(R.string.ai_images_count, state.images.size),
                                OrbitPrimary
                            )
                            Surface(
                                onClick = { viewModel.removeImage(state.images.lastIndex) },
                                color = Color.Transparent
                            ) {
                                Text(
                                    text = stringResource(R.string.action_remove),
                                    style = MaterialTheme.typography.labelSmall,
                                    color = OrbitDanger
                                )
                            }
                        }
                    }
                    Spacer(Modifier.height(14.dp))

                    // Notes
                    OrbitTextField(
                        value = state.notes,
                        onValueChange = viewModel::setNotes,
                        label = stringResource(R.string.ai_notes_label),
                        placeholder = stringResource(R.string.ai_notes_placeholder),
                        singleLine = false,
                        minLines = 2
                    )
                    Spacer(Modifier.height(12.dp))

                    state.error?.let {
                        AuthErrorBox(message = it.asString())
                        Spacer(Modifier.height(10.dp))
                    }

                    if (state.evaluating) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 10.dp),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(18.dp),
                                color = OrbitPrimary,
                                strokeWidth = 2.dp
                            )
                            Spacer(Modifier.width(10.dp))
                            Text(
                                text = stringResource(R.string.ai_evaluating),
                                style = MaterialTheme.typography.bodySmall,
                                color = colors.textSecondary
                            )
                        }
                    } else {
                        OrbitButton(
                            text = stringResource(R.string.ai_run_check),
                            onClick = {
                                viewModel.evaluate(taskTitle, taskDescription, taskId)
                            },
                            enabled = state.files.isNotEmpty() || state.images.isNotEmpty(),
                            fullWidth = true
                        )
                    }
                }
            }
        }
    }
}

// Result view
@Composable
private fun ResultView(
    result: EvaluationResultDto,
    onRetry: () -> Unit,
    onAutoSubmitted: () -> Unit
) {
    val colors = OrbitTheme.colors
    val score = result.complianceScore.roundToInt()
    val passed = score >= UI_PASS_SCORE
    var countdown by remember { mutableIntStateOf(3) }

    // Auto submit
    LaunchedEffect(passed) {
        if (passed) {
            countdown = 3
            while (countdown > 0) {
                delay(1_000)
                countdown--
            }
            onAutoSubmitted()
        }
    }

    Column {
        Row(verticalAlignment = Alignment.CenterVertically) {
            ProgressRing(
                pct = score,
                color = if (passed) OrbitSuccess else OrbitDanger,
                size = 84.dp,
                thick = 7.dp
            )
            Spacer(Modifier.width(14.dp))
            Column {
                Text(
                    text = if (passed) stringResource(R.string.ai_passed_title)
                    else stringResource(R.string.ai_failed_title),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (passed) OrbitSuccess else OrbitDanger
                )
                Text(
                    text = if (passed) {
                        stringResource(R.string.ai_auto_submit, countdown)
                    } else {
                        stringResource(R.string.ai_failed_sub)
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textSecondary
                )
            }
        }
        Spacer(Modifier.height(14.dp))

        // Compliance card
        result.reportTypeCompliance?.let { compliance ->
            val tone = when (compliance.isCompliant) {
                true -> OrbitSuccess
                false -> OrbitDanger
                null -> OrbitWarning
            }
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(tone.copy(alpha = 0.05f), RoundedCornerShape(10.dp))
                    .border(1.dp, tone.copy(alpha = 0.16f), RoundedCornerShape(10.dp))
                    .padding(10.dp)
            ) {
                Text(
                    text = stringResource(R.string.ai_type_compliance).uppercase(),
                    style = OrbitTextStyles.techLabel,
                    color = tone
                )
                compliance.complianceNote?.let {
                    Spacer(Modifier.height(3.dp))
                    Text(
                        text = it,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary
                    )
                }
                if (compliance.missingElements.isNotEmpty()) {
                    Spacer(Modifier.height(4.dp))
                    Text(
                        text = stringResource(
                            R.string.ai_missing_elements,
                            compliance.missingElements.joinToString("، ")
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = tone
                    )
                }
            }
            Spacer(Modifier.height(12.dp))
        }

        if (result.passedStandards.isNotEmpty()) {
            AnalysisSection(
                title = stringResource(R.string.tk_passed),
                items = result.passedStandards.map {
                    listOfNotNull(it.rule, it.result).joinToString(" — ")
                },
                tone = OrbitSuccess
            )
        }
        if (result.failedStandards.isNotEmpty()) {
            AnalysisSection(
                title = stringResource(R.string.tk_failed),
                items = result.failedStandards.map {
                    listOfNotNull(it.rule, it.reason).joinToString(" — ")
                },
                tone = OrbitDanger
            )
        }
        if (result.suggestions.isNotEmpty()) {
            AnalysisSection(
                title = stringResource(R.string.tk_suggestions),
                items = result.suggestions,
                tone = OrbitWarning
            )
        }

        if (!passed) {
            Spacer(Modifier.height(8.dp))
            OrbitButton(
                text = stringResource(R.string.action_retry),
                onClick = onRetry,
                variant = OrbitButtonVariant.Secondary,
                fullWidth = true
            )
        }
    }
}
