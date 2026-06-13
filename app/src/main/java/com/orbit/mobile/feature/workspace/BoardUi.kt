package com.orbit.mobile.feature.workspace

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Send
import androidx.compose.material.icons.outlined.AttachFile
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Description
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.KanbanDone
import com.orbit.mobile.core.theme.KanbanInProgress
import com.orbit.mobile.core.theme.KanbanReview
import com.orbit.mobile.core.theme.KanbanTodo
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.ui.components.OrbitTextField
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.data.dto.BoardCommentDto
import com.orbit.mobile.data.dto.BoardMemberDto
import com.orbit.mobile.data.dto.BoardResourceDto
import com.orbit.mobile.domain.repository.ReplyMeta
import com.orbit.mobile.domain.repository.UploadPart
import com.orbit.mobile.feature.dashboard.HashAvatar
import com.orbit.mobile.feature.dashboard.TinyPill

// Column color
fun columnColor(column: String): Color = when (column) {
    "in_progress" -> KanbanInProgress
    "review" -> KanbanReview
    "done" -> KanbanDone
    else -> KanbanTodo
}

// Column label
@Composable
fun columnLabel(column: String): String = when (column) {
    "in_progress" -> stringResource(R.string.ws_col_in_progress)
    "review" -> stringResource(R.string.ws_col_review)
    "done" -> stringResource(R.string.ws_col_done)
    else -> stringResource(R.string.ws_col_todo)
}

// File color
fun fileTone(fileName: String): Color {
    val ext = fileName.substringAfterLast('.', "").lowercase()
    return when (ext) {
        "pdf" -> OrbitDanger
        "doc", "docx" -> OrbitPrimary
        "xls", "xlsx", "csv" -> OrbitSuccess
        "png", "jpg", "jpeg", "gif", "webp" -> OrbitPurple
        else -> OrbitWarning
    }
}

// Read uri
fun readUploadPart(context: Context, uri: Uri): UploadPart? {
    return try {
        var name = "file"
        context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
            val idx = cursor.getColumnIndex(OpenableColumns.DISPLAY_NAME)
            if (idx >= 0 && cursor.moveToFirst()) name = cursor.getString(idx) ?: name
        }
        val bytes = context.contentResolver.openInputStream(uri)?.use { it.readBytes() }
            ?: return null
        val mime = context.contentResolver.getType(uri) ?: "application/octet-stream"
        UploadPart(fileName = name, bytes = bytes, mimeType = mime)
    } catch (_: Exception) {
        null
    }
}

// Mention pattern
fun activeMentionQuery(text: String): String? {
    val at = text.lastIndexOf('@')
    if (at == -1) return null
    val after = text.substring(at + 1)
    if (after.contains(' ') || after.contains('\n')) return null
    return after
}

