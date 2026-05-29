package com.haritasismik.bestas.data.firebase

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/**
 * Firebase Matchmaking - STUB
 * Firebase kurulduktan sonra gerçek implementasyonla değiştirilecek.
 */
class FirebaseMatchmaking {

    suspend fun findOrCreateRoom(userId: String, userName: String): Result<String> {
        return Result.failure(Exception("Firebase henüz kurulmadı. Online mod için Firebase gerekli."))
    }

    fun observeRoom(roomId: String): Flow<RoomState> = flowOf()

    suspend fun sendMove(roomId: String, move: GameMove) {}

    suspend fun switchTurn(roomId: String, nextPlayerId: String, nextRound: String) {}

    suspend fun endGame(roomId: String, winnerId: String) {}

    suspend fun leaveRoom(roomId: String, userId: String) {}
}

/**
 * Oda durumu data class
 */
data class RoomState(
    val roomId: String = "",
    val player1Id: String = "",
    val player1Name: String = "",
    val player2Id: String = "",
    val player2Name: String = "",
    val status: String = "waiting",
    val currentTurn: String = "",
    val currentRound: String = "ONES",
    val winnerId: String? = null
) {
    val isWaiting: Boolean get() = status == "waiting"
    val isPlaying: Boolean get() = status == "playing"
    val isFinished: Boolean get() = status == "finished"
}

/**
 * Oyun hamlesi
 */
data class GameMove(
    val type: MoveType,
    val stoneIds: List<Int>,
    val playerId: String
)

enum class MoveType {
    THROW,
    PICK_UP,
    CATCH,
    FAIL,
    BRIDGE
}
