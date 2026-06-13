package com.orbit.mobile.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.ErrorOutline
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitPurple
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.theme.OrbitWarning
import com.orbit.mobile.core.util.asString
import com.orbit.mobile.core.util.timeAgo
import com.orbit.mobile.domain.model.Notification
import com.orbit.mobile.domain.model.NotificationType

// Route resolver
private fun notificationRoute(n: Notification): String? {
    val channels = setOf("public-group", "all-sub-admin")
    return when {
        // Route task
        !n.taskId.isNullOrBlank() -> "${InnerRoutes.STAFF_TASK}?taskId=${n.taskId}"
        // Route team
        !n.teamId.isNullOrBlank() -> "${InnerRoutes.TEAMS}/${n.teamId}"
        // Route channel
        !n.projectId.isNullOrBlank() && n.projectId in channels ->
            "${InnerRoutes.TASKFLOW}?projectId=${n.projectId}"
        // Route mention
        !n.projectId.isNullOrBlank() -> "${InnerRoutes.WORKSPACE}?projectId=${n.projectId}"
        else -> null
    }
}

// Type color
private fun notifColor(type: NotificationType): Color = when (type) {
    NotificationType.INFO -> OrbitPrimary
    NotificationType.SUCCESS -> OrbitSuccess
    NotificationType.WARNING -> OrbitWarning
    NotificationType.ERROR -> OrbitDanger
    NotificationType.MENTION -> OrbitPurple
}

// Type icon
private fun notifIcon(type: NotificationType): ImageVector = when (type) {
    NotificationType.INFO -> Icons.Outlined.Info
    NotificationType.SUCCESS -> Icons.Outlined.CheckCircle
    NotificationType.WARNING -> Icons.Outlined.WarningAmber
    NotificationType.ERROR -> Icons.Outlined.ErrorOutline
    NotificationType.MENTION -> Icons.Outlined.AlternateEmail
}

// Notifications panel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotificationsSheet(
    items: List<Notification>,
    onDismissRequest: () -> Unit,
    onMarkAllRead: () -> Unit,
    onDismissItem: (String) -> Unit,
    onItemClick: (String) -> Unit,
    onNavigate: (String) -> Unit
) {
    val colors = OrbitTheme.colors
    val unread = items.count { !it.isRead }

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        // Header
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = stringResource(R.string.notif_title),
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary
                )
                Text(
                    text = stringResource(R.string.notif_unread_count, unread),
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted
                )
            }
            if (unread > 0) {
                Surface(onClick = onMarkAllRead, color = Color.Transparent) {
                    Text(
                        text = stringResource(R.string.notif_mark_all),
                        style = MaterialTheme.typography.labelMedium,
                        color = OrbitPrimary,
                        modifier = Modifier.padding(6.dp)
                    )
                }
            }
        }
        HorizontalDivider(color = colors.border)

        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(32.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = stringResource(R.string.notif_empty),
                    style = MaterialTheme.typography.bodySmall,
                    color = colors.textMuted
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(420.dp)
            ) {
                items(items, key = { it.id }) { item ->
                    NotificationRow(
                        item = item,
                        onClick = {
                            // Mark read
                            onItemClick(item.id)
                            // Route dynamic
                            notificationRoute(item)?.let(onNavigate)
                        },
                        onDismiss = { onDismissItem(item.id) }
                    )
                    HorizontalDivider(color = colors.border)
                }
            }
        }
    }
}

// Single row
@Composable
private fun NotificationRow(item: Notification, onClick: () -> Unit, onDismiss: () -> Unit) {
    val colors = OrbitTheme.colors
    val tone = notifColor(item.type)
    val rowBg = if (!item.isRead && colors.isDark) OrbitPrimary.copy(alpha = 0.04f)
    else Color.Transparent

    // Clickable row
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .background(rowBg)
            .padding(horizontal = 16.dp, vertical = 12.dp)
    ) {
        // Icon tile
        Box(
            modifier = Modifier
                .size(32.dp)
                .background(tone.copy(alpha = 0.08f), RoundedCornerShape(9.dp))
                .border(1.dp, tone.copy(alpha = 0.15f), RoundedCornerShape(9.dp)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = notifIcon(item.type),
                contentDescription = null,
                modifier = Modifier.size(16.dp),
                tint = tone
            )
        }
        Spacer(Modifier.size(12.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.Top) {
                Text(
                    text = item.title,
                    style = MaterialTheme.typography.labelMedium,
                    fontWeight = if (item.isRead) FontWeight.Medium else FontWeight.Bold,
                    color = colors.textPrimary,
                    modifier = Modifier.weight(1f)
                )
                if (!item.isRead) {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp, top = 3.dp)
                            .size(6.dp)
                            .background(OrbitPrimary, CircleShape)
                    )
                }
            }
            if (item.body.isNotBlank()) {
                Spacer(Modifier.height(3.dp))
                Text(
                    text = item.body,
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textSecondary
                )
            }
            Spacer(Modifier.height(4.dp))
            Text(
                text = timeAgo(item.createdAt).asString(),
                style = MaterialTheme.typography.labelSmall,
                color = colors.textMuted
            )
        }

        // Dismiss
        IconButton(onClick = onDismiss, modifier = Modifier.size(24.dp)) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = stringResource(R.string.action_close),
                modifier = Modifier.size(14.dp),
                tint = colors.textMuted
            )
        }
    }
}
