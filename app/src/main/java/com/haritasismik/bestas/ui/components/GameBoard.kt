package com.haritasismik.bestas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import com.haritasismik.bestas.game.engine.GameEngine
import com.haritasismik.bestas.game.models.*
import kotlin.math.*

/**
 * Oyun tahtası - Canvas ile gerçekçi çizim + animasyonlar
 */
@Composable
fun GameBoard(
    gameState: GameState,
    onStoneClicked: (Int) -> Unit,
    onBoardClicked: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    // Havadaki taş animasyonu
    val throwAnim = rememberThrowAnimation(
        isActive = gameState.thrownStone != null,
        startPosition = Offset(500f, 700f)
    )

    // Sparkle efekti (başarılı toplama sonrası)
    val sparkle = rememberSparkleAnimation(
        isActive = gameState.stonesPickedThisTurn > 0
    )

    // Taş seçim animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "stone_anim")
    val selectedGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    // Arka plan partikülleri (toz efekti)
    val dustParticles by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(3000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dust"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gameState) {
                detectTapGestures { offset ->
                    val boardWidth = size.width.toFloat()
                    val boardHeight = size.height.toFloat()
                    val scaleX = boardWidth / GameEngine.BOARD_WIDTH
                    val scaleY = boardHeight / GameEngine.BOARD_HEIGHT

                    val clickedStone = gameState.stones
                        .filter { !it.isPickedUp }
                        .find { stone ->
                            val stoneX = stone.position.x * scaleX
                            val stoneY = stone.position.y * scaleY
                            val dx = offset.x - stoneX
                            val dy = offset.y - stoneY
                            (dx * dx + dy * dy) < (GameEngine.STONE_SIZE * scaleX * 1.5f).let { it * it }
                        }

                    if (clickedStone != null) {
                        onStoneClicked(clickedStone.id)
                    } else {
                        val gamePos = Position(
                            x = offset.x / scaleX,
                            y = offset.y / scaleY
                        )
                        onBoardClicked(gamePos)
                    }
                }
            }
    ) {
        val boardWidth = size.width
        val boardHeight = size.height
        val scaleX = boardWidth / GameEngine.BOARD_WIDTH
        val scaleY = boardHeight / GameEngine.BOARD_HEIGHT

        // Ahşap zemin çiz
        drawWoodenFloor(boardWidth, boardHeight)

        // Toz partikülleri
        drawDustParticles(boardWidth, boardHeight, dustParticles)

        // Taşları çiz
        gameState.stones.forEach { stone ->
            if (!stone.isPickedUp) {
                val baseX = stone.position.x * scaleX
                val baseY = stone.position.y * scaleY

                val x: Float
                val y: Float
                val currentRotation: Float
                val currentScale: Float
                val currentAlpha: Float

                if (stone.isInAir && throwAnim.isAnimating) {
                    // Havadaki taş - parabolik animasyon
                    x = baseX
                    y = baseY + throwAnim.yOffset
                    currentRotation = stone.rotation + throwAnim.rotation
                    currentScale = throwAnim.scale
                    currentAlpha = 1f
                } else if (stone.isInAir) {
                    // Havada ama animasyon bitti - üstte sabit
                    x = baseX
                    y = baseY - 300f
                    currentRotation = stone.rotation
                    currentScale = 0.9f
                    currentAlpha = 1f

                    // Gölge çiz (havadayken)
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.25f),
                        radius = GameEngine.STONE_SIZE * scaleX * 0.4f,
                        center = Offset(baseX + 5f, baseY + 5f)
                    )
                } else {
                    x = baseX
                    y = baseY
                    currentRotation = stone.rotation
                    currentScale = 1f
                    currentAlpha = 1f
                }

                // Taşı çiz
                drawStoneWithEffects(
                    x = x,
                    y = y,
                    rotation = currentRotation,
                    scale = currentScale * scaleX,
                    alpha = currentAlpha,
                    stoneStyle = gameState.stoneStyle,
                    isInAir = stone.isInAir
                )

                // Sparkle efekti (yakında toplanan taş varsa)
                if (sparkle.isActive && !stone.isInAir) {
                    drawSparkle(x, y, sparkle.alpha, sparkle.scale, scaleX)
                }
            }
        }

        // Havadaki taşın gölgesi (animasyon sırasında)
        if (throwAnim.isAnimating) {
            gameState.thrownStone?.let { thrown ->
                val shadowX = thrown.position.x * scaleX
                val shadowY = thrown.position.y * scaleY
                val shadowAlpha = 0.3f * (1f - throwAnim.progress * 0.5f)
                drawOval(
                    color = Color.Black.copy(alpha = shadowAlpha),
                    topLeft = Offset(shadowX - 20f, shadowY - 10f),
                    size = Size(40f, 20f)
                )
            }
        }
    }
}

