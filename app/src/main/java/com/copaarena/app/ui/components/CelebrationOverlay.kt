package com.copaarena.app.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import kotlinx.coroutines.delay

/**
 * Plays confetti.json once, then calls [onFinished] after [durationMillis] so the caller can
 * reset [visible] back to false. Deliberately has no pointerInput/clickable modifier so it never
 * intercepts touches meant for the content underneath.
 */
@Composable
fun CelebrationOverlay(
    visible: Boolean,
    modifier: Modifier = Modifier,
    durationMillis: Long = 700L,
    onFinished: () -> Unit = {}
) {
    LaunchedEffect(visible) {
        if (visible) {
            delay(durationMillis)
            onFinished()
        }
    }
    if (visible) {
        LottieView(
            assetName = "confetti.json",
            modifier = modifier,
            iterations = 1,
            isPlaying = true
        )
    }
}