// Chat thread
@Composable
fun ChatThread(
    comments: List<BoardCommentDto>,
    members: List<BoardMemberDto>,
    sending: Boolean,
    canDelete: (String?) -> Boolean,
    onSend: (String?, List<UploadPart>, List<String>, ReplyMeta?) -> Unit,
    onDeleteComment: (String) -> Unit,
    onDownloadAttachment: (commentId: String, attachmentId: String, fileName: String) -> Unit
) {
    val colors = OrbitTheme.colors
    val context = LocalContext.current
    var input by remember { mutableStateOf("") }
    var pendingFiles by remember { mutableStateOf<List<UploadPart>>(emptyList()) }
    var mentions by remember { mutableStateOf<Map<String, String>>(emptyMap()) }
    var replyTo by remember { mutableStateOf<ReplyMeta?>(null) }

    val picker = androidx.activity.compose.rememberLauncherForActivityResult(
        androidx.activity.result.contract.ActivityResultContracts.GetMultipleContents()
    ) { uris ->
        val parts = uris.mapNotNull { readUploadPart(context, it) }
        pendingFiles = pendingFiles + parts
    }

    Column {
        // Messages
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(max = 380.dp)
                .verticalScroll(rememberScrollState())
        ) {
            if (comments.isEmpty()) {
                Text(
                    text = stringResource(R.string.ws_no_messages),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted,
                    modifier = Modifier.padding(vertical = 20.dp)
                )
            }
            comments.forEach { comment ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 6.dp)
                ) {
                    HashAvatar(name = comment.userName.ifBlank { "U" }, size = 30.dp)
                    Spacer(Modifier.width(8.dp))
                    Column(modifier = Modifier.weight(1f)) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Text(
                                text = comment.userName,
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = colors.textPrimary
                            )
                            Spacer(Modifier.width(6.dp))
                            Text(
                                text = timeAgo(comment.createdAt).asString(),
                                style = MaterialTheme.typography.labelSmall,
                                color = colors.textMuted
                            )
                            Spacer(Modifier.weight(1f))
                            if (canDelete(comment.userId)) {
                                IconButton(
                                    onClick = { onDeleteComment(comment.id) },
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
                        if (comment.message.isNotBlank()) {
                            Surface(
                                onClick = {
                                    replyTo = ReplyMeta(
                                        id = comment.id,
                                        preview = comment.message.take(80),
                                        author = comment.userName
                                    )
                                },
                                shape = RoundedCornerShape(10.dp),
                                color = colors.surface2,
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    colors.border
                                )
                            ) {
                                Text(
                                    text = comment.message,
                                    style = MaterialTheme.typography.bodySmall,
                                    color = colors.textPrimary,
                                    modifier = Modifier.padding(
                                        horizontal = 10.dp,
                                        vertical = 8.dp
                                    )
                                )
                            }
                        }
                        comment.attachments.forEach { att ->
                            Surface(
                                onClick = {
                                    onDownloadAttachment(comment.id, att.id, att.fileName)
                                },
                                modifier = Modifier.padding(top = 4.dp),
                                shape = RoundedCornerShape(8.dp),
                                color = fileTone(att.fileName).copy(alpha = 0.07f),
                                border = androidx.compose.foundation.BorderStroke(
                                    1.dp,
                                    fileTone(att.fileName).copy(alpha = 0.2f)
                                )
                            ) {
                                Row(
                                    modifier = Modifier.padding(
                                        horizontal = 8.dp,
                                        vertical = 6.dp
                                    ),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.Outlined.AttachFile,
                                        contentDescription = null,
                                        tint = fileTone(att.fileName),
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
                    }
                }
            }
        }

        // Reply banner
        replyTo?.let { reply ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
                    .background(OrbitPrimary.copy(alpha = 0.06f), RoundedCornerShape(8.dp))
                    .padding(horizontal = 10.dp, vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.ws_replying_to, reply.author),
                        style = MaterialTheme.typography.labelSmall,
                        fontWeight = FontWeight.Bold,
                        color = OrbitPrimary
                    )
                    Text(
                        text = reply.preview,
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                }
                IconButton(onClick = { replyTo = null }, modifier = Modifier.size(22.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = stringResource(R.string.action_close),
                        tint = colors.textMuted,
                        modifier = Modifier.size(13.dp)
                    )
                }
            }
        }

        // Pending files
        if (pendingFiles.isNotEmpty()) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                pendingFiles.take(3).forEach { file ->
                    TinyPill(file.fileName.take(18), OrbitPrimary)
                }
                if (pendingFiles.size > 3) {
                    TinyPill("+${pendingFiles.size - 3}", OrbitPrimary)
                }
                Surface(
                    onClick = { pendingFiles = emptyList() },
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

        // Mention list
        val query = activeMentionQuery(input)
        if (query != null) {
            val suggestions = members.filter {
                it.name.lowercase().contains(query.lowercase())
            }.take(6)
            if (suggestions.isNotEmpty()) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp)
                        .border(1.dp, colors.borderStrong, RoundedCornerShape(10.dp))
                        .padding(4.dp)
                ) {
                    suggestions.forEach { member ->
                        Surface(
                            onClick = {
                                val at = input.lastIndexOf('@')
                                input = input.substring(0, at) + "@${member.name} "
                                member.userId?.let {
                                    mentions = mentions + (member.name to it)
                                }
                            },
                            color = Color.Transparent
                        ) {
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 8.dp, vertical = 5.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                HashAvatar(name = member.name, size = 22.dp)
                                Spacer(Modifier.width(8.dp))
                                Text(
                                    text = member.name,
                                    style = MaterialTheme.typography.labelMedium,
                                    color = colors.textPrimary
                                )
                            }
                        }
                    }
                }
            }
        }

        // Composer
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp),
            verticalAlignment = Alignment.Bottom
        ) {
            IconButton(onClick = { picker.launch("*/*") }) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = stringResource(R.string.action_upload),
                    tint = colors.textSecondary,
                    modifier = Modifier.size(18.dp)
                )
            }
            OrbitTextField(
                value = input,
                onValueChange = { input = it },
                placeholder = stringResource(R.string.ws_message_placeholder),
                modifier = Modifier.weight(1f),
                singleLine = false
            )
            Spacer(Modifier.width(6.dp))
            Surface(
                onClick = {
                    val mentionedIds = mentions
                        .filterKeys { input.contains("@$it") }
                        .values.distinct().toList()
                    onSend(
                        input.trim().ifBlank { null },
                        pendingFiles,
                        mentionedIds,
                        replyTo
                    )
                    input = ""
                    pendingFiles = emptyList()
                    mentions = emptyMap()
                    replyTo = null
                },
                shape = CircleShape,
                color = OrbitPrimary,
                modifier = Modifier.size(42.dp)
            ) {
                Box(contentAlignment = Alignment.Center) {
                    if (sending) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(16.dp),
                            color = Color.White,
                            strokeWidth = 2.dp
                        )
                    } else {
                        Icon(
                            imageVector = Icons.AutoMirrored.Outlined.Send,
                            contentDescription = stringResource(R.string.action_send),
                            tint = Color.White,
                            modifier = Modifier.size(17.dp)
                        )
                    }
                }
            }
        }
    }
}