/**
 * Taşı efektlerle çiz
 */
private fun DrawScope.drawStoneWithEffects(
    x: Float,
    y: Float,
    rotation: Float,
    scale: Float,
    alpha: Float,
    stoneStyle: StoneStyle,
    isInAir: Boolean
) {
    val stoneSize = GameEngine.STONE_SIZE * scale

    rotate(rotation, pivot = Offset(x, y)) {
        when (stoneStyle) {
            StoneStyle.REALISTIC -> {
                // Gölge (yerdeyken)
                if (!isInAir) {
                    drawOval(
                        color = Color.Black.copy(alpha = 0.2f * alpha),
                        topLeft = Offset(x - stoneSize * 0.4f + 4f, y - stoneSize * 0.25f + 4f),
                        size = Size(stoneSize * 0.8f, stoneSize * 0.55f)
                    )
                }

                // Ana taş gövdesi - gradient efekti
                drawOval(
                    color = Color(0xFF6B6B6B).copy(alpha = alpha),
                    topLeft = Offset(x - stoneSize * 0.4f, y - stoneSize * 0.3f),
                    size = Size(stoneSize * 0.8f, stoneSize * 0.6f)
                )

                // Üst katman - daha açık
                drawOval(
                    color = Color(0xFF8A8A8A).copy(alpha = 0.7f * alpha),
                    topLeft = Offset(x - stoneSize * 0.35f, y - stoneSize * 0.25f),
                    size = Size(stoneSize * 0.7f, stoneSize * 0.45f)
                )

                // Işık yansıması
                drawOval(
                    color = Color(0xFFB0B0B0).copy(alpha = 0.4f * alpha),
                    topLeft = Offset(x - stoneSize * 0.2f, y - stoneSize * 0.18f),
                    size = Size(stoneSize * 0.35f, stoneSize * 0.2f)
                )

                // Parlak nokta
                drawCircle(
                    color = Color.White.copy(alpha = 0.35f * alpha),
                    radius = stoneSize * 0.07f,
                    center = Offset(x - stoneSize * 0.08f, y - stoneSize * 0.1f)
                )

                // Doku çizgileri
                drawLine(
                    color = Color(0xFF555555).copy(alpha = 0.2f * alpha),
                    start = Offset(x - stoneSize * 0.2f, y),
                    end = Offset(x + stoneSize * 0.15f, y - stoneSize * 0.05f),
                    strokeWidth = 1f
                )
            }

            StoneStyle.PISTACHIO -> {
                // Gölge
                if (!isInAir) {
                    drawOval(
                        color = Color.Black.copy(alpha = 0.2f * alpha),
                        topLeft = Offset(x - stoneSize * 0.35f + 3f, y - stoneSize * 0.22f + 3f),
                        size = Size(stoneSize * 0.7f, stoneSize * 0.45f)
                    )
                }

                // Kabuk (bej/krem)
                drawOval(
                    color = Color(0xFFE8D5B7).copy(alpha = alpha),
                    topLeft = Offset(x - stoneSize * 0.35f, y - stoneSize * 0.22f),
                    size = Size(stoneSize * 0.7f, stoneSize * 0.45f)
                )

                // Kabuk üst ton
                drawOval(
                    color = Color(0xFFF0E6D0).copy(alpha = 0.5f * alpha),
                    topLeft = Offset(x - stoneSize * 0.28f, y - stoneSize * 0.18f),
                    size = Size(stoneSize * 0.5f, stoneSize * 0.3f)
                )

                // Yarık (fıstığın açık kısmı)
                drawLine(
                    color = Color(0xFF4A3520).copy(alpha = 0.8f * alpha),
                    start = Offset(x - stoneSize * 0.18f, y + stoneSize * 0.02f),
                    end = Offset(x + stoneSize * 0.18f, y + stoneSize * 0.02f),
                    strokeWidth = 2.5f
                )

                // Yeşil iç kısım (fıstığın kendisi)
                drawOval(
                    color = Color(0xFF7CB342).copy(alpha = alpha),
                    topLeft = Offset(x - stoneSize * 0.14f, y - stoneSize * 0.03f),
                    size = Size(stoneSize * 0.28f, stoneSize * 0.15f)
                )

                // Yeşil iç parlama
                drawOval(
                    color = Color(0xFFA5D65C).copy(alpha = 0.4f * alpha),
                    topLeft = Offset(x - stoneSize * 0.08f, y - stoneSize * 0.01f),
                    size = Size(stoneSize * 0.15f, stoneSize * 0.08f)
                )

                // Parlak nokta
                drawCircle(
                    color = Color.White.copy(alpha = 0.2f * alpha),
                    radius = stoneSize * 0.05f,
                    center = Offset(x - stoneSize * 0.12f, y - stoneSize * 0.1f)
                )
            }
        }
    }
}

