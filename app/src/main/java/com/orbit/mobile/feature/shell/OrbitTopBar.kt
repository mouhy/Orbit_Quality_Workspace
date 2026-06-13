package com.orbit.mobile.feature.shell

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitTheme
import com.orbit.mobile.core.ui.components.OrbitAvatar
import com.orbit.mobile.core.ui.components.OrbitLogo
import com.orbit.mobile.core.ui.components.OrbitLogoVariant

// Shell header
@Composable
fun OrbitTopBar(
    title: String,
    userName: String,
    avatarUrl: String?,
    unreadCount: Int,
    onBellClick: () -> Unit,
    onAvatarClick: () -> Unit
) {
    val colors = OrbitTheme.colors

    Column(modifier = Modifier.background(colors.headerBg)) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .statusBarsPadding()
                .height(58.dp)
                .padding(horizontal = 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // App logo
            OrbitLogo(
                variant = OrbitLogoVariant.Mark,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.size(10.dp))
            Text(
                text = title,
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = colors.textPrimary,
                maxLines = 1,
                modifier = Modifier.weight(1f)
            )

            // Bell button
            Box {
                Surface(
                    onClick = onBellClick,
                    shape = RoundedCornerShape(9.dp),
                    color = colors.surface2,
                    border = androidx.compose.foundation.BorderStroke(1.dp, colors.border),
                    modifier = Modifier.size(36.dp)
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = Icons.Outlined.Notifications,
                            contentDescription = stringResource(R.string.cd_notifications),
                            modifier = Modifier.size(18.dp),
                            tint = colors.textSecondary
                        )
                    }
                }
                if (unreadCount > 0) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .offset(x = 4.dp, y = (-4).dp)
                            .size(16.dp)
                            .border(2.dp, colors.headerBg, CircleShape)
                            .clip(CircleShape)
                            .background(OrbitPrimary),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = unreadCount.coerceAtMost(9).toString(),
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.ExtraBold,
                            lineHeight = 9.sp
                        )
                    }
                }
            }
            Spacer(Modifier.size(10.dp))

            // Avatar button
            Surface(
                onClick = onAvatarClick,
                shape = CircleShape,
                color = Color.Transparent
            ) {
                OrbitAvatar(name = userName, imageUrl = avatarUrl, size = 34.dp)
            }
        }
        HorizontalDivider(color = colors.border)
    }
}
