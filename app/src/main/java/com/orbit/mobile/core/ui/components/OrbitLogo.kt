package com.orbit.mobile.core.ui.components

import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import com.orbit.mobile.R
import com.orbit.mobile.core.theme.OrbitTheme

// Logo variants
enum class OrbitLogoVariant { Mark, Full, Silver }

// Brand logo
@Composable
fun OrbitLogo(
    modifier: Modifier = Modifier,
    variant: OrbitLogoVariant = OrbitLogoVariant.Full
) {
    val colors = OrbitTheme.colors
    val res = when (variant) {
        OrbitLogoVariant.Mark -> R.drawable.orbit_logo_mark
        OrbitLogoVariant.Silver -> R.drawable.orbit_logo_silver
        OrbitLogoVariant.Full ->
            if (colors.isDark) R.drawable.orbit_logo_full_dark
            else R.drawable.orbit_logo_full_light
    }
    Image(
        painter = painterResource(res),
        contentDescription = stringResource(R.string.cd_orbit_logo),
        modifier = modifier,
        contentScale = ContentScale.Fit
    )
}
