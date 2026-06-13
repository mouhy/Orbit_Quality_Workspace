package com.orbit.mobile.feature.workspace

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
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.PersonAdd
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.priorityColor
import com.orbit.mobile.core.theme.roleColor
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitButtonVariant
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.ui.components.StatusDot
import com.orbit.mobile.data.dto.BoardTaskDto
import com.orbit.mobile.data.dto.BoardTaskPatchRequest
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.TinyPill

// Detail tabs
private enum class DetailTab { INFO, DISCUSSION, FILES, AI }

// Task detail
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TaskDetailSheet(
    task: BoardTaskDto,
    viewModel: WorkspaceViewModel,
    onDismiss: () -> Unit,
    onDelete: () -> Unit,
    onDownloadResource: (String, String) -> Unit,
    onDownloadAttachment: (String, String, String) -> Unit
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var tab by remember { mutableStateOf(DetailTab.INFO) }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        Column(
            modifier = Modifier
                .padding(horizontal = 16.dp)
                .padding(bottom = 24.dp)
        ) {
            // Header
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.titleMedium,
                        color = colors.textPrimary
                    )
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TinyPill(columnLabel(task.statusKey), columnColor(task.statusKey))
                        task.priority?.let { TinyPill(it.uppercase(), priorityColor(it)) }
                    }
                }
                IconButton(onClick = onDelete) {
                    Icon(
                        imageVector = Icons.Outlined.Delete,
                        contentDescription = stringResource(R.string.action_delete),
                        tint = OrbitDanger,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))

            // Tabs
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                DetailTab.entries.forEach { t ->
                    val active = tab == t
                    val label = when (t) {
                        DetailTab.INFO -> stringResource(R.string.ws_tab_info)
                        DetailTab.DISCUSSION -> stringResource(R.string.ws_tab_discussion)
                        DetailTab.FILES -> stringResource(R.string.ws_panel_files)
                        DetailTab.AI -> stringResource(R.string.ws_panel_ai)
                    }
                    Surface(
                        onClick = { tab = t },
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) OrbitPrimary else colors.surface2,
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            if (active) OrbitPrimary else colors.borderStrong
                        )
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else colors.textSecondary,
                            modifier = Modifier.padding(horizontal = 11.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            when (tab) {
                DetailTab.INFO -> InfoTab(task, viewModel)
                DetailTab.DISCUSSION -> ChatThread(
                    comments = state.board?.comments ?: emptyList(),
                    members = state.board?.members ?: emptyList(),
                    sending = state.sendingComment,
                    canDelete = viewModel::canDelete,
                    onSend = viewModel::sendComment,
                    onDeleteComment = viewModel::deleteComment,
                    onDownloadAttachment = onDownloadAttachment
                )
                DetailTab.FILES -> Column(
                    modifier = Modifier
                        .heightIn(max = 420.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    FilesList(
                        resources = state.board?.resources ?: emptyList(),
                        canDelete = viewModel::canDelete,
                        onUpload = {},
                        onDownload = { onDownloadResource(it.id, it.fileName) },
                        onDelete = viewModel::deleteResource
                    )
                }
                DetailTab.AI -> Column(
                    modifier = Modifier
                        .heightIn(max = 460.dp)
                        .verticalScroll(rememberScrollState())
                ) {
                    com.orbit.mobile.feature.aireview.AiReviewTab(
                        taskId = task.id,
                        taskTitle = task.title,
                        taskDescription = task.description ?: "",
                        reportType = task.reportType
                    )
                }
            }
        }
    }
}

