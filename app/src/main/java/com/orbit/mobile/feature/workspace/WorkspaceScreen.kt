package com.orbit.mobile.feature.workspace

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Chat
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.AutoAwesome
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Group
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
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
import com.orbit.mobile.core.theme.priorityColor
import com.orbit.mobile.core.theme.projectStatusColor
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.ui.components.StaggeredAppear
import com.orbit.mobile.core.ui.components.StatusDot
import com.orbit.mobile.core.util.Downloader
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.parseInstant
import com.orbit.mobile.data.dto.BoardTaskDto
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.dashboard.daysUntil
import com.orbit.mobile.feature.projects.ConfirmDeleteDialog
import kotlinx.coroutines.launch

// Sheet kinds
private enum class BoardSheet { NONE, TEAM, CHAT, FILES, AI }

// Workspace page
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun WorkspaceScreen(
    viewModel: WorkspaceViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var sheet by remember { mutableStateOf(BoardSheet.NONE) }
    var deleteTaskId by remember { mutableStateOf<String?>(null) }

    val resourcePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { picked ->
            readUploadPart(context, picked)?.let { viewModel.uploadResource(it) }
        }
    }

    // Download helper
    fun downloadResource(resourceId: String, fileName: String) {
        scope.launch {
            val body = viewModel.downloadResourceBody(resourceId) ?: return@launch
            val file = Downloader.saveToCache(context, body, fileName)
            Downloader.open(context, file)
        }
    }

    fun downloadAttachment(commentId: String, attachmentId: String, fileName: String) {
        scope.launch {
            val body = viewModel.downloadAttachmentBody(commentId, attachmentId) ?: return@launch
            val file = Downloader.saveToCache(context, body, fileName)
            Downloader.open(context, file)
        }
    }

    Column(modifier = Modifier.fillMaxSize()) {
        state.error?.let {
            Box(modifier = Modifier.padding(16.dp)) {
                ErrorBanner(it.asString()) { viewModel.refresh() }
            }
        }
        when {
            state.loading -> LoadingHint()
            state.board != null -> {
                val board = state.board!!

                // Overview header
                Column(modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = board.overview.title,
                                style = MaterialTheme.typography.titleMedium,
                                color = colors.textPrimary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            if (board.overview.description.isNotBlank()) {
                                Text(
                                    text = board.overview.description,
                                    style = MaterialTheme.typography.labelSmall,
                                    color = colors.textMuted,
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                        TinyPill(
                            board.overview.statusBadge.ifBlank { "ACTIVE" },
                            projectStatusColor(board.overview.statusBadge)
                        )
                        Spacer(Modifier.width(8.dp))
                        Text(
                            text = "${board.overview.progress}%",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = OrbitPrimary
                        )
                    }
                    Spacer(Modifier.height(8.dp))

                    // Panel buttons
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        PanelChip(
                            icon = Icons.Outlined.Group,
                            label = stringResource(R.string.ws_panel_team),
                            count = board.members.size
                        ) {
                            viewModel.loadAvailableMembers()
                            sheet = BoardSheet.TEAM
                        }
                        PanelChip(
                            icon = Icons.AutoMirrored.Outlined.Chat,
                            label = stringResource(R.string.ws_panel_chat),
                            count = board.comments.size
                        ) { sheet = BoardSheet.CHAT }
                        PanelChip(
                            icon = Icons.Outlined.Description,
                            label = stringResource(R.string.ws_panel_files),
                            count = board.resources.size
                        ) { sheet = BoardSheet.FILES }
                        PanelChip(
                            icon = Icons.Outlined.AutoAwesome,
                            label = stringResource(R.string.ws_panel_ai),
                            count = null
                        ) { sheet = BoardSheet.AI }
                    }
                }

                // Kanban columns
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 12.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    BOARD_COLUMNS.forEach { column ->
                        KanbanColumn(
                            column = column,
                            tasks = state.tasksFor(column),
                            onAdd = { title -> viewModel.addTask(column, title) },
                            onOpen = { viewModel.selectTask(it.id) },
                            onMove = { task, status -> viewModel.moveTask(task, status) }
                        )
                    }
                }
            }
        }
    }

    // Panels
    when (sheet) {
        BoardSheet.TEAM -> ModalBottomSheet(
            onDismissRequest = { sheet = BoardSheet.NONE },
            containerColor = colors.popupBg,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            TeamPanel(viewModel = viewModel)
        }
        BoardSheet.CHAT -> ModalBottomSheet(
            onDismissRequest = { sheet = BoardSheet.NONE },
            containerColor = colors.popupBg,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
            ) {
                Text(
                    text = stringResource(R.string.ws_panel_chat),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                ChatThread(
                    comments = state.board?.comments ?: emptyList(),
                    members = state.board?.members ?: emptyList(),
                    sending = state.sendingComment,
                    canDelete = viewModel::canDelete,
                    onSend = viewModel::sendComment,
                    onDeleteComment = viewModel::deleteComment,
                    onDownloadAttachment = { c, a, name -> downloadAttachment(c, a, name) }
                )
            }
        }
        BoardSheet.FILES -> ModalBottomSheet(
            onDismissRequest = { sheet = BoardSheet.NONE },
            containerColor = colors.popupBg,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 20.dp)
                    .verticalScroll(rememberScrollState())
            ) {
                Text(
                    text = stringResource(R.string.ws_panel_files),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                FilesList(
                    resources = state.board?.resources ?: emptyList(),
                    canDelete = viewModel::canDelete,
                    onUpload = { resourcePicker.launch("*/*") },
                    onDownload = { downloadResource(it.id, it.fileName) },
                    onDelete = viewModel::deleteResource
                )
            }
        }
        BoardSheet.AI -> ModalBottomSheet(
            onDismissRequest = { sheet = BoardSheet.NONE },
            containerColor = colors.popupBg,
            shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
        ) {
            Column(
                modifier = Modifier
                    .padding(horizontal = 16.dp)
                    .padding(bottom = 24.dp)
            ) {
                Text(
                    text = stringResource(R.string.ws_panel_ai),
                    style = MaterialTheme.typography.titleMedium,
                    color = colors.textPrimary
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    text = stringResource(R.string.ws_ai_hint),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        }
        BoardSheet.NONE -> Unit
    }

    // Task detail
    state.selectedTask?.let { task ->
        TaskDetailSheet(
            task = task,
            viewModel = viewModel,
            onDismiss = { viewModel.selectTask(null) },
            onDelete = { deleteTaskId = task.id },
            onDownloadResource = { id, name -> downloadResource(id, name) },
            onDownloadAttachment = { c, a, name -> downloadAttachment(c, a, name) }
        )
    }

    // Delete confirm
    deleteTaskId?.let { id ->
        ConfirmDeleteDialog(
            title = stringResource(R.string.ws_delete_task_title),
            message = stringResource(R.string.ws_delete_task_message),
            busy = state.busy,
            onConfirm = {
                viewModel.deleteTask(id)
                deleteTaskId = null
            },
            onDismiss = { deleteTaskId = null }
        )
    }
}

