package com.orbit.mobile.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitShapeTokens
import com.orbit.mobile.core.theme.OrbitTheme

// Button variants
enum class OrbitButtonVariant { Primary, Secondary, Ghost, Danger }

// App button
@Composable
fun OrbitButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    variant: OrbitButtonVariant = OrbitButtonVariant.Primary,
    enabled: Boolean = true,
    loading: Boolean = false,
    fullWidth: Boolean = false,
    leadingIcon: ImageVector? = null
) {
    val colors = OrbitTheme.colors
    val container = when (variant) {
        OrbitButtonVariant.Primary -> OrbitPrimary
        OrbitButtonVariant.Secondary -> colors.surface2
        OrbitButtonVariant.Ghost -> Color.Transparent
        OrbitButtonVariant.Danger -> OrbitDanger
    }
    val content = when (variant) {
        OrbitButtonVariant.Primary, OrbitButtonVariant.Danger -> Color.White
        OrbitButtonVariant.Secondary -> colors.textPrimary
        OrbitButtonVariant.Ghost -> colors.textSecondary
    }
    val border = when (variant) {
        OrbitButtonVariant.Secondary -> BorderStroke(1.dp, colors.borderStrong)
        else -> null
    }
    val alpha = if (enabled) 1f else 0.5f

    Surface(
        onClick = onClick,
        modifier = (if (fullWidth) modifier.fillMaxWidth() else modifier).height(44.dp),
        enabled = enabled && !loading,
        shape = OrbitShapeTokens.button,
        color = container.copy(alpha = container.alpha * alpha),
        contentColor = content.copy(alpha = content.alpha * alpha),
        border = border
    ) {
        Row(
            modifier = Modifier.padding(horizontal = 18.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (loading) {
                CircularProgressIndicator(
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(16.dp),
                    color = content,
                    strokeWidth = 2.dp
                )
            } else if (leadingIcon != null) {
                Icon(
                    imageVector = leadingIcon,
                    contentDescription = null,
                    modifier = Modifier
                        .padding(end = 8.dp)
                        .size(18.dp)
                )
            }
            Text(
                text = text,
                style = MaterialTheme.typography.labelLarge,
                maxLines = 1
            )
        }
    }
}
