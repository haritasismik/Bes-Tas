package com.haritasismik.bestas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haritasismik.bestas.game.models.StoneStyle
import com.haritasismik.bestas.ui.theme.*

@Composable
fun SettingsScreen(
    currentStoneStyle: StoneStyle,
    onStoneStyleChange: (StoneStyle) -> Unit,
    onBackClick: () -> Unit
) {
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
            )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(24.dp)
        ) {
            // Geri butonu
            TextButton(onClick = onBackClick) {
                Text("← Geri", color = CreamWhite, fontSize = 16.sp)
            }

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = "⚙️ Ayarlar",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )

            Spacer(modifier = Modifier.height(32.dp))

            // Taş stili seçimi
            Text(
                text = "Taş Stili",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = CreamWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(16.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                StoneStyleCard(
                    title = "Gerçekçi Taş",
                    emoji = "🪨",
                    isSelected = currentStoneStyle == StoneStyle.REALISTIC,
                    onClick = { onStoneStyleChange(StoneStyle.REALISTIC) },
                    modifier = Modifier.weight(1f)
                )

                StoneStyleCard(
                    title = "Fıstık",
                    emoji = "🥜",
                    isSelected = currentStoneStyle == StoneStyle.PISTACHIO,
                    onClick = { onStoneStyleChange(StoneStyle.PISTACHIO) },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(32.dp))

            // Zorluk seviyesi (AI için)
            Text(
                text = "AI Zorluk Seviyesi",
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                color = CreamWhite
            )

            Spacer(modifier = Modifier.height(16.dp))

            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                DifficultyOption("Kolay", "Başlangıç seviyesi", true)
                DifficultyOption("Orta", "Dengeli rakip", false)
                DifficultyOption("Zor", "Usta seviye", false)
            }
        }
    }
}

@Composable
private fun StoneStyleCard(
    title: String,
    emoji: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier
            .aspectRatio(1f)
            .clickable { onClick() }
            .then(
                if (isSelected) Modifier.border(
                    3.dp,
                    GoldAccent,
                    RoundedCornerShape(16.dp)
                ) else Modifier
            ),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkWood else Color(0xFF3A2A1A)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(text = emoji, fontSize = 48.sp)
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = title,
                color = CreamWhite,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
private fun DifficultyOption(
    title: String,
    description: String,
    isSelected: Boolean
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { },
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) DarkWood.copy(alpha = 0.8f) else Color(0xFF3A2A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(
                selected = isSelected,
                onClick = { },
                colors = RadioButtonDefaults.colors(
                    selectedColor = GoldAccent
                )
            )
            Spacer(modifier = Modifier.width(12.dp))
            Column {
                Text(text = title, color = CreamWhite, fontWeight = FontWeight.Medium)
                Text(text = description, color = CreamWhite.copy(alpha = 0.6f), fontSize = 12.sp)
            }
        }
    }
}