// Panel chip
@Composable
private fun PanelChip(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    count: Int?,
    onClick: () -> Unit
) {
    val colors = OrbitTheme.colors
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(10.dp),
        color = colors.surface2,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 10.dp, vertical = 7.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = colors.textSecondary,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(5.dp))
            Text(
                text = label,
                style = MaterialTheme.typography.labelSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textSecondary
            )
            if (count != null && count > 0) {
                Spacer(Modifier.width(5.dp))
                Box(
                    modifier = Modifier
                        .background(OrbitPrimary.copy(alpha = 0.12f), CircleShape)
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        text = count.toString(),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OrbitPrimary
                    )
                }
            }
        }
    }
}

// Kanban column
@Composable
private fun KanbanColumn(
    column: String,
    tasks: List<BoardTaskDto>,
    onAdd: (String) -> Unit,
    onOpen: (BoardTaskDto) -> Unit,
    onMove: (BoardTaskDto, String) -> Unit
) {
    val colors = OrbitTheme.colors
    val tone = columnColor(column)
    var adding by remember { mutableStateOf(false) }
    var newTitle by remember { mutableStateOf("") }

    Column(
        modifier = Modifier
            .width(280.dp)
            .fillMaxSize()
            .background(colors.pageBackground, RoundedCornerShape(14.dp))
            .border(1.dp, colors.border, RoundedCornerShape(14.dp))
            .padding(10.dp)
    ) {
        // Column header
        Row(verticalAlignment = Alignment.CenterVertically) {
            Box(
                modifier = Modifier
                    .size(8.dp)
                    .background(tone, CircleShape)
            )
            Spacer(Modifier.width(7.dp))
            Text(
                text = columnLabel(column).uppercase(),
                style = OrbitTextStyles.techLabel,
                color = colors.textSecondary,
                modifier = Modifier.weight(1f)
            )
            Box(
                modifier = Modifier
                    .background(tone.copy(alpha = 0.12f), CircleShape)
                    .padding(horizontal = 7.dp, vertical = 2.dp)
            ) {
                Text(
                    text = tasks.size.toString(),
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = tone
                )
            }
        }
        Spacer(Modifier.height(8.dp))

        // Inline create
        if (adding) {
            OrbitTextField(
                value = newTitle,
                onValueChange = { newTitle = it },
                placeholder = stringResource(R.string.ws_new_task_placeholder)
            )
            Spacer(Modifier.height(6.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                Surface(
                    onClick = {
                        onAdd(newTitle)
                        newTitle = ""
                        adding = false
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = OrbitPrimary
                ) {
                    Text(
                        text = stringResource(R.string.action_add),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
                Surface(
                    onClick = {
                        adding = false
                        newTitle = ""
                    },
                    shape = RoundedCornerShape(8.dp),
                    color = colors.surface2
                ) {
                    Text(
                        text = stringResource(R.string.action_cancel),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textSecondary,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                    )
                }
            }
        } else {
            Surface(
                onClick = { adding = true },
                shape = RoundedCornerShape(8.dp),
                color = Color.Transparent,
                border = androidx.compose.foundation.BorderStroke(1.dp, colors.borderStrong)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 7.dp),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Add,
                        contentDescription = null,
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        text = stringResource(R.string.ws_add_task),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
        Spacer(Modifier.height(8.dp))

        // Cards
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            tasks.forEachIndexed { index, task ->
                // Card entrance
                StaggeredAppear(index = index) {
                    TaskCard(
                        task = task,
                        column = column,
                        onOpen = { onOpen(task) },
                        onMove = { onMove(task, it) }
                    )
                }
            }
        }
    }
}

// Task card
@Composable
private fun TaskCard(
    task: BoardTaskDto,
    column: String,
    onOpen: () -> Unit,
    onMove: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    var showMove by remember { mutableStateOf(false) }
    val dueDays = parseInstant(task.due)?.let { daysUntil(task.due) }

    Surface(
        onClick = onOpen,
        shape = RoundedCornerShape(12.dp),
        color = colors.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, colors.border)
    ) {
        Column(modifier = Modifier.padding(10.dp)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = task.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
                if (task.visibility == "private") {
                    Icon(
                        imageVector = Icons.Outlined.Lock,
                        contentDescription = stringResource(R.string.ct_visibility_private),
                        tint = colors.textMuted,
                        modifier = Modifier.size(12.dp)
                    )
                }
            }
            Spacer(Modifier.height(6.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(5.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                task.priority?.let { TinyPill(it.uppercase(), priorityColor(it)) }
                task.reportType?.let { TinyPill(it, OrbitPrimary) }
            }
            Spacer(Modifier.height(7.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                // Assignees
                Row(modifier = Modifier.weight(1f)) {
                    val names = task.assignee.split(",").map { it.trim() }
                        .filter { it.isNotBlank() && it != "Unassigned" }
                    names.take(3).forEach { n ->
                        Box(modifier = Modifier.padding(end = 2.dp)) {
                            HashAvatar(name = n, size = 20.dp)
                        }
                    }
                    if (names.isEmpty()) {
                        Text(
                            text = stringResource(R.string.tk_unassigned),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                }
                if (task.due.isNotBlank() && task.due != "TBD") {
                    Text(
                        text = task.due.take(10),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.SemiBold,
                        color = when {
                            dueDays != null && dueDays < 0 -> OrbitDanger
                            dueDays != null && dueDays <= 3 -> OrbitWarning
                            else -> colors.textMuted
                        }
                    )
                }
                Spacer(Modifier.width(6.dp))
                Surface(
                    onClick = { showMove = !showMove },
                    shape = CircleShape,
                    color = columnColor(column).copy(alpha = 0.12f)
                ) {
                    Box(
                        modifier = Modifier.size(22.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        StatusDot(online = true, size = 8.dp)
                    }
                }
            }
            if (task.completedByNames.isNotEmpty()) {
                Spacer(Modifier.height(5.dp))
                Text(
                    text = stringResource(
                        R.string.ws_completed_by,
                        task.completedByNames.joinToString(", ")
                    ),
                    style = MaterialTheme.typography.labelSmall,
                    color = OrbitSuccess,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }

            // Move chips
            if (showMove) {
                Spacer(Modifier.height(7.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    BOARD_COLUMNS.filter { it != column }.forEach { target ->
                        Surface(
                            onClick = {
                                showMove = false
                                onMove(target)
                            },
                            shape = RoundedCornerShape(20.dp),
                            color = columnColor(target).copy(alpha = 0.1f)
                        ) {
                            Text(
                                text = columnLabel(target),
                                style = MaterialTheme.typography.labelSmall,
                                fontWeight = FontWeight.Bold,
                                color = columnColor(target),
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}
