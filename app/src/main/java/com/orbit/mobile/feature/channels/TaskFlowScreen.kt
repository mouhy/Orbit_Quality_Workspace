package com.orbit.mobile.feature.channels

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
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material3.Checkbox
import androidx.compose.material3.CheckboxDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
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
import com.orbit.mobile.core.theme.OrbitGradients
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.theme.roleColor
import com.orbit.mobile.core.ui.components.OrbitButton
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.ui.components.StatusDot
import com.orbit.mobile.core.util.Downloader
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.data.dto.BoardCommentDto
import com.orbit.mobile.domain.repository.UploadPart
import com.orbit.mobile.feature.dashboard.ErrorBanner
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.LoadingHint
import com.orbit.mobile.feature.dashboard.TinyPill
import com.orbit.mobile.feature.workspace.FilesList
import com.orbit.mobile.feature.workspace.activeMentionQuery
import com.orbit.mobile.feature.workspace.readUploadPart
import kotlinx.coroutines.launch

// Reaction set
private val REACTIONS = listOf("👍", "❤️", "😂", "🔥", "✅")

// Channel tabs
private enum class ChannelTab { FEED, TODOS, FILES, MEMBERS, ACTIVITY }

// Kind tone
private fun postTone(kind: PostKind): Color = when (kind) {
    PostKind.ANNOUNCEMENT -> Color(0xFF6366F1)
    PostKind.TASK -> OrbitSuccess
    PostKind.NOTE -> OrbitWarning
    else -> OrbitPrimary
}

private fun postEmoji(kind: PostKind): String = when (kind) {
    PostKind.ANNOUNCEMENT -> "📢"
    PostKind.TASK -> "📋"
    PostKind.NOTE -> "📝"
    else -> "💬"
}

