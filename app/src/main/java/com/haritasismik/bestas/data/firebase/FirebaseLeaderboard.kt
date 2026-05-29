package com.haritasismik.bestas.data.firebase

import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.Query
import kotlinx.coroutines.tasks.await

/**
 * Firebase Firestore ile liderboard/sıralama sistemi
 */
class FirebaseLeaderboard {

    private val firestore = FirebaseFirestore.getInstance()
    private val leaderboardRef = firestore.collection("leaderboard")
    private val playersRef = firestore.collection("players")

    /**
     * Oyuncu istatistiklerini kaydet/güncelle
     */
    suspend fun updatePlayerStats(userId: String, userName: String, won: Boolean): Result<Unit> {
        return try {
            val playerDoc = playersRef.document(userId)
            val snapshot = playerDoc.get().await()

            if (snapshot.exists()) {
                // Mevcut oyuncu - istatistikleri güncelle
                val currentWins = snapshot.getLong("wins") ?: 0
                val currentLosses = snapshot.getLong("losses") ?: 0
                val currentGames = snapshot.getLong("totalGames") ?: 0

                val updates = mutableMapOf<String, Any>(
                    "totalGames" to currentGames + 1,
                    "lastPlayed" to System.currentTimeMillis(),
                    "name" to userName
                )

                if (won) {
                    updates["wins"] = currentWins + 1
                } else {
                    updates["losses"] = currentLosses + 1
                }

                // Win rate hesapla
                val newWins = if (won) currentWins + 1 else currentWins
                val newTotal = currentGames + 1
                updates["winRate"] = if (newTotal > 0) newWins.toDouble() / newTotal else 0.0

                playerDoc.update(updates).await()
            } else {
                // Yeni oyuncu
                val playerData = mapOf(
                    "userId" to userId,
                    "name" to userName,
                    "wins" to if (won) 1L else 0L,
                    "losses" to if (won) 0L else 1L,
                    "totalGames" to 1L,
                    "winRate" to if (won) 1.0 else 0.0,
                    "joinedAt" to System.currentTimeMillis(),
                    "lastPlayed" to System.currentTimeMillis()
                )
                playerDoc.set(playerData).await()
            }

            // Leaderboard'u güncelle
            updateLeaderboard(userId, userName)

            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Leaderboard tablosunu güncelle
     */
    private suspend fun updateLeaderboard(userId: String, userName: String) {
        val playerSnapshot = playersRef.document(userId).get().await()
        val wins = playerSnapshot.getLong("wins") ?: 0
        val totalGames = playerSnapshot.getLong("totalGames") ?: 0
        val winRate = playerSnapshot.getDouble("winRate") ?: 0.0

        val leaderboardData = mapOf(
            "userId" to userId,
            "name" to userName,
            "wins" to wins,
            "totalGames" to totalGames,
            "winRate" to winRate,
            "lastUpdated" to System.currentTimeMillis()
        )

        leaderboardRef.document(userId).set(leaderboardData).await()
    }

    /**
     * En iyi oyuncuları getir (galibiyet sayısına göre)
     */
    suspend fun getTopPlayers(limit: Int = 50): Result<List<LeaderboardEntry>> {
        return try {
            val snapshot = leaderboardRef
                .orderBy("wins", Query.Direction.DESCENDING)
                .limit(limit.toLong())
                .get()
                .await()

            val entries = snapshot.documents.mapIndexed { index, doc ->
                LeaderboardEntry(
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "Bilinmeyen",
                    wins = doc.getLong("wins")?.toInt() ?: 0,
                    totalGames = doc.getLong("totalGames")?.toInt() ?: 0,
                    winRate = doc.getDouble("winRate") ?: 0.0,
                    rank = index + 1
                )
            }

            Result.success(entries)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Belirli bir oyuncunun sıralamasını getir
     */
    suspend fun getPlayerRank(userId: String): Result<LeaderboardEntry?> {
        return try {
            val doc = leaderboardRef.document(userId).get().await()
            if (doc.exists()) {
                // Sırasını bul
                val playerWins = doc.getLong("wins") ?: 0
                val higherCount = leaderboardRef
                    .whereGreaterThan("wins", playerWins)
                    .get()
                    .await()
                    .size()

                val entry = LeaderboardEntry(
                    userId = doc.getString("userId") ?: "",
                    name = doc.getString("name") ?: "Bilinmeyen",
                    wins = doc.getLong("wins")?.toInt() ?: 0,
                    totalGames = doc.getLong("totalGames")?.toInt() ?: 0,
                    winRate = doc.getDouble("winRate") ?: 0.0,
                    rank = higherCount + 1
                )
                Result.success(entry)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Oyuncu istatistiklerini getir
     */
    suspend fun getPlayerStats(userId: String): Result<PlayerStats?> {
        return try {
            val doc = playersRef.document(userId).get().await()
            if (doc.exists()) {
                val stats = PlayerStats(
                    wins = doc.getLong("wins")?.toInt() ?: 0,
                    losses = doc.getLong("losses")?.toInt() ?: 0,
                    totalGames = doc.getLong("totalGames")?.toInt() ?: 0,
                    winRate = doc.getDouble("winRate") ?: 0.0
                )
                Result.success(stats)
            } else {
                Result.success(null)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}

/**
 * Liderboard girişi
 */
data class LeaderboardEntry(
    val userId: String,
    val name: String,
    val wins: Int,
    val totalGames: Int,
    val winRate: Double,
    val rank: Int
)

/**
 * Oyuncu istatistikleri
 */
data class PlayerStats(
    val wins: Int,
    val losses: Int,
    val totalGames: Int,
    val winRate: Double
)
