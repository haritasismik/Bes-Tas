package com.haritasismik.bestas.data.firebase

/**
 * Firebase Leaderboard - STUB
 * Firebase kurulduktan sonra gerçek implementasyonla değiştirilecek.
 */
class FirebaseLeaderboard {

    suspend fun updatePlayerStats(userId: String, userName: String, won: Boolean): Result<Unit> {
        return Result.success(Unit)
    }

    suspend fun getTopPlayers(limit: Int = 50): Result<List<LeaderboardEntry>> {
        // Örnek veri döndür
        return Result.success(
            listOf(
                LeaderboardEntry("1", "Ahmet", 42, 55, 0.76, 1),
                LeaderboardEntry("2", "Ayşe", 38, 52, 0.73, 2),
                LeaderboardEntry("3", "Mehmet", 35, 50, 0.70, 3),
                LeaderboardEntry("4", "Fatma", 29, 45, 0.64, 4),
                LeaderboardEntry("5", "Ali", 25, 40, 0.62, 5),
            )
        )
    }

    suspend fun getPlayerRank(userId: String): Result<LeaderboardEntry?> {
        return Result.success(
            LeaderboardEntry("local_user", "Misafir", 0, 0, 0.0, 99)
        )
    }

    suspend fun getPlayerStats(userId: String): Result<PlayerStats?> {
        return Result.success(
            PlayerStats(wins = 0, losses = 0, totalGames = 0, winRate = 0.0)
        )
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
