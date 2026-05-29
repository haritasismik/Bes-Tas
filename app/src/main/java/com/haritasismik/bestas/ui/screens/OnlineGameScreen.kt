package com.haritasismik.bestas.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haritasismik.bestas.data.repository.OnlineConnectionState
import com.haritasismik.bestas.ui.theme.*

/**
 * Online eşleşme bekleme ekranı
 */
@Composable
fun OnlineMatchmakingScreen(
    connectionState: OnlineConnectionState,
    onCancel: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "searching")
    val pulseScale by infiniteTransition.animateFloat(
        initialValue = 0.8f,
        targetValue = 1.2f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse"
    )

    val dotAnimation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "dots"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF1A1A2E),
                        Color(0xFF16213E),
                        Color(0xFF0F3460)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier.padding(32.dp)
        ) {
            when (connectionState) {
                is OnlineConnectionState.Searching -> {
                    SearchingUI(pulseScale, dotAnimation)
                }
                is OnlineConnectionState.WaitingForOpponent -> {
                    WaitingUI(pulseScale, dotAnimation)
                }
                is OnlineConnectionState.Error -> {
                    ErrorUI(connectionState.message)
                }
                else -> {}
            }

            Spacer(modifier = Modifier.height(48.dp))

            // İptal butonu
            OutlinedButton(
                onClick = onCancel,
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.outlinedButtonColors(
                    contentColor = CreamWhite
                )
            ) {
                Text("İptal", fontSize = 16.sp)
            }
        }
    }
}

@Composable
private fun SearchingUI(pulseScale: Float, dotAnimation: Float) {
    val dots = ".".repeat(dotAnimation.toInt() + 1)

    // Animasyonlu arama ikonu
    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(pulseScale)
            .background(
                GoldAccent.copy(alpha = 0.2f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "🔍",
            fontSize = 48.sp
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Rakip Aranıyor$dots",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = CreamWhite,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Uygun bir rakip bulunuyor...",
        fontSize = 14.sp,
        color = CreamWhite.copy(alpha = 0.6f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun WaitingUI(pulseScale: Float, dotAnimation: Float) {
    val dots = ".".repeat(dotAnimation.toInt() + 1)

    Box(
        modifier = Modifier
            .size(120.dp)
            .scale(pulseScale)
            .background(
                PistachioGreen.copy(alpha = 0.2f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "⏳",
            fontSize = 48.sp
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Rakip Bekleniyor$dots",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = CreamWhite,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = "Oda oluşturuldu, rakip katılmasını bekliyoruz",
        fontSize = 14.sp,
        color = CreamWhite.copy(alpha = 0.6f),
        textAlign = TextAlign.Center
    )
}

@Composable
private fun ErrorUI(message: String) {
    Box(
        modifier = Modifier
            .size(120.dp)
            .background(
                DeepRed.copy(alpha = 0.2f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = "❌",
            fontSize = 48.sp
        )
    }

    Spacer(modifier = Modifier.height(32.dp))

    Text(
        text = "Bağlantı Hatası",
        fontSize = 24.sp,
        fontWeight = FontWeight.Bold,
        color = CreamWhite,
        textAlign = TextAlign.Center
    )

    Spacer(modifier = Modifier.height(8.dp))

    Text(
        text = message,
        fontSize = 14.sp,
        color = CreamWhite.copy(alpha = 0.6f),
        textAlign = TextAlign.Center
    )
}
