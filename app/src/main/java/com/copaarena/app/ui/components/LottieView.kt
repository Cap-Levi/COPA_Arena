package com.copaarena.app.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.rememberLottieComposition

@Composable
fun LottieView(
    assetName: String,
    modifier: Modifier = Modifier,
    iterations: Int = 1,
    isPlaying: Boolean = true
) {
    val compositionResult = rememberLottieComposition(LottieCompositionSpec.Asset("lottie/$assetName"))
    val composition by compositionResult

    if (compositionResult.isFailure) {
        val alpha by animateFloatAsState(
            targetValue = if (isPlaying) 1f else 0f,
            animationSpec = tween(300),
            label = "LottieFallbackAlpha"
        )
        Box(modifier = modifier.alpha(alpha))
    } else {
        LottieAnimation(
            composition = composition,
            modifier = modifier,
            iterations = iterations,
            isPlaying = isPlaying
        )
    }
}
