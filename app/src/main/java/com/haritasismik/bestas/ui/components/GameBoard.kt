package com.haritasismik.bestas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.*
import androidx.compose.ui.input.pointer.pointerInput
import com.haritasismik.bestas.game.engine.GameEngine
import com.haritasismik.bestas.game.models.*
import kotlin.math.cos
import kotlin.math.sin

/**
 * Oyun tahtası - Canvas ile gerçekçi çizim
 */
@Composable
fun GameBoard(
    gameState: GameState,
    onStoneClicked: (Int) -> Unit,
    onBoardClicked: (Position) -> Unit,
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "stone_anim")

    // Havadaki taş animasyonu
    val throwAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = EaseInOutQuad),
            repeatMode = RepeatMode.Reverse
        ),
        label = "throw"
    )

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInput(gameState) {
                detectTapGestures { offset ->
                    val boardWidth = size.width.toFloat()
                    val boardHeight = size.height.toFloat()

                    // Ölçekleme
                    val scaleX = boardWidth / GameEngine.BOARD_WIDTH
                    val scaleY = boardHeight / GameEngine.BOARD_HEIGHT

                    // Tıklanan taşı bul
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

        // Taşları çiz
        gameState.stones.forEach { stone ->
            if (!stone.isPickedUp) {
                val x = stone.position.x * scaleX
                val y = if (stone.isInAir) {
                    stone.position.y * scaleY - (150f * throwAnimation)
                } else {
                    stone.position.y * scaleY
                }

                when (gameState.stoneStyle) {
                    StoneStyle.REALISTIC -> drawRealisticStone(x, y, stone.rotation, scaleX)
                    StoneStyle.PISTACHIO -> drawPistachio(x, y, stone.rotation, scaleX)
                }

                // Gölge
                if (stone.isInAir) {
                    drawCircle(
                        color = Color.Black.copy(alpha = 0.3f),
                        radius = GameEngine.STONE_SIZE * scaleX * 0.5f,
                        center = Offset(x, stone.position.y * scaleY),
                    )
                }
            }
        }
    }
}

/**
 * Ahşap zemin çizimi
 */
private fun DrawScope.drawWoodenFloor(width: Float, height: Float) {
    // Arka plan - koyu ahşap
    drawRect(
        color = Color(0xFF8B6914),
        size = Size(width, height)
    )

    // Ahşap damarları
    val lineColor = Color(0xFF6B4C12).copy(alpha = 0.3f)
    for (i in 0..20) {
        val y = (height / 20) * i
        drawLine(
            color = lineColor,
            start = Offset(0f, y),
            end = Offset(width, y + 10f),
            strokeWidth = 2f
        )
    }

    // Oyun alanı çerçevesi
    val padding = 30f
    drawRoundRect(
        color = Color(0xFF5C4033),
        topLeft = Offset(padding, padding),
        size = Size(width - padding * 2, height - padding * 2),
        cornerRadius = androidx.compose.ui.geometry.CornerRadius(20f, 20f),
        style = Stroke(width = 4f)
    )
}

/**
 * Gerçekçi taş çizimi
 */
private fun DrawScope.drawRealisticStone(x: Float, y: Float, rotation: Float, scale: Float) {
    val stoneSize = GameEngine.STONE_SIZE * scale

    rotate(rotation, pivot = Offset(x, y)) {
        // Gölge
        drawOval(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(x - stoneSize * 0.4f + 3f, y - stoneSize * 0.3f + 3f),
            size = Size(stoneSize * 0.8f, stoneSize * 0.6f)
        )

        // Ana taş gövdesi
        drawOval(
            color = Color(0xFF7A7A7A),
            topLeft = Offset(x - stoneSize * 0.4f, y - stoneSize * 0.3f),
            size = Size(stoneSize * 0.8f, stoneSize * 0.6f)
        )

        // Açık yansıma (3D efekti)
        drawOval(
            color = Color(0xFFA0A0A0).copy(alpha = 0.5f),
            topLeft = Offset(x - stoneSize * 0.25f, y - stoneSize * 0.2f),
            size = Size(stoneSize * 0.4f, stoneSize * 0.25f)
        )

        // Parlak nokta
        drawCircle(
            color = Color.White.copy(alpha = 0.3f),
            radius = stoneSize * 0.08f,
            center = Offset(x - stoneSize * 0.1f, y - stoneSize * 0.1f)
        )
    }
}

/**
 * Fıstık çizimi
 */
private fun DrawScope.drawPistachio(x: Float, y: Float, rotation: Float, scale: Float) {
    val stoneSize = GameEngine.STONE_SIZE * scale

    rotate(rotation, pivot = Offset(x, y)) {
        // Gölge
        drawOval(
            color = Color.Black.copy(alpha = 0.2f),
            topLeft = Offset(x - stoneSize * 0.35f + 3f, y - stoneSize * 0.25f + 3f),
            size = Size(stoneSize * 0.7f, stoneSize * 0.5f)
        )

        // Kabuk (bej/krem rengi)
        drawOval(
            color = Color(0xFFE8D5B7),
            topLeft = Offset(x - stoneSize * 0.35f, y - stoneSize * 0.25f),
            size = Size(stoneSize * 0.7f, stoneSize * 0.5f)
        )

        // Yarık (fıstığın açık kısmı)
        drawLine(
            color = Color(0xFF5C4033),
            start = Offset(x - stoneSize * 0.15f, y),
            end = Offset(x + stoneSize * 0.15f, y),
            strokeWidth = 2f * scale
        )

        // Yeşil iç kısım
        drawOval(
            color = Color(0xFF93C572),
            topLeft = Offset(x - stoneSize * 0.12f, y - stoneSize * 0.05f),
            size = Size(stoneSize * 0.24f, stoneSize * 0.15f)
        )

        // Parlak nokta
        drawCircle(
            color = Color.White.copy(alpha = 0.25f),
            radius = stoneSize * 0.06f,
            center = Offset(x - stoneSize * 0.1f, y - stoneSize * 0.1f)
        )
    }
}
