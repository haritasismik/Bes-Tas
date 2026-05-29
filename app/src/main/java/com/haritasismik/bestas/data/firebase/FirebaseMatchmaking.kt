package com.haritasismik.bestas.data.firebase

import com.google.firebase.database.*
import com.haritasismik.bestas.game.models.*
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.tasks.await

/**
 * Firebase Realtime Database ile online matchmaking sistemi
 */
class FirebaseMatchmaking {

    private val database = FirebaseDatabase.getInstance()
    private val roomsRef = database.getReference("rooms")
    private val waitingRef = database.getReference("waiting_queue")

    companion object {
        const val MAX_ROOM_AGE_MS = 5 * 60 * 1000L  // 5 dakika
    }

    /**
     * Eşleşme kuyruğuna katıl - uygun oda bul veya yeni oda oluştur
     */
    suspend fun findOrCreateRoom(userId: String, userName: String): Result<String> {
        return try {
            // Önce bekleyen bir oda var mı bak
            val waitingRoom = findWaitingRoom(userId)

            if (waitingRoom != null) {
                // Var olan odaya katıl
                joinRoom(waitingRoom, userId, userName)
                Result.success(waitingRoom)
            } else {
                // Yeni oda oluştur
                val roomId = createRoom(userId, userName)
                Result.success(roomId)
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Bekleyen oda bul
     */
    private suspend fun findWaitingRoom(excludeUserId: String): String? {
        val snapshot = waitingRef
            .orderByChild("timestamp")
            .limitToFirst(1)
            .get()
            .await()

        for (child in snapshot.children) {
            val creatorId = child.child("creatorId").getValue(String::class.java)
            val roomId = child.child("roomId").getValue(String::class.java)
            val timestamp = child.child("timestamp").getValue(Long::class.java) ?: 0L

            // Kendi odamız değilse ve süresi dolmamışsa
            if (creatorId != excludeUserId &&
                System.currentTimeMillis() - timestamp < MAX_ROOM_AGE_MS &&
                roomId != null
            ) {
                // Bekleme kuyruğundan kaldır
                child.ref.removeValue().await()
                return roomId
            }
        }
        return null
    }

    /**
     * Yeni oda oluştur
     */
    private suspend fun createRoom(userId: String, userName: String): String {
        val roomId = roomsRef.push().key ?: throw Exception("Oda ID oluşturulamadı")

        val roomData = mapOf(
            "roomId" to roomId,
            "player1Id" to userId,
            "player1Name" to userName,
            "player2Id" to "",
            "player2Name" to "",
            "status" to "waiting",
            "currentTurn" to userId,
            "currentRound" to "ONES",
            "createdAt" to ServerValue.TIMESTAMP
        )

        roomsRef.child(roomId).setValue(roomData).await()

        // Bekleme kuyruğuna ekle
        val waitingData = mapOf(
            "roomId" to roomId,
            "creatorId" to userId,
            "timestamp" to ServerValue.TIMESTAMP
        )
        waitingRef.child(roomId).setValue(waitingData).await()

        return roomId
    }

    /**
     * Mevcut odaya katıl
     */
    private suspend fun joinRoom(roomId: String, userId: String, userName: String) {
        val updates = mapOf(
            "player2Id" to userId,
            "player2Name" to userName,
            "status" to "playing"
        )
        roomsRef.child(roomId).updateChildren(updates).await()
    }

    /**
     * Oda durumunu dinle (real-time)
     */
    fun observeRoom(roomId: String): Flow<RoomState> = callbackFlow {
        val listener = object : ValueEventListener {
            override fun onDataChange(snapshot: DataSnapshot) {
                val state = parseRoomState(snapshot)
                trySend(state)
            }

            override fun onCancelled(error: DatabaseError) {
                close(error.toException())
            }
        }

        roomsRef.child(roomId).addValueEventListener(listener)

        awaitClose {
            roomsRef.child(roomId).removeEventListener(listener)
        }
    }

    /**
     * Hamle gönder
     */
    suspend fun sendMove(roomId: String, move: GameMove) {
        val moveData = mapOf(
            "type" to move.type.name,
            "stoneIds" to move.stoneIds,
            "playerId" to move.playerId,
            "timestamp" to ServerValue.TIMESTAMP
        )
        roomsRef.child(roomId).child("lastMove").setValue(moveData).await()
    }

    /**
     * Sırayı değiştir
     */
    suspend fun switchTurn(roomId: String, nextPlayerId: String, nextRound: String) {
        val updates = mapOf<String, Any>(
            "currentTurn" to nextPlayerId,
            "currentRound" to nextRound
        )
        roomsRef.child(roomId).updateChildren(updates).await()
    }

    /**
     * Oyunu bitir
     */
    suspend fun endGame(roomId: String, winnerId: String) {
        val updates = mapOf<String, Any>(
            "status" to "finished",
            "winnerId" to winnerId
        )
        roomsRef.child(roomId).updateChildren(updates).await()
    }

    /**
     * Odadan ayrıl
     */
    suspend fun leaveRoom(roomId: String, userId: String) {
        val snapshot = roomsRef.child(roomId).get().await()
        val status = snapshot.child("status").getValue(String::class.java)

        if (status == "waiting") {
            // Oda silinir
            roomsRef.child(roomId).removeValue().await()
            waitingRef.child(roomId).removeValue().await()
        } else {
            // Oyuncu ayrıldı - diğer oyuncu kazanır
            val player1Id = snapshot.child("player1Id").getValue(String::class.java)
            val winnerId = if (userId == player1Id) {
                snapshot.child("player2Id").getValue(String::class.java) ?: ""
            } else {
                player1Id ?: ""
            }
            endGame(roomId, winnerId)
        }
    }

    /**
     * Snapshot'tan oda durumunu parse et
     */
    private fun parseRoomState(snapshot: DataSnapshot): RoomState {
        return RoomState(
            roomId = snapshot.child("roomId").getValue(String::class.java) ?: "",
            player1Id = snapshot.child("player1Id").getValue(String::class.java) ?: "",
            player1Name = snapshot.child("player1Name").getValue(String::class.java) ?: "",
            player2Id = snapshot.child("player2Id").getValue(String::class.java) ?: "",
            player2Name = snapshot.child("player2Name").getValue(String::class.java) ?: "",
            status = snapshot.child("status").getValue(String::class.java) ?: "waiting",
            currentTurn = snapshot.child("currentTurn").getValue(String::class.java) ?: "",
            currentRound = snapshot.child("currentRound").getValue(String::class.java) ?: "ONES",
            winnerId = snapshot.child("winnerId").getValue(String::class.java)
        )
    }
}

/**
 * Oda durumu data class
 */
data class RoomState(
    val roomId: String,
    val player1Id: String,
    val player1Name: String,
    val player2Id: String,
    val player2Name: String,
    val status: String,  // waiting, playing, finished
    val currentTurn: String,
    val currentRound: String,
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
