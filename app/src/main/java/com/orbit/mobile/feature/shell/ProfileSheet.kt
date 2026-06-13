package com.orbit.mobile.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.orbit.mobile.R
import com.orbit.mobile.core.datastore.UserSession
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitSuccess
import com.orbit.mobile.core.theme.OrbitTextStyles
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitAvatar

// Format role
fun formatRoleLabel(role: String?): String =
    role?.replace("_", " ")
        ?.split(" ")
        ?.joinToString(" ") { word -> word.replaceFirstChar { it.uppercase() } }
        ?: "Member"

// Profile panel
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ProfileSheet(
    session: UserSession,
    onDismissRequest: () -> Unit,
    onProfileSettings: () -> Unit,
    onLogout: () -> Unit
) {
    val colors = OrbitTheme.colors

    ModalBottomSheet(
        onDismissRequest = onDismissRequest,
        containerColor = colors.popupBg,
        shape = RoundedCornerShape(topStart = 14.dp, topEnd = 14.dp)
    ) {
        // User card
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(OrbitPrimary.copy(alpha = if (colors.isDark) 0.05f else 0.03f))
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            OrbitAvatar(
                name = session.fullName ?: "",
                imageUrl = session.avatar,
                size = 40.dp
            )
            Spacer(Modifier.size(12.dp))
            Column {
                Text(
                    text = session.fullName ?: "",
                    style = MaterialTheme.typography.titleSmall,
                    color = colors.textPrimary,
                    maxLines = 1
                )
                Text(
                    text = session.email ?: "",
                    style = MaterialTheme.typography.labelSmall,
                    color = colors.textMuted,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                Row(
                    modifier = Modifier
                        .background(OrbitPrimary.copy(alpha = 0.12f), RoundedCornerShape(20.dp))
                        .border(1.dp, OrbitPrimary.copy(alpha = 0.2f), RoundedCornerShape(20.dp))
                        .padding(horizontal = 8.dp, vertical = 2.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(5.dp)
                            .background(OrbitSuccess, CircleShape)
                    )
                    Spacer(Modifier.size(4.dp))
                    Text(
                        text = formatRoleLabel(session.role).uppercase(),
                        style = OrbitTextStyles.techLabel.copy(fontSize = MaterialTheme.typography.labelSmall.fontSize),
                        color = OrbitPrimary
                    )
                }
            }
        }
        HorizontalDivider(color = colors.border)

        // Profile settings
        Surface(onClick = onProfileSettings, color = Color.Transparent) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.Outlined.Person,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = colors.textPrimary
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.profile_settings),
                    style = MaterialTheme.typography.bodyMedium,
                    color = colors.textPrimary
                )
            }
        }
        HorizontalDivider(
            color = colors.border,
            modifier = Modifier.padding(horizontal = 12.dp)
        )

        // Logout row
        Surface(onClick = onLogout, color = Color.Transparent) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 18.dp, vertical = 13.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    imageVector = Icons.AutoMirrored.Outlined.Logout,
                    contentDescription = null,
                    modifier = Modifier.size(16.dp),
                    tint = OrbitDanger
                )
                Spacer(Modifier.size(10.dp))
                Text(
                    text = stringResource(R.string.action_logout),
                    style = MaterialTheme.typography.bodyMedium,
                    color = OrbitDanger
                )
            }
        }
        Spacer(Modifier.height(16.dp))
    }
}
