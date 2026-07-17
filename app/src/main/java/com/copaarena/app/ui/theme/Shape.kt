package com.copaarena.app.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.ui.unit.dp

val Shapes = Shapes(
    small = RoundedCornerShape(8.dp),      // chips, badges, small buttons
    medium = RoundedCornerShape(12.dp),    // buttons, text fields, standard inputs
    large = RoundedCornerShape(16.dp),     // cards (CopaCard default)
    extraLarge = RoundedCornerShape(28.dp) // bottom sheets, full dialogs
)
