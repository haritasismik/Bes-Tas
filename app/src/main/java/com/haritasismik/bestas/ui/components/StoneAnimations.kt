package com.haritasismik.bestas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.runtime.*
import androidx.compose.ui.geometry.Offset
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

    // Parabolik yörünge hesaplama
    val yOffset = -4f * peakHeight * currentProgress * (1f - currentProgress)
    val rotation = currentProgress * 720f  // 2 tam tur dönsün
    val scale = 1f + 0.3f * sin(currentProgress * PI.toFloat())  // Havadayken biraz büyüsün

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
 * Taş yakalama animasyonu - el kapanması efekti
 */
@Composable
fun rememberCatchAnimation(
    isActive: Boolean,
    duration: Int = 400
): CatchAnimationState {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            )
        }
    }

    val currentProgress = progress.value
    // Hızla küçülsün (elde kaybolsun) ve sonra hafif bounce
    val scale = if (currentProgress < 0.5f) {
        1f - currentProgress * 1.5f
    } else {
        0.25f + (currentProgress - 0.5f) * 0.1f
    }

    val alpha = 1f - currentProgress

    return CatchAnimationState(
        progress = currentProgress,
        scale = scale.coerceAtLeast(0f),
        alpha = alpha.coerceAtLeast(0f),
        isAnimating = isActive && currentProgress < 1f
    )
}

data class CatchAnimationState(
    val progress: Float,
    val scale: Float,
    val alpha: Float,
    val isAnimating: Boolean
)

/**
 * Taş toplama animasyonu - yerden kaldırma efekti
 */
@Composable
fun rememberPickUpAnimation(
    isActive: Boolean,
    delay: Int = 0,
    duration: Int = 300
): PickUpAnimationState {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            kotlinx.coroutines.delay(delay.toLong())
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = FastOutSlowInEasing
                )
            )
        } else {
            progress.snapTo(0f)
        }
    }

    val currentProgress = progress.value
    val yOffset = -80f * currentProgress  // Yukarı kalksın
    val scale = 1f - 0.6f * currentProgress  // Küçülsün
    val alpha = 1f - currentProgress

    return PickUpAnimationState(
        progress = currentProgress,
        yOffset = yOffset,
        scale = scale,
        alpha = alpha,
        isAnimating = isActive && currentProgress < 1f
    )
}

data class PickUpAnimationState(
    val progress: Float,
    val yOffset: Float,
    val scale: Float,
    val alpha: Float,
    val isAnimating: Boolean
)

/**
 * Taş düşürme animasyonu - başarısız hamle
 */
@Composable
fun rememberDropAnimation(
    isActive: Boolean,
    duration: Int = 600
): DropAnimationState {
    val progress = remember { Animatable(0f) }

    LaunchedEffect(isActive) {
        if (isActive) {
            progress.snapTo(0f)
            progress.animateTo(
                targetValue = 1f,
                animationSpec = tween(
                    durationMillis = duration,
                    easing = BounceEasing
                )
            )
        }
    }

    val currentProgress = progress.value
    // Yerçekimi ile düşüş + bounce
    val yOffset = 200f * currentProgress
    val rotation = currentProgress * 180f
    val bounceScale = if (currentProgress > 0.8f) {
        1f + 0.1f * sin((currentProgress - 0.8f) * 5f * PI.toFloat())
    } else {
        1f
    }

    return DropAnimationState(
        progress = currentProgress,
        yOffset = yOffset,
        rotation = rotation,
        scale = bounceScale,
        isAnimating = isActive && currentProgress < 1f
    )
}

data class DropAnimationState(
    val progress: Float,
    val yOffset: Float,
    val rotation: Float,
    val scale: Float,
    val isAnimating: Boolean
)

/**
 * Taş dağıtma animasyonu - yeni tur başlangıcı
 */
@Composable
fun rememberScatterAnimation(
    isActive: Boolean,
    targetX: Float,
    targetY: Float,
    delay: Int = 0
): ScatterAnimationState {
    val xAnim = remember { Animatable(500f) }  // Ortadan başla
    val yAnim = remember { Animatable(200f) }  // Üstten başla

    LaunchedEffect(isActive, targetX, targetY) {
        if (isActive) {
            kotlinx.coroutines.delay(delay.toLong())
            // X ve Y paralel animate
            kotlinx.coroutines.launch {
                xAnim.animateTo(
                    targetValue = targetX,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioLowBouncy,
                        stiffness = Spring.StiffnessLow
                    )
                )
            }
            yAnim.animateTo(
                targetValue = targetY,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioLowBouncy,
                    stiffness = Spring.StiffnessLow
                )
            )
        }
    }

    return ScatterAnimationState(
        x = xAnim.value,
        y = yAnim.value,
        isAnimating = isActive
    )
}

data class ScatterAnimationState(
    val x: Float,
    val y: Float,
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
            animation = tween(500, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "sparkle_alpha"
    )

    val sparkleScale by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1.5f,
        animationSpec = infiniteRepeatable(
            animation = tween(700, easing = EaseInOutSine),
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