// Info tab
@Composable
private fun InfoTab(task: BoardTaskDto, viewModel: WorkspaceViewModel) {
    val colors = OrbitTheme.colors

    Column(
        modifier = Modifier
            .heightIn(max = 460.dp)
            .verticalScroll(rememberScrollState())
    ) {
        // Description
        Text(
            text = stringResource(R.string.pj_desc_label).uppercase(),
            style = OrbitTextStyles.techLabel,
            color = colors.textMuted
        )
        Spacer(Modifier.height(4.dp))
        Text(
            text = task.description?.ifBlank { null }
                ?: stringResource(R.string.pj_no_description),
            style = MaterialTheme.typography.bodySmall,
            color = colors.textSecondary
        )
        Spacer(Modifier.height(14.dp))

        // Status move
        Text(
            text = stringResource(R.string.pj_status_label).uppercase(),
            style = OrbitTextStyles.techLabel,
            color = colors.textMuted
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            BOARD_COLUMNS.forEach { column ->
                val active = task.statusKey == column
                Surface(
                    onClick = { if (!active) viewModel.moveTask(task, column) },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) columnColor(column)
                    else columnColor(column).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = columnLabel(column),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else columnColor(column),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Priority pick
        Text(
            text = stringResource(R.string.ct_priority_label).uppercase(),
            style = OrbitTextStyles.techLabel,
            color = colors.textMuted
        )
        Spacer(Modifier.height(6.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
            listOf("urgent", "high", "medium", "low").forEach { p ->
                val active = task.priority == p
                Surface(
                    onClick = {
                        viewModel.patchTask(task.id, BoardTaskPatchRequest(priority = p))
                    },
                    shape = RoundedCornerShape(18.dp),
                    color = if (active) priorityColor(p) else priorityColor(p).copy(alpha = 0.1f)
                ) {
                    Text(
                        text = p.uppercase(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = if (active) Color.White else priorityColor(p),
                        modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                    )
                }
            }
        }
        Spacer(Modifier.height(14.dp))

        // Meta rows
        MetaRow(
            label = stringResource(R.string.ws_meta_assignee),
            value = task.assignee
        )
        MetaRow(
            label = stringResource(R.string.ws_meta_due),
            value = task.due
        )
        MetaRow(
            label = stringResource(R.string.ws_meta_visibility),
            value = task.visibility ?: "team"
        )
        task.reportType?.let {
            MetaRow(label = stringResource(R.string.ct_report_type_label), value = it)
        }
        if (task.completedByNames.isNotEmpty()) {
            MetaRow(
                label = stringResource(R.string.ws_meta_completed_by),
                value = task.completedByNames.joinToString(", ")
            )
        }
        task.submittedByName?.let {
            MetaRow(label = stringResource(R.string.ws_meta_submitted_by), value = it)
        }
    }
}

// Meta row
@Composable
private fun MetaRow(label: String, value: String) {
    val colors = OrbitTheme.colors
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = colors.textMuted,
            modifier = Modifier.width(110.dp)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = colors.textPrimary
        )
    }
}

// Team panel
@Composable
fun TeamPanel(viewModel: WorkspaceViewModel) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    var search by remember { mutableStateOf("") }
    var showAdd by remember { mutableStateOf(false) }
    var editingMember by remember { mutableStateOf<String?>(null) }
    var responsibility by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .padding(bottom = 24.dp)
            .heightIn(max = 540.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = stringResource(R.string.ws_panel_team),
                style = MaterialTheme.typography.titleMedium,
                color = colors.textPrimary,
                modifier = Modifier.weight(1f)
            )
            OrbitButton(
                text = stringResource(R.string.ws_add_member),
                onClick = { showAdd = !showAdd },
                variant = OrbitButtonVariant.Secondary,
                leadingIcon = Icons.Outlined.PersonAdd
            )
        }
        Spacer(Modifier.height(10.dp))

        // Add panel
        if (showAdd) {
            OrbitTextField(
                value = search,
                onValueChange = {
                    search = it
                    viewModel.loadAvailableMembers(it.ifBlank { null })
                },
                placeholder = stringResource(R.string.ct_search_members),
                leading = {
                    Icon(
                        imageVector = Icons.Outlined.Search,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(15.dp)
                    )
                }
            )
            Spacer(Modifier.height(6.dp))
            state.availableMembers.take(8).forEach { candidate ->
                Surface(
                    onClick = {
                        viewModel.addMember(candidate.userId)
                        showAdd = false
                    },
                    color = Color.Transparent
                ) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(vertical = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HashAvatar(name = candidate.name, size = 28.dp)
                        Spacer(Modifier.width(8.dp))
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = candidate.name,
                                style = MaterialTheme.typography.labelMedium,
                                color = colors.textPrimary
                            )
                            Text(
                                text = candidate.email ?: "",
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                        }
                        TinyPill(candidate.role, roleColor(candidate.role))
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
        }

        // Members list
        state.board?.members?.forEach { member ->
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(12.dp))
                    .padding(10.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Box {
                        HashAvatar(name = member.name, size = 36.dp)
                        if (member.online) {
                            Box(
                                modifier = Modifier
                                    .align(Alignment.BottomEnd)
                            ) {
                                StatusDot(online = true, size = 11.dp)
                            }
                        }
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = member.name,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                        Text(
                            text = member.responsibility?.ifBlank { null }
                                ?: stringResource(R.string.ws_no_responsibility),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    TinyPill(member.role, roleColor(member.role))
                    Spacer(Modifier.width(4.dp))
                    member.userId?.let { uid ->
                        IconButton(
                            onClick = {
                                editingMember = if (editingMember == uid) null else uid
                                responsibility = member.responsibility ?: ""
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.PersonAdd,
                                contentDescription = stringResource(R.string.action_edit),
                                tint = colors.textSecondary,
                                modifier = Modifier.size(14.dp)
                            )
                        }
                    }
                }
                if (editingMember != null && editingMember == member.userId) {
                    Spacer(Modifier.height(8.dp))
                    OrbitTextField(
                        value = responsibility,
                        onValueChange = { responsibility = it },
                        placeholder = stringResource(R.string.ws_responsibility_placeholder)
                    )
                    Spacer(Modifier.height(6.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                        OrbitButton(
                            text = stringResource(R.string.action_save),
                            onClick = {
                                member.userId?.let {
                                    viewModel.updateResponsibility(it, responsibility)
                                }
                                editingMember = null
                            },
                            modifier = Modifier.weight(1f)
                        )
                        OrbitButton(
                            text = stringResource(R.string.action_remove),
                            onClick = {
                                member.userId?.let { viewModel.removeMember(it) }
                                editingMember = null
                            },
                            variant = OrbitButtonVariant.Danger,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }
        if (state.board?.members.isNullOrEmpty()) {
            Text(
                text = stringResource(R.string.tm_no_members),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 14.dp)
            )
        }
    }
}