// Channel page
@Composable
fun TaskFlowScreen(
    viewModel: TaskFlowViewModel = hiltViewModel()
) {
    val colors = OrbitTheme.colors
    val state by viewModel.state.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var tab by remember { mutableStateOf(ChannelTab.FEED) }

    val resourcePicker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetContent()
    ) { uri ->
        uri?.let { picked ->
            readUploadPart(context, picked)?.let { viewModel.uploadResource(it) }
        }
    }

    fun downloadResource(id: String, name: String) {
        scope.launch {
            val body = viewModel.downloadResourceBody(id) ?: return@launch
            Downloader.open(context, Downloader.saveToCache(context, body, name))
        }
    }

    fun downloadAttachment(commentId: String, attachmentId: String, name: String) {
        scope.launch {
            val body = viewModel.downloadAttachmentBody(commentId, attachmentId) ?: return@launch
            Downloader.open(context, Downloader.saveToCache(context, body, name))
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
    ) {
        // Gradient header
        val gradient = when (viewModel.projectId) {
            "public-group" -> OrbitGradients.channelPublic
            "all-sub-admin" -> OrbitGradients.channelOps
            else -> OrbitGradients.avatarFallback
        }
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(gradient)
                .padding(16.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = if (viewModel.projectId == "all-sub-admin") "⚡" else "🌐",
                    style = MaterialTheme.typography.headlineSmall
                )
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = state.board?.overview?.title ?: viewModel.projectId,
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (!state.board?.overview?.description.isNullOrBlank()) {
                        Text(
                            text = state.board?.overview?.description ?: "",
                            style = MaterialTheme.typography.labelSmall,
                            color = Color.White.copy(alpha = 0.7f),
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.15f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 9.dp, vertical = 4.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatusDot(online = true, size = 9.dp)
                    Spacer(Modifier.width(5.dp))
                    Text(
                        text = stringResource(R.string.ch_online_count, state.onlineCount),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }
            }
        }

        Column(modifier = Modifier.padding(14.dp)) {
            state.error?.let { ErrorBanner(it.asString()) { viewModel.refresh() } }

            // Tabs
            Row(
                modifier = Modifier.horizontalScroll(rememberScrollState()),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                ChannelTab.entries.forEach { t ->
                    val active = tab == t
                    val label = when (t) {
                        ChannelTab.FEED -> stringResource(R.string.ch_tab_feed)
                        ChannelTab.TODOS -> stringResource(R.string.nav_tasks)
                        ChannelTab.FILES -> stringResource(R.string.ws_panel_files)
                        ChannelTab.MEMBERS -> stringResource(R.string.ch_tab_members)
                        ChannelTab.ACTIVITY -> stringResource(R.string.ch_tab_activity)
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
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(12.dp))

            when {
                state.loading -> LoadingHint()
                else -> when (tab) {
                    ChannelTab.FEED -> FeedTab(
                        state = state,
                        viewModel = viewModel,
                        onDownloadAttachment = { c, a, n -> downloadAttachment(c, a, n) }
                    )
                    ChannelTab.TODOS -> TodosTab(state, viewModel)
                    ChannelTab.FILES -> FilesList(
                        resources = state.board?.resources ?: emptyList(),
                        canDelete = viewModel::canDelete,
                        onUpload = { resourcePicker.launch("*/*") },
                        onDownload = { downloadResource(it.id, it.fileName) },
                        onDelete = viewModel::deleteResource
                    )
                    ChannelTab.MEMBERS -> MembersTab(state)
                    ChannelTab.ACTIVITY -> ActivityTab(state)
                }
            }
        }
    }
}

// Feed tab
@Composable
private fun FeedTab(
    state: ChannelState,
    viewModel: TaskFlowViewModel,
    onDownloadAttachment: (String, String, String) -> Unit
) {
    val colors = OrbitTheme.colors
    val context = LocalContext.current
    var postType by remember { mutableStateOf("announcement") }
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var pendingFiles by remember { mutableStateOf<List<UploadPart>>(emptyList()) }
    var mentions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var replyingTo by remember { mutableStateOf<String?>(null) }
    var replyText by remember { mutableStateOf("") }

    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        pendingFiles = pendingFiles + uris.mapNotNull { readUploadPart(context, it) }
    }

    Column {
        // Composer
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .background(colors.surface, RoundedCornerShape(14.dp))
                .border(1.dp, colors.border, RoundedCornerShape(14.dp))
                .padding(12.dp)
        ) {
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                listOf(
                    "announcement" to stringResource(R.string.ch_type_announcement),
                    "task" to stringResource(R.string.ch_type_task),
                    "note" to stringResource(R.string.ch_type_note)
                ).forEach { (key, label) ->
                    val active = postType == key
                    val tone = postTone(
                        when (key) {
                            "announcement" -> PostKind.ANNOUNCEMENT
                            "task" -> PostKind.TASK
                            else -> PostKind.NOTE
                        }
                    )
                    Surface(
                        onClick = { postType = key },
                        shape = RoundedCornerShape(18.dp),
                        color = if (active) tone else tone.copy(alpha = 0.08f)
                    ) {
                        Text(
                            text = label,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else tone,
                            modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
                        )
                    }
                }
            }
            Spacer(Modifier.height(8.dp))
            OrbitTextField(
                value = title,
                onValueChange = { title = it },
                placeholder = stringResource(R.string.ch_post_title_placeholder)
            )
            Spacer(Modifier.height(8.dp))
            OrbitTextField(
                value = content,
                onValueChange = { content = it },
                placeholder = stringResource(R.string.ch_post_content_placeholder),
                singleLine = false,
                minLines = 2
            )

            // Mention list
            val query = activeMentionQuery(content)
            if (query != null) {
                val suggestions = (state.board?.members ?: emptyList())
                    .filter { it.name.lowercase().contains(query.lowercase()) }
                    .take(6)
                suggestions.forEach { member ->
                    Surface(
                        onClick = {
                            val at = content.lastIndexOf('@')
                            content = content.substring(0, at) + "@${member.name} "
                            member.userId?.let { mentions = mentions + (member.name to it) }
                        },
                        color = Color.Transparent
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 4.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            HashAvatar(name = member.name, size = 20.dp)
                            Spacer(Modifier.width(7.dp))
                            Text(
                                text = member.name,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textPrimary
                            )
                        }
                    }
                }
            }

            if (pendingFiles.isNotEmpty()) {
                Spacer(Modifier.height(6.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    pendingFiles.take(3).forEach { TinyPill(it.fileName.take(16), OrbitPrimary) }
                    if (pendingFiles.size > 3) TinyPill("+${pendingFiles.size - 3}", OrbitPrimary)
                }
            }
            Spacer(Modifier.height(8.dp))
            Row(verticalAlignment = Alignment.CenterVertically) {
                IconButton(onClick = { picker.launch("*/*") }) {
                    Icon(
                        imageVector = Icons.Outlined.AttachFile,
                        contentDescription = stringResource(R.string.action_upload),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(17.dp)
                    )
                }
                Spacer(Modifier.weight(1f))
                OrbitButton(
                    text = stringResource(R.string.ch_publish),
                    onClick = {
                        val mentionedIds = mentions
                            .filterKeys { content.contains("@$it") }
                            .values.distinct().toList()
                        viewModel.sendPost(postType, title, content, pendingFiles, mentionedIds)
                        title = ""
                        content = ""
                        pendingFiles = emptyList()
                        mentions = emptyMap()
                    },
                    loading = state.sending,
                    enabled = title.isNotBlank() || content.isNotBlank() ||
                        pendingFiles.isNotEmpty()
                )
            }
        }
        Spacer(Modifier.height(12.dp))

        // Posts
        if (state.posts.isEmpty()) {
            Text(
                text = stringResource(R.string.ws_no_messages),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        state.posts.forEach { post ->
            val parsed = parseMessage(post.message)
            val tone = postTone(parsed.kind)
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
                    .background(colors.surface, RoundedCornerShape(14.dp))
                    .border(1.dp, tone.copy(alpha = 0.18f), RoundedCornerShape(14.dp))
                    .padding(12.dp)
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(text = postEmoji(parsed.kind))
                    Spacer(Modifier.width(8.dp))
                    HashAvatar(name = post.userName.ifBlank { "U" }, size = 26.dp)
                    Spacer(Modifier.width(7.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = post.userName,
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Text(
                            text = timeAgo(post.createdAt).asString(),
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textMuted
                        )
                    }
                    if (viewModel.canDelete(post.userId)) {
                        IconButton(
                            onClick = { viewModel.deleteComment(post.id) },
                            modifier = Modifier.size(22.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Close,
                                contentDescription = stringResource(R.string.action_delete),
                                tint = colors.textMuted,
                                modifier = Modifier.size(13.dp)
                            )
                        }
                    }
                }
                Spacer(Modifier.height(8.dp))
                parsed.title?.let {
                    Text(
                        text = it,
                        style = MaterialTheme.typography.titleSmall,
                        color = colors.textPrimary
                    )
                    Spacer(Modifier.height(3.dp))
                }
                if (parsed.content.isNotBlank()) {
                    Text(
                        text = parsed.content,
                        style = MaterialTheme.typography.bodySmall,
                        color = colors.textSecondary
                    )
                }

                // Attachments
                post.attachments.forEach { att ->
                    Surface(
                        onClick = { onDownloadAttachment(post.id, att.id, att.fileName) },
                        modifier = Modifier.padding(top = 6.dp),
                        shape = RoundedCornerShape(8.dp),
                        color = tone.copy(alpha = 0.06f),
                        border = androidx.compose.foundation.BorderStroke(
                            1.dp,
                            tone.copy(alpha = 0.18f)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.AttachFile,
                                contentDescription = null,
                                tint = tone,
                                modifier = Modifier.size(13.dp)
                            )
                            Spacer(Modifier.width(5.dp))
                            Text(
                                text = att.fileName,
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textSecondary,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                        }
                    }
                }

                // Reactions
                Spacer(Modifier.height(8.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    REACTIONS.forEach { emoji ->
                        val count = state.reactions[post.id]?.get(emoji) ?: 0
                        Surface(
                            onClick = { viewModel.react(post.id, emoji) },
                            shape = RoundedCornerShape(16.dp),
                            color = if (count > 0) OrbitPrimary.copy(alpha = 0.1f)
                            else colors.surface2,
                            border = androidx.compose.foundation.BorderStroke(
                                1.dp,
                                colors.border
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = emoji, style = MaterialTheme.typography.labelSmall)
                                if (count > 0) {
                                    Spacer(Modifier.width(3.dp))
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
                    Spacer(Modifier.weight(1f))
                    Surface(
                        onClick = {
                            replyingTo = if (replyingTo == post.id) null else post.id
                            replyText = ""
                        },
                        color = Color.Transparent
                    ) {
                        Text(
                            text = stringResource(R.string.ch_reply),
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = OrbitPrimary,
                            modifier = Modifier.padding(4.dp)
                        )
                    }
                }

                // Replies
                val postReplies = state.replies[post.id] ?: emptyList()
                postReplies.forEach { reply ->
                    ReplyRow(
                        reply = reply,
                        canDelete = viewModel.canDelete(reply.userId),
                        onDelete = { viewModel.deleteComment(reply.id) }
                    )
                }

                // Reply composer
                if (replyingTo == post.id) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.Bottom) {
                        OrbitTextField(
                            value = replyText,
                            onValueChange = { replyText = it },
                            placeholder = stringResource(R.string.ch_reply_placeholder),
                            modifier = Modifier.weight(1f)
                        )
                        Spacer(Modifier.width(6.dp))
                        OrbitButton(
                            text = stringResource(R.string.action_send),
                            onClick = {
                                viewModel.sendReply(post.id, replyText, emptyList())
                                replyText = ""
                                replyingTo = null
                            },
                            enabled = replyText.isNotBlank()
                        )
                    }
                }
            }
        }
    }
}

// Reply row
@Composable
private fun ReplyRow(
    reply: BoardCommentDto,
    canDelete: Boolean,
    onDelete: () -> Unit
) {
    val colors = OrbitTheme.colors
    val parsed = parseMessage(reply.message)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp, start = 14.dp)
            .background(colors.surface2, RoundedCornerShape(10.dp))
            .padding(8.dp)
    ) {
        HashAvatar(name = reply.userName.ifBlank { "U" }, size = 22.dp)
        Spacer(Modifier.width(7.dp))
        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = reply.userName,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = FontWeight.Bold,
                    color = colors.textPrimary
                )
                Spacer(Modifier.width(5.dp))
                Text(
                    text = timeAgo(reply.createdAt).asString(),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
                Spacer(Modifier.weight(1f))
                if (canDelete) {
                    IconButton(onClick = onDelete, modifier = Modifier.size(18.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Close,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = colors.textMuted,
                            modifier = Modifier.size(11.dp)
                        )
                    }
                }
            }
            Text(
                text = parsed.content,
                style = MaterialTheme.typography.labelSmall,
                color = colors.textSecondary
            )
        }
    }
}

