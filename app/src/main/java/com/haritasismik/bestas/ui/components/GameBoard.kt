package com.haritasismik.bestas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
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
 * Oyun tahtası - parmakla çizme mekanizması + gerçekçi fıstık/taş
 */
@Composable
fun GameBoard(
    gameState: GameState,
    onStoneClicked: (Int) -> Unit,
    onBoardClicked: (Position) -> Unit,
    onSwipeStones: (List<Int>) -> Unit = {},
    modifier: Modifier = Modifier
) {
    // Çizme yolu (swipe sırasında parmağın geçtiği taşlar)
    var swipePath by remember { mutableStateOf(listOf<Offset>()) }
    var swipedStoneIds by remember { mutableStateOf(setOf<Int>()) }
    var isDragging by remember { mutableStateOf(false) }

    // Havadaki heneke animasyonu
    val infiniteTransition = rememberInfiniteTransition(label = "board_anim")
    val henekeBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "heneke_bounce"
    )

    // Arka plan toz partikülleri
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
                        .filter { !it.isPickedUp && !it.isInAir }
                        .find { stone ->
                            val stoneX = stone.position.x * scaleX
                            val stoneY = stone.position.y * scaleY
                            val dx = offset.x - stoneX
                            val dy = offset.y - stoneY
                            (dx * dx + dy * dy) < (GameEngine.TOUCH_RADIUS * scaleX).let { it * it }
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
            .pointerInput(gameState) {
                // Parmakla çizme (swipe) - taş toplama
                detectDragGestures(
                    onDragStart = { offset ->
                        swipePath = listOf(offset)
                        swipedStoneIds = emptySet()
                        isDragging = true
                    },
                    onDrag = { change, _ ->
                        val pos = change.position
                        swipePath = swipePath + pos

                        // Parmak bir taşın üzerinden geçti mi?
                        val boardWidth = size.width.toFloat()
                        val boardHeight = size.height.toFloat()
                        val scaleX = boardWidth / GameEngine.BOARD_WIDTH
                        val scaleY = boardHeight / GameEngine.BOARD_HEIGHT

                        gameState.stones
                            .filter { !it.isPickedUp && !it.isInAir && it.id != gameState.henekeId }
                            .forEach { stone ->
                                val stoneX = stone.position.x * scaleX
                                val stoneY = stone.position.y * scaleY
                                val dx = pos.x - stoneX
                                val dy = pos.y - stoneY
                                val dist = sqrt(dx * dx + dy * dy)
                                if (dist < GameEngine.TOUCH_RADIUS * scaleX) {
                                    swipedStoneIds = swipedStoneIds + stone.id
                                }
                            }
                    },
                    onDragEnd = {
                        isDragging = false
                        if (swipedStoneIds.isNotEmpty()) {
                            onSwipeStones(swipedStoneIds.toList())
                        }
                        swipePath = emptyList()
                        swipedStoneIds = emptySet()
                    },
                    onDragCancel = {
                        isDragging = false
                        swipePath = emptyList()
                        swipedStoneIds = emptySet()
                    }
                )
            }
    ) {
        val boardWidth = size.width
        val boardHeight = size.height
        val scaleX = boardWidth / GameEngine.BOARD_WIDTH
        val scaleY = boardHeight / GameEngine.BOARD_HEIGHT

        // Ahşap zemin
        drawWoodenFloor(boardWidth, boardHeight)

        // Toz efekti
        drawDustParticles(boardWidth, boardHeight, dustParticles)

        // Taşları çiz
        gameState.stones.forEach { stone ->
            if (!stone.isPickedUp) {
                val baseX = stone.position.x * scaleX
                val baseY = stone.position.y * scaleY

                val x: Float
                val y: Float

                if (stone.isInAir) {
                    // Heneke havada - yukarıda zıplıyor
                    x = baseX
                    y = 80f + henekeBounce * 40f

                    // Gölge (yerde)
                    drawOval(
                        color = Color.Black.copy(alpha = 0.2f),
                        topLeft = Offset(baseX - 30f, baseY - 15f),
                        size = Size(60f, 30f)
                    )
                } else {
                    x = baseX
                    y = baseY
                }

                // Swipe sırasında seçilen taşlara parlama efekti
                val isHighlighted = stone.id in swipedStoneIds

                when (gameState.stoneStyle) {
                    StoneStyle.REALISTIC -> drawRealisticStone(x, y, stone.rotation, scaleX, stone.isHeneke, isHighlighted)
                    StoneStyle.PISTACHIO -> drawRealisticPistachio(x, y, stone.rotation, scaleX, stone.isHeneke, isHighlighted)
                }
            }
        }

        // Swipe çizgisi (parmağın geçtiği yol)
        if (isDragging && swipePath.size > 1) {
            val path = Path().apply {
                moveTo(swipePath.first().x, swipePath.first().y)
                for (i in 1 until swipePath.size) {
                    lineTo(swipePath[i].x, swipePath[i].y)
                }
            }
            drawPath(
                path = path,
                color = Color(0xFFFFD700).copy(alpha = 0.6f),
                style = Stroke(
                    width = 6f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}

/**
 * Gerçekçi taş çizimi (büyük)
 */
private fun DrawScope.drawRealisticStone(
    x: Float, y: Float, rotation: Float, scale: Float,
    isHeneke: Boolean, isHighlighted: Boolean
) {
    val stoneSize = GameEngine.STONE_SIZE * scale

    rotate(rotation, pivot = Offset(x, y)) {
        // Seçim parlaması
        if (isHighlighted) {
            drawOval(
                color = Color(0xFFFFD700).copy(alpha = 0.4f),
                topLeft = Offset(x - stoneSize * 0.5f, y - stoneSize * 0.4f),
                size = Size(stoneSize * 1f, stoneSize * 0.8f)
            )
        }

        // Heneke işareti (altın halka)
        if (isHeneke) {
            drawOval(
                color = Color(0xFFFFD700).copy(alpha = 0.6f),
                topLeft = Offset(x - stoneSize * 0.48f, y - stoneSize * 0.38f),
                size = Size(stoneSize * 0.96f, stoneSize * 0.76f),
                style = Stroke(width = 4f)
            )
        }

        // Gölge
        drawOval(
            color = Color.Black.copy(alpha = 0.25f),
            topLeft = Offset(x - stoneSize * 0.42f + 5f, y - stoneSize * 0.3f + 5f),
            size = Size(stoneSize * 0.84f, stoneSize * 0.62f)
        )

        // Ana taş gövdesi
        drawOval(
            color = Color(0xFF5A5A5A),
            topLeft = Offset(x - stoneSize * 0.42f, y - stoneSize * 0.32f),
            size = Size(stoneSize * 0.84f, stoneSize * 0.64f)
        )

        // Orta katman
        drawOval(
            color = Color(0xFF787878),
            topLeft = Offset(x - stoneSize * 0.36f, y - stoneSize * 0.26f),
            size = Size(stoneSize * 0.72f, stoneSize * 0.5f)
        )

        // Işık yansıması
        drawOval(
            color = Color(0xFFA0A0A0).copy(alpha = 0.5f),
            topLeft = Offset(x - stoneSize * 0.22f, y - stoneSize * 0.2f),
            size = Size(stoneSize * 0.4f, stoneSize * 0.22f)
        )

        // Parlak nokta
        drawCircle(
            color = Color.White.copy(alpha = 0.4f),
            radius = stoneSize * 0.07f,
            center = Offset(x - stoneSize * 0.1f, y - stoneSize * 0.12f)
        )
    }
}

/**
 * Gerçekçi fıstık çizimi - fıstık kabuğu şeklinde (büyük, detaylı)
 */
private fun DrawScope.drawRealisticPistachio(
    x: Float, y: Float, rotation: Float, scale: Float,
    isHeneke: Boolean, isHighlighted: Boolean
) {
    val s = GameEngine.STONE_SIZE * scale

    rotate(rotation, pivot = Offset(x, y)) {
        // Seçim parlaması
        if (isHighlighted) {
            drawOval(
                color = Color(0xFFFFD700).copy(alpha = 0.4f),
                topLeft = Offset(x - s * 0.55f, y - s * 0.42f),
                size = Size(s * 1.1f, s * 0.84f)
            )
        }

        // Heneke işareti
        if (isHeneke) {
            drawOval(
                color = Color(0xFFFFD700).copy(alpha = 0.7f),
                topLeft = Offset(x - s * 0.52f, y - s * 0.4f),
                size = Size(s * 1.04f, s * 0.8f),
                style = Stroke(width = 4f)
            )
        }

        // === FISTIK KABUĞU ===
        // Gölge
        drawOval(
            color = Color.Black.copy(alpha = 0.3f),
            topLeft = Offset(x - s * 0.44f + 5f, y - s * 0.32f + 5f),
            size = Size(s * 0.88f, s * 0.64f)
        )

        // Kabuk dış kontur (koyu kahve kenar)
        drawOval(
            color = Color(0xFF6B4513),
            topLeft = Offset(x - s * 0.46f, y - s * 0.34f),
            size = Size(s * 0.92f, s * 0.68f)
        )

        // Kabuk ana gövde (sıcak bej - fıstık kabuğu rengi)
        drawOval(
            color = Color(0xFFD4A862),
            topLeft = Offset(x - s * 0.42f, y - s * 0.3f),
            size = Size(s * 0.84f, s * 0.6f)
        )

        // Kabuk üst açık ton
        drawOval(
            color = Color(0xFFE8C88A).copy(alpha = 0.7f),
            topLeft = Offset(x - s * 0.34f, y - s * 0.24f),
            size = Size(s * 0.6f, s * 0.38f)
        )

        // Kabuk doku çizgileri (dikey ince çizgiler - gerçekçi kabuk dokusu)
        val lineColor = Color(0xFFB8894A).copy(alpha = 0.4f)
        for (i in -2..2) {
            val offsetX = i * s * 0.08f
            drawLine(
                color = lineColor,
                start = Offset(x + offsetX, y - s * 0.2f),
                end = Offset(x + offsetX + s * 0.02f, y + s * 0.18f),
                strokeWidth = 1.5f
            )
        }

        // === FISTIK ÇATLAĞI (açık yarık) ===
        // Koyu çatlak çizgisi
        drawArc(
            color = Color(0xFF3D2810),
            startAngle = -20f,
            sweepAngle = 40f,
            useCenter = false,
            topLeft = Offset(x - s * 0.3f, y - s * 0.05f),
            size = Size(s * 0.6f, s * 0.2f),
            style = Stroke(width = 3.5f)
        )

        // === YEŞİL İÇ KISIM (fıstığın kendisi - çatlaktan görünen) ===
        drawOval(
            color = Color(0xFF4CAF50),
            topLeft = Offset(x - s * 0.2f, y - s * 0.02f),
            size = Size(s * 0.4f, s * 0.18f)
        )

        // Yeşil iç parlama
        drawOval(
            color = Color(0xFF81C784).copy(alpha = 0.7f),
            topLeft = Offset(x - s * 0.12f, y + s * 0.01f),
            size = Size(s * 0.22f, s * 0.09f)
        )

        // Kabuk ışık yansıması (üst sol)
        drawCircle(
            color = Color.White.copy(alpha = 0.35f),
            radius = s * 0.07f,
            center = Offset(x - s * 0.18f, y - s * 0.16f)
        )

        // İkinci küçük parlama
        drawCircle(
            color = Color.White.copy(alpha = 0.2f),
            radius = s * 0.04f,
            center = Offset(x - s * 0.08f, y - s * 0.2f)
        )
    }
}

/**
 * Ahşap zemin
 */
private fun DrawScope.drawWoodenFloor(width: Float, height: Float) {
    drawRect(color = Color(0xFF8B6914), size = Size(width, height))

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
        drawLine(
            color = Color(0xFF5C4033).copy(alpha = 0.4f),
            start = Offset(0f, y),
            end = Offset(width, y),
            strokeWidth = 1.5f
        )
    }

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

    val padding = 20f
    drawRoundRect(
        color = Color(0xFF5C4033).copy(alpha = 0.5f),
        topLeft = Offset(padding, padding),
        size = Size(width - padding * 2, height - padding * 2),
        cornerRadius = CornerRadius(16f, 16f),
        style = Stroke(width = 3f)
    )
}

/**
 * Toz partikülleri
 */
private fun DrawScope.drawDustParticles(width: Float, height: Float, progress: Float) {
    for (i in 0 until 6) {
        val baseX = (width / 6) * i + 20f
        val phase = (progress + i * 0.15f) % 1f
        val y = height * (1f - phase)
        val alpha = sin(phase * PI.toFloat()) * 0.12f

        drawCircle(
            color = Color(0xFFDAA520).copy(alpha = alpha),
            radius = 2.5f,
            center = Offset(baseX + sin(phase * 3f) * 12f, y)
        )
    }
}
