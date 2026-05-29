package com.haritasismik.bestas.ui.screens

import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haritasismik.bestas.game.models.GameMode
import com.haritasismik.bestas.ui.theme.*

@Composable
fun MainMenuScreen(
    onPlayClick: (GameMode) -> Unit,
    onSettingsClick: () -> Unit,
    onLeaderboardClick: () -> Unit
) {
    val infiniteTransition = rememberInfiniteTransition(label = "menu_anim")
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.05f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = EaseInOutSine),
            repeatMode = RepeatMode.Reverse
        ),
        label = "title_scale"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2C1810),
                        Color(0xFF4A3728),
                        Color(0xFF2C1810)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(20.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            // Başlık
            Text(
                text = "Beş Taş",
                fontSize = (48 * titleScale).sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                textAlign = TextAlign.Center
            )

            Text(
                text = "Geleneksel Türk Oyunu",
                fontSize = 16.sp,
                color = LightWood,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(40.dp))

            // Oyun modu butonları
            MenuButton(
                text = "\uD83E\uDD16  Yapay Zekaya Karşı",
                onClick = { onPlayClick(GameMode.VS_AI) }
            )

            MenuButton(
                text = "\uD83D\uDC65  Yerel İki Kişi",
                onClick = { onPlayClick(GameMode.LOCAL) }
            )

            MenuButton(
                text = "\uD83C\uDF10  Online Oyna",
                onClick = { onPlayClick(GameMode.ONLINE) }
            )

            Spacer(modifier = Modifier.height(20.dp))

            // Alt butonlar
            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                SmallMenuButton(
                    text = "\u2699\uFE0F Ayarlar",
                    onClick = onSettingsClick
                )
                SmallMenuButton(
                    text = "\uD83C\uDFC6 Sıralama",
                    onClick = onLeaderboardClick
                )
            }
        }
    }
}

@Composable
private fun MenuButton(
    text: String,
    onClick: () -> Unit
) {
    Button(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(56.dp)
            .shadow(8.dp, RoundedCornerShape(16.dp)),
        shape = RoundedCornerShape(16.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = DarkWood,
            contentColor = CreamWhite
        )
    ) {
        Text(
            text = text,
            fontSize = 18.sp,
            fontWeight = FontWeight.SemiBold
        )
    }
}

@Composable
private fun SmallMenuButton(
    text: String,
    onClick: () -> Unit
) {
    OutlinedButton(
        onClick = onClick,
        shape = RoundedCornerShape(12.dp),
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = GoldAccent
        )
    ) {
        Text(
            text = text,
            fontSize = 14.sp
        )
    }
}
