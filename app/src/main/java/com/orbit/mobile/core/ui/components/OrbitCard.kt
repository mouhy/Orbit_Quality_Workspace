package com.orbit.mobile.core.ui.components

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.orbit.mobile.core.theme.OrbitShapeTokens
import com.orbit.mobile.core.theme.OrbitTheme

// App card
@Composable
fun OrbitCard(
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null,
    contentPadding: PaddingValues = PaddingValues(16.dp),
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = OrbitTheme.colors
    val border = BorderStroke(1.dp, colors.border)

    if (onClick != null) {
        Surface(
            onClick = onClick,
            modifier = modifier,
            shape = OrbitShapeTokens.card,
            color = colors.surface,
            border = border,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    } else {
        Surface(
            modifier = modifier,
            shape = OrbitShapeTokens.card,
            color = colors.surface,
            border = border,
            shadowElevation = 1.dp
        ) {
            Column(modifier = Modifier.padding(contentPadding), content = content)
        }
    }
}