// Files list
@Composable
fun FilesList(
    resources: List<BoardResourceDto>,
    canDelete: (String?) -> Boolean,
    onUpload: () -> Unit,
    onDownload: (BoardResourceDto) -> Unit,
    onDelete: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    Column {
        Surface(
            onClick = onUpload,
            shape = RoundedCornerShape(10.dp),
            color = OrbitPrimary.copy(alpha = 0.08f),
            border = androidx.compose.foundation.BorderStroke(
                1.dp,
                OrbitPrimary.copy(alpha = 0.25f)
            )
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 10.dp),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.AttachFile,
                    contentDescription = null,
                    tint = OrbitPrimary,
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
                Text(
                    text = stringResource(R.string.ws_upload_file),
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = FontWeight.Bold,
                    color = OrbitPrimary
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        if (resources.isEmpty()) {
            Text(
                text = stringResource(R.string.ws_no_files),
                style = MaterialTheme.typography.bodySmall,
                color = colors.textMuted,
                modifier = Modifier.padding(vertical = 16.dp)
            )
        }
        resources.forEach { res ->
            val tone = fileTone(res.fileName)
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 5.dp)
                    .border(1.dp, colors.border, RoundedCornerShape(10.dp))
                    .padding(horizontal = 10.dp, vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(
                    modifier = Modifier
                        .size(34.dp)
                        .background(tone.copy(alpha = 0.1f), RoundedCornerShape(8.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Description,
                        contentDescription = null,
                        tint = tone,
                        modifier = Modifier.size(16.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = res.fileName,
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.SemiBold,
                        color = colors.textPrimary,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        text = "${res.uploaderName ?: res.uploaderRole} · ${
                            timeAgo(res.createdAt).asString()
                        }",
                        style = MaterialTheme.typography.labelSmall,
                        color = colors.textMuted
                    )
                }
                IconButton(onClick = { onDownload(res) }, modifier = Modifier.size(28.dp)) {
                    Icon(
                        imageVector = Icons.Outlined.Download,
                        contentDescription = stringResource(R.string.action_download),
                        tint = colors.textSecondary,
                        modifier = Modifier.size(16.dp)
                    )
                }
                if (canDelete(res.uploadedBy)) {
                    IconButton(onClick = { onDelete(res.id) }, modifier = Modifier.size(28.dp)) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = stringResource(R.string.action_delete),
                            tint = OrbitDanger,
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }
            }
        }
    }
}
