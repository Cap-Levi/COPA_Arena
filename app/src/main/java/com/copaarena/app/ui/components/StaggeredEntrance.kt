package com.copaarena.app.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.slideInVertically
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/** Fades + slides an item up on first composition, staggered by [index] so a list "cascades" in
 * instead of popping in all at once. */
@Composable
fun StaggeredEntrance(
    index: Int,
    modifier: Modifier = Modifier,
    staggerMillis: Long = 40L,
    content: @Composable () -> Unit
) {
    var visible by remember(index) { mutableStateOf(false) }
    LaunchedEffect(index) {
        delay(index * staggerMillis)
        visible = true
    }
    AnimatedVisibility(
        visible = visible,
        modifier = modifier,
        enter = fadeIn(tween(250)) + slideInVertically(
            animationSpec = tween(250),
            initialOffsetY = { it / 4 }
        )
    ) {
        content()
    }
}
