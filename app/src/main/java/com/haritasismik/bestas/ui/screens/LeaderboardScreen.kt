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
import androidx.lifecycle.viewmodel.compose.viewModel
import com.haritasismik.bestas.data.firebase.LeaderboardEntry
import com.haritasismik.bestas.data.firebase.PlayerStats
import com.haritasismik.bestas.ui.theme.*

@Composable
fun LeaderboardScreen(
    onBackClick: () -> Unit,
    viewModel: LeaderboardViewModel = viewModel()
) {
    val entries by viewModel.entries.collectAsState()
    val myStats by viewModel.myStats.collectAsState()
    val myRank by viewModel.myRank.collectAsState()
    val isLoading by viewModel.isLoading.collectAsState()
    val errorMessage by viewModel.errorMessage.collectAsState()

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
            // Üst bar
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                TextButton(onClick = onBackClick) {
                    Text("← Geri", color = CreamWhite, fontSize = 16.sp)
                }
                TextButton(onClick = { viewModel.refresh() }) {
                    Text("🔄 Yenile", color = GoldAccent, fontSize = 14.sp)
                }
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

            Spacer(modifier = Modifier.height(16.dp))

            // Kendi istatistiklerimiz
            myStats?.let { stats ->
                MyStatsCard(stats = stats, rank = myRank)
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Loading
            if (isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(32.dp),
                    contentAlignment = Alignment.Center
                ) {
                    CircularProgressIndicator(color = GoldAccent)
                }
            }

            // Hata mesajı
            errorMessage?.let { error ->
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = DeepRed.copy(alpha = 0.2f)),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Text(
                        text = error,
                        modifier = Modifier.padding(12.dp),
                        color = CreamWhite,
                        textAlign = TextAlign.Center
                    )
                }
                Spacer(modifier = Modifier.height(16.dp))
            }

            // Sıralama listesi
            LazyColumn(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                itemsIndexed(entries) { index, entry ->
                    LeaderboardRow(entry = entry, index = index)
                }
            }

            if (entries.isEmpty() && !isLoading) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(48.dp),
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
private fun MyStatsCard(stats: PlayerStats, rank: LeaderboardEntry?) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = GoldAccent.copy(alpha = 0.1f)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Text(
                text = "📊 Senin İstatistiklerin",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = GoldAccent
            )

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                StatItem(label = "Sıra", value = rank?.rank?.toString() ?: "-")
                StatItem(label = "Galibiyet", value = "${stats.wins}")
                StatItem(label = "Mağlubiyet", value = "${stats.losses}")
                StatItem(label = "Oran", value = "%${(stats.winRate * 100).toInt()}")
            }
        }
    }
}

@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = value,
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = CreamWhite
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = CreamWhite.copy(alpha = 0.6f)
        )
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

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = entry.name,
                    color = CreamWhite,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = "${entry.totalGames} maç · %${(entry.winRate * 100).toInt()} oran",
                    color = CreamWhite.copy(alpha = 0.5f),
                    fontSize = 11.sp
                )
            }

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
