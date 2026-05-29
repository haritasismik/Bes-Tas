package com.haritasismik.bestas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.haritasismik.bestas.ui.theme.*

data class LeaderboardEntry(
    val name: String,
    val wins: Int,
    val rank: Int
)

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit
) {
    // Örnek veri - Firebase'den gelecek
    val entries = remember {
        listOf(
            LeaderboardEntry("Ahmet", 42, 1),
            LeaderboardEntry("Ayşe", 38, 2),
            LeaderboardEntry("Mehmet", 35, 3),
            LeaderboardEntry("Fatma", 29, 4),
            LeaderboardEntry("Ali", 25, 5),
        )
    }

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

            Spacer(modifier = Modifier.height(16.dp))

            Text(
                text = "🏆 Sıralama",
                fontSize = 32.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(24.dp))

            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    LeaderboardRow(entry = entry, index = index)
                }
            }

            if (entries.isEmpty()) {
                Box(
                    modifier = Modifier.fillMaxSize(),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = "Henüz sıralama verisi yok.\nOnline oyna ve sıralamaya gir!",
                        color = CreamWhite.copy(alpha = 0.6f),
                        textAlign = TextAlign.Center,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun LeaderboardRow(
    entry: LeaderboardEntry,
    index: Int
) {
    val medalEmoji = when (index) {
        0 -> "🥇"
        1 -> "🥈"
        2 -> "🥉"
        else -> "${index + 1}."
    }

    val bgColor = when (index) {
        0 -> GoldAccent.copy(alpha = 0.15f)
        1 -> Color.Gray.copy(alpha = 0.1f)
        2 -> Color(0xFFCD7F32).copy(alpha = 0.1f)
        else -> Color.Transparent
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(12.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF3A2A1A)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .background(bgColor)
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = medalEmoji,
                fontSize = 24.sp,
                modifier = Modifier.width(48.dp)
            )

            Text(
                text = entry.name,
                color = CreamWhite,
                fontSize = 18.sp,
                fontWeight = FontWeight.Medium,
                modifier = Modifier.weight(1f)
            )

            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "${entry.wins}",
                    color = GoldAccent,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "galibiyet",
                    color = CreamWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }
        }
    }
}
