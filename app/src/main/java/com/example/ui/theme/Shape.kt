package com.example.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

// Design System Shape tokens (rounded values from DESIGN.md)
val Shapes = Shapes(
    extraSmall = RoundedCornerShape(2.dp), // 0.125rem ≈ 2dp
    small = RoundedCornerShape(4.dp), // default 0.25rem
    medium = RoundedCornerShape(6.dp), // 0.375rem
    large = RoundedCornerShape(8.dp), // 0.5rem
    extraLarge = RoundedCornerShape(12.dp) // 0.75rem
)
