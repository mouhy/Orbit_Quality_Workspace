package com.orbit.mobile.feature.shell

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.rounded.MoreHoriz
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitTheme

// Shell bottom
@Composable
fun OrbitBottomBar(
    items: List<OrbitNavItem>,
    hasOverflow: Boolean,
    currentRoute: String?,
    overflowSelected: Boolean,
    onItemClick: (OrbitNavItem) -> Unit,
    onMoreClick: () -> Unit
) {
    val colors = OrbitTheme.colors
    val currentBase = currentRoute?.substringBefore("?")

    val itemColors = NavigationBarItemDefaults.colors(
        selectedIconColor = OrbitPrimary,
        selectedTextColor = OrbitPrimary,
        indicatorColor = OrbitPrimary.copy(alpha = 0.14f),
        unselectedIconColor = colors.textMuted,
        unselectedTextColor = colors.textMuted
    )

    Column {
        HorizontalDivider(color = colors.border)
        NavigationBar(
            modifier = Modifier.fillMaxWidth(),
            containerColor = colors.sidebarBg,
            tonalElevation = 0.dp
        ) {
            items.forEach { item ->
                val selected = item.route.substringBefore("?") == currentBase
                NavigationBarItem(
                    selected = selected,
                    onClick = { onItemClick(item) },
                    icon = {
                        // Bounce icon
                        BounceIcon(
                            icon = item.icon,
                            selected = selected,
                            contentDescription = stringResource(item.labelRes)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(item.labelRes),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    },
                    colors = itemColors
                )
            }
            if (hasOverflow) {
                NavigationBarItem(
                    selected = overflowSelected,
                    onClick = onMoreClick,
                    icon = {
                        // Bounce icon
                        BounceIcon(
                            icon = Icons.Rounded.MoreHoriz,
                            selected = overflowSelected,
                            contentDescription = stringResource(R.string.nav_more)
                        )
                    },
                    label = {
                        Text(
                            text = stringResource(R.string.nav_more),
                            style = MaterialTheme.typography.labelSmall,
                            maxLines = 1
                        )
                    },
                    colors = itemColors
                )
            }
        }
    }
}

// Zoom icon
@Composable
private fun BounceIcon(
    icon: ImageVector,
    selected: Boolean,
    contentDescription: String?
) {
    // Spring scale
    val scale by animateFloatAsState(
        targetValue = if (selected) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navScale"
    )
    // Active popup
    val pop by animateFloatAsState(
        targetValue = if (selected) -15f else 0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "navPop"
    )
    Icon(
        imageVector = icon,
        contentDescription = contentDescription,
        modifier = Modifier.graphicsLayer {
            scaleX = scale
            scaleY = scale
            translationY = pop
        }
    )
}
