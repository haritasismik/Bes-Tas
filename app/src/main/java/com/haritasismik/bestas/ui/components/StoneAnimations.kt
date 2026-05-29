package com.haritasismik.bestas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlin.math.*

/**
 * Taş fırlatma animasyonu - parabolik yörünge
 */
@Composable
fun rememberThrowAnimation(
    isActive: Boolean,
    startPosition: Offset,
    peakHeight: Float = 400f,
    duration: Int = 800
): ThrowAnimationState {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = LinearEasing
                )
            )
        } else {
            progress.snapTo(0f)
        }
    }

    val currentProgress = progress.value

    val yOffset = -4f * peakHeight * currentProgress * (1f - currentProgress)
    val rotation = currentProgress * 720f
    val scale = 1f + 0.3f * sin(currentProgress * PI.toFloat())

    return ThrowAnimationState(
        progress = currentProgress,
        yOffset = yOffset,
        rotation = rotation,
        scale = scale,
        isAnimating = isActive && currentProgress < 1f
    )
}

data class ThrowAnimationState(
    val progress: Float,
    val yOffset: Float,
    val rotation: Float,
    val scale: Float,
    val isAnimating: Boolean
)

/**
 * Parıltı/sparkle efekti - başarılı hamle
 */
@Composable
fun rememberSparkleAnimation(
    isActive: Boolean
): SparkleAnimationState {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle")

    val sparkleAlpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_alpha"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_scale"
    )

    return SparkleAnimationState(
        alpha = if (isActive) sparkleAlpha else 0f,
        scale = if (isActive) sparkleScale else 1f,
        isActive = isActive
    )
}

data class SparkleAnimationState(
    val alpha: Float,
    val scale: Float,
    val isActive: Boolean
)

/**
 * Bounce easing - yerçekimi ile düşüş simülasyonu
 */
val BounceEasing = Easing { fraction ->
    val n1 = 7.5625f
    val d1 = 2.75f
    var t = fraction

    when {
        t < 1f / d1 -> n1 * t * t
        t < 2f / d1 -> {
            t -= 1.5f / d1
            n1 * t * t + 0.75f
        }
        t < 2.5f / d1 -> {
            t -= 2.25f / d1
            n1 * t * t + 0.9375f
        }
        else -> {
            t -= 2.625f / d1
            n1 * t * t + 0.984375f
        }
    }
}

/**
 * Sine easing
 */
val EaseInOutSine = Easing { fraction ->
    -(cos(PI * fraction).toFloat() - 1f) / 2f
}
