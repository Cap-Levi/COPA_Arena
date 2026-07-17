package com.copaarena.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.background
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.unit.dp
import com.copaarena.app.ui.theme.AccentGold
import com.copaarena.app.ui.theme.Surface

/**
 * The one shared card used everywhere: Material3 elevation (theme-adaptive shadow, no manual
 * rgba tuning needed) plus a thin gold "Stadium Lights" glow line across the top edge.
 */
@Composable
fun CopaCard(
    modifier: Modifier = Modifier,
    shape: Shape = MaterialTheme.shapes.large,
    onClick: (() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val cardColors = CardDefaults.cardColors(containerColor = Surface)
    val cardElevation = CardDefaults.cardElevation(defaultElevation = 6.dp)
    val body: @Composable ColumnScope.() -> Unit = {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(2.dp)
                .background(
                    Brush.horizontalGradient(
                        listOf(Color.Transparent, AccentGold.copy(alpha = 0.65f), Color.Transparent)
                    )
                )
        )
        content()
    }
    if (onClick != null) {
        val interactionSource = remember { MutableInteractionSource() }
        val isPressed by interactionSource.collectIsPressedAsState()
        val pressScale by animateFloatAsState(
            targetValue = if (isPressed) 0.97f else 1f,
            label = "cardPressScale"
        )
        Card(
            onClick = onClick,
            modifier = modifier.graphicsLayer(scaleX = pressScale, scaleY = pressScale),
            shape = shape,
            colors = cardColors,
            elevation = cardElevation,
            interactionSource = interactionSource,
            content = body
        )
    } else {
        Card(modifier = modifier, shape = shape, colors = cardColors, elevation = cardElevation, content = body)
    }
}