/**
 * Ahşap zemin çizimi - geliştirilmiş versiyon
 */
private fun DrawScope.drawWoodenFloor(width: Float, height: Float) {
    // Ana arka plan rengi
    drawRect(
        color = Color(0xFF8B6914),
        size = Size(width, height)
    )

    // Ahşap tahta şeritleri
    val plankCount = 8
    val plankHeight = height / plankCount
    for (i in 0 until plankCount) {
        val y = i * plankHeight
        val shade = if (i % 2 == 0) 0.05f else 0f

        drawRect(
            color = Color.Black.copy(alpha = shade),
            topLeft = Offset(0f, y),
            size = Size(width, plankHeight)
        )

        // Tahta arası çizgi
        drawLine(
            color = Color(0xFF5C4033).copy(alpha = 0.4f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.5f
        )
    }

    // Ahşap damarları
    val grainColor = Color(0xFF6B4C12).copy(alpha = 0.15f)
    for (i in 0..30) {
        val startY = (height / 30) * i + sin(i * 0.5f) * 5f
        drawLine(
            color = grainColor,
            start = Offset(0f, startY),
            end = Offset(width, startY + 8f),
            strokeWidth = 1f
        )
    }

    // Oyun alanı - hafif kenar gölgesi
    val padding = 20f
    drawRoundRect(
        color = Color(0xFF5C4033).copy(alpha = 0.5f),
        topLeft = Offset(padding, padding),
        size = Size(width - padding * 2, height - padding * 2),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 3f)
    )

    // İç gölge efekti (üst ve sol)
    drawLine(
        color = Color.Black.copy(alpha = 0.1f),
        start = Offset(padding, padding),
        end = Offset(width - padding, padding),
        strokeWidth = 6f
    )
    drawLine(
        color = Color.Black.copy(alpha = 0.08f),
        start = Offset(padding, padding),
        end = Offset(padding, height - padding),
        strokeWidth = 4f
    )
}

/**
 * Toz partikülleri efekti
 */
private fun DrawScope.drawDustParticles(width: Float, height: Float, progress: Float) {
    val particleCount = 8
    for (i in 0 until particleCount) {
        val baseX = (width / particleCount) * i + 20f
        val phase = (progress + i * 0.13f) % 1f
        val y = height * (1f - phase)
        val alpha = sin(phase * PI.toFloat()) * 0.15f
        val size = 2f + sin(i * 1.5f) * 1.5f

        drawCircle(
            color = Color(0xFFDAA520).copy(alpha = alpha),
            radius = size,
            center = Offset(baseX + sin(phase * 3f) * 15f, y)
        )
    }
}

/**
 * Sparkle/parıltı efekti
 */
private fun DrawScope.drawSparkle(x: Float, y: Float, alpha: Float, scale: Float, scaleX: Float) {
    val sparkleSize = 4f * scale * scaleX
    val sparkleColor = Color(0xFFFFD700).copy(alpha = alpha * 0.6f)

    // 4 yönde parlama
    val offsets = listOf(
        Offset(x - 25f, y - 25f),
        Offset(x + 20f, y - 15f),
        Offset(x - 15f, y + 20f),
        Offset(x + 25f, y + 15f)
    )

    offsets.forEach { offset ->
        drawCircle(
            color = sparkleColor,
            radius = sparkleSize,
            center = offset
        )
    }
}