// Todos tab
@Composable
private fun TodosTab(state: ChannelState, viewModel: TaskFlowViewModel) {
    val colors = OrbitTheme.colors
    val tasks = state.board?.tasks ?: emptyList()
    Column {
        if (tasks.isEmpty()) {
            Text(
                text = stringResource(R.string.dash_no_active_tasks),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        tasks.forEach { task ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 4.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 6.dp, vertical = 2.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Checkbox(
                    checked = task.done,
                    onCheckedChange = { viewModel.toggleTodo(task.id, it) },
                    colors = CheckboxDefaults.colors(checkedColor = OrbitSuccess)
                )
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = task.title,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = if (task.done) colors.textMuted else colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = task.assignee,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                task.priority?.let {
                    TinyPill(it.uppercase(), com.orbit.mobile.core.theme.priorityColor(it))
                }
            }
        }
    }
}

// Members tab
@Composable
private fun MembersTab(state: ChannelState) {
    val colors = OrbitTheme.colors
    Column {
        Text(
            text = stringResource(R.string.ch_online_count, state.onlineCount).uppercase(),
            style = OrbitTextStyles.techLabel,
            color = colors.textMuted
        )
        Spacer(Modifier.height(8.dp))
        (state.board?.members ?: emptyList()).forEach { member ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box {
                    HashAvatar(name = member.name, size = 36.dp)
                    if (member.online) {
                        Box(modifier = Modifier.align(Alignment.BottomEnd)) {
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
                        color = colors.textPrimary
                    )
                    Text(
                        text = member.email ?: "",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                TinyPill(member.role, roleColor(member.role))
            }
        }
    }
}

// Activity tab
@Composable
private fun ActivityTab(state: ChannelState) {
    val colors = OrbitTheme.colors
    Column {
        if (state.activity.isEmpty()) {
            Text(
                text = stringResource(R.string.dash_no_activity),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        state.activity.forEach { entry ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(30.dp)
                        .background(
                            (if (entry.isFile) OrbitWarning else OrbitPrimary)
                                .copy(alpha = 0.1f),
                            CircleShape
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = if (entry.isFile) "📎" else "💬",
                        style = MaterialTheme.typography.labelSmall
                    )
                }
                Spacer(Modifier.width(9.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Row {
                        Text(
                            text = entry.actor,
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = colors.textPrimary
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = entry.text,
                            style = MaterialTheme.typography.labelSmall,
                            color = colors.textSecondary,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Text(
                        text = timeAgo(entry.time).asString(),
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
            }
        }
    }
}
