package com.haritasismik.bestas.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haritasismik.bestas.game.models.*
import com.haritasismik.bestas.ui.components.GameBoard
import com.haritasismik.bestas.ui.theme.*

@Composable
fun GameScreen(
    gameMode: GameMode,
    stoneStyle: StoneStyle,
    onBackToMenu: () -> Unit,
    viewModel: GameViewModel = viewModel()
) {
    val gameState by viewModel.gameState.collectAsState()
    val message by viewModel.message.collectAsState()
    val henekeTimeLeft by viewModel.henekeTimeLeft.collectAsState()
    val isScattering by viewModel.isScattering.collectAsState()

    // Oyunu başlat
    LaunchedEffect(gameMode, stoneStyle) {
        viewModel.startGame(gameMode, stoneStyle)
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E)
                    )
                )
            )
    ) {
        Column(
            modifier = Modifier.fillMaxSize()
        ) {
            // Üst bilgi çubuğu
            GameTopBar(
                gameState = gameState,
                onBackClick = onBackToMenu
            )

            // Heneke zamanlayıcı çubuğu
            if (henekeTimeLeft > 0f) {
                HenekeTimerBar(timeLeft = henekeTimeLeft)
            }

            // Mesaj alanı
            AnimatedVisibility(
                visible = message.isNotEmpty(),
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 4.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = GoldAccent.copy(alpha = 0.2f)
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = message,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(8.dp),
                        textAlign = TextAlign.Center,
                        color = GoldAccent,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }

            // Oyun tahtası
            Box(
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
                    .padding(8.dp)
            ) {
                GameBoard(
                    gameState = gameState,
                    onStoneClicked = { stoneId ->
                        viewModel.onStoneClicked(stoneId)
                    },
                    onBoardClicked = { position ->
                        viewModel.onBoardClicked(position)
                    },
                    onSwipeStones = { stoneIds ->
                        viewModel.onSwipeStones(stoneIds)
                    }
                )

                // Serpme animasyonu overlay
                if (isScattering) {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .background(Color.Black.copy(alpha = 0.3f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "🎲 Serpiliyor...",
                            fontSize = 28.sp,
                            color = GoldAccent,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Alt kontrol butonları
            GameBottomBar(
                gameState = gameState,
                onThrowClick = { viewModel.onThrowStone() },
                onCatchClick = { viewModel.onCatchStone() },
                onScatterClick = { viewModel.scatterStones() }
            )
        }

        // Oyun sonu dialog
        if (gameState.isGameOver) {
            GameOverDialog(
                winnerName = gameState.currentPlayer.name,
                onPlayAgain = { viewModel.startGame(gameMode, stoneStyle) },
                onBackToMenu = onBackToMenu
            )
        }
    }
}

/**
 * Heneke zamanlayıcı çubuğu - yeşilden kırmızıya değişir
 */
@Composable
private fun HenekeTimerBar(timeLeft: Float) {
    val color = when {
        timeLeft > 0.5f -> Color(0xFF4CAF50)  // Yeşil
        timeLeft > 0.25f -> Color(0xFFFF9800) // Turuncu
        else -> Color(0xFFF44336)             // Kırmızı
    }

    LinearProgressIndicator(
        progress = { timeLeft },
        modifier = Modifier
            .fillMaxWidth()
            .height(6.dp)
            .padding(horizontal = 16.dp),
        color = color,
        trackColor = Color.Gray.copy(alpha = 0.3f)
    )
}

@Composable
private fun GameTopBar(
    gameState: GameState,
    onBackClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        TextButton(onClick = onBackClick) {
            Text("← Menü", color = CreamWhite)
        }

        Column(
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = gameState.currentRound.displayName,
                color = GoldAccent,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
            Text(
                text = "${gameState.currentPlayer.name}'ın sırası",
                color = CreamWhite.copy(alpha = 0.7f),
                fontSize = 12.sp
            )
        }

        Column(
            horizontalAlignment = Alignment.End
        ) {
            Text(
                text = "${gameState.player1.name}: ${gameState.player1.score}",
                color = CreamWhite,
                fontSize = 12.sp
            )
            Text(
                text = "${gameState.player2.name}: ${gameState.player2.score}",
                color = CreamWhite,
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun GameBottomBar(
    gameState: GameState,
    onThrowClick: () -> Unit,
    onCatchClick: () -> Unit,
    onScatterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.CenterVertically
    ) {
        // Serpme butonu (heneke seçilmeden önce aktif)
        Button(
            onClick = onScatterClick,
            enabled = !gameState.isHenekeSelected && !gameState.isGameOver,
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF795548),
                contentColor = CreamWhite
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text("🎲 Serp", fontSize = 14.sp)
        }

        // Fırlat butonu
        Button(
            onClick = onThrowClick,
            enabled = gameState.isHenekeSelected && gameState.thrownStone == null && !gameState.isGameOver,
            colors = ButtonDefaults.buttonColors(
                containerColor = DarkWood,
                contentColor = CreamWhite
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text("🪨 Fırlat", fontSize = 14.sp)
        }

        // Yakala butonu
        Button(
            onClick = onCatchClick,
            enabled = gameState.thrownStone != null && !gameState.isGameOver,
            colors = ButtonDefaults.buttonColors(
                containerColor = GoldAccent,
                contentColor = Color.Black
            ),
            shape = RoundedCornerShape(12.dp),
            modifier = Modifier.height(48.dp)
        ) {
            Text("✋ Yakala", fontSize = 14.sp)
        }
    }
}

@Composable
private fun GameOverDialog(
    winnerName: String,
    onPlayAgain: () -> Unit,
    onBackToMenu: () -> Unit
) {
    AlertDialog(
        onDismissRequest = {},
        title = {
            Text(
                text = "🏆 Oyun Bitti!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
        },
        text = {
            Text(
                text = "$winnerName kazandı!",
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth(),
                fontSize = 18.sp
            )
        },
        confirmButton = {
            Button(onClick = onPlayAgain) {
                Text("Tekrar Oyna")
            }
        },
        dismissButton = {
            TextButton(onClick = onBackToMenu) {
                Text("Menüye Dön")
            }
        }
    )
}
