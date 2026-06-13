package com.orbit.mobile.core.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Shape tokens
val OrbitShapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(12.dp),
    large = RoundedCornerShape(14.dp),
    extraLarge = RoundedCornerShape(20.dp)
)

// Named shapes
object OrbitShapeTokens {
    val input = RoundedCornerShape(8.dp)
    val button = RoundedCornerShape(10.dp)
    val card = RoundedCornerShape(12.dp)
    val sheet = RoundedCornerShape(14.dp)
    val chip = RoundedCornerShape(20.dp)
}
