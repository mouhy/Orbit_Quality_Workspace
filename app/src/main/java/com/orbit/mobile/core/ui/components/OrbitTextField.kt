package com.orbit.mobile.core.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import com.orbit.mobile.core.theme.OrbitDanger
import com.orbit.mobile.core.theme.OrbitPrimary
import com.orbit.mobile.core.theme.OrbitShapeTokens
import com.orbit.mobile.core.theme.OrbitTheme

// App input
@Composable
fun OrbitTextField(
    value: String,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    enabled: Boolean = true,
    isError: Boolean = false,
    errorText: String? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    visualTransformation: VisualTransformation = VisualTransformation.None,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
    keyboardActions: KeyboardActions = KeyboardActions.Default,
    leading: (@Composable () -> Unit)? = null,
    trailing: (@Composable () -> Unit)? = null
) {
    val colors = OrbitTheme.colors
    val interaction = remember { MutableInteractionSource() }
    val focused by interaction.collectIsFocusedAsState()

    val targetBorder = when {
        isError -> OrbitDanger
        focused -> OrbitPrimary
        else -> colors.borderStrong
    }
    val fieldBg = if (colors.isDark) colors.surface2 else colors.surface
    val targetRing = when {
        focused && isError -> OrbitDanger.copy(alpha = 0.12f)
        focused -> colors.focusRing
        else -> Color.Transparent
    }
    // Animated border
    val borderColor by animateColorAsState(targetBorder, tween(180), label = "border")
    // Animated ring
    val ringColor by animateColorAsState(targetRing, tween(180), label = "ring")
    // Label color
    val labelColor by animateColorAsState(
        if (focused && !isError) OrbitPrimary else colors.textSecondary,
        tween(180),
        label = "labelColor"
    )

    Column(modifier = modifier) {
        if (label != null) {
            Text(
                text = label,
                style = MaterialTheme.typography.labelMedium,
                color = labelColor,
                modifier = Modifier.padding(bottom = 6.dp)
            )
        }
        // Focus ring
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(ringColor, RoundedCornerShape(11.dp))
                .padding(3.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .defaultMinSize(minHeight = 44.dp)
                    .background(fieldBg, OrbitShapeTokens.input)
                    .border(1.dp, borderColor, OrbitShapeTokens.input)
                    .padding(horizontal = 12.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (leading != null) {
                    Box(modifier = Modifier.padding(end = 8.dp)) { leading() }
                }
                Box(modifier = Modifier.weight(1f)) {
                    BasicTextField(
                        value = value,
                        onValueChange = onValueChange,
                        modifier = Modifier.fillMaxWidth(),
                        enabled = enabled,
                        textStyle = MaterialTheme.typography.bodyMedium.copy(
                            color = colors.textPrimary
                        ),
                        cursorBrush = SolidColor(OrbitPrimary),
                        visualTransformation = visualTransformation,
                        keyboardOptions = keyboardOptions,
                        keyboardActions = keyboardActions,
                        singleLine = singleLine,
                        minLines = minLines,
                        interactionSource = interaction
                    )
                    if (value.isEmpty() && placeholder != null) {
                        Text(
                            text = placeholder,
                            style = MaterialTheme.typography.bodyMedium,
                            color = colors.textMuted,
                            maxLines = 1
                        )
                    }
                }
                if (trailing != null) {
                    Box(modifier = Modifier.padding(start = 8.dp)) { trailing() }
                }
            }
        }
        if (isError && errorText != null) {
            Text(
                text = errorText,
                style = MaterialTheme.typography.bodySmall,
                color = OrbitDanger,
                modifier = Modifier.padding(top = 4.dp, start = 3.dp)
            )
        }
    }
}
