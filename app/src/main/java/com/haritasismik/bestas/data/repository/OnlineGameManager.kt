package com.haritasismik.bestas.data.repository

import com.haritasismik.bestas.data.firebase.*
import com.haritasismik.bestas.game.engine.GameEngine
import com.haritasismik.bestas.game.models.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

/**
 * Online oyun yöneticisi - matchmaking, oda yönetimi ve hamle senkronizasyonu
 */
class OnlineGameManager(
    private val matchmaking: FirebaseMatchmaking = FirebaseMatchmaking(),
    private val authService: FirebaseAuthService = FirebaseAuthService(),
    private val leaderboard: FirebaseLeaderboard = FirebaseLeaderboard(),
    private val engine: GameEngine = GameEngine()
) {

    private val _connectionState = MutableStateFlow<OnlineConnectionState>(OnlineConnectionState.Disconnected)
    val connectionState: StateFlow<OnlineConnectionState> = _connectionState.asStateFlow()

    private val _roomState = MutableStateFlow<RoomState?>(null)
    val roomState: StateFlow<RoomState?> = _roomState.asStateFlow()

    private val _onlineGameState = MutableStateFlow<GameState?>(null)
    val onlineGameState: StateFlow<GameState?> = _onlineGameState.asStateFlow()

    private var currentRoomId: String? = null

    /**
     * Online eşleşme ara
     */
    suspend fun findMatch(scope: CoroutineScope): Result<String> {
        _connectionState.value = OnlineConnectionState.Searching

        val userId = authService.userId ?: run {
            // Otomatik anonim giriş
            val signInResult = authService.signInAnonymously()
            if (signInResult.isFailure) {
                _connectionState.value = OnlineConnectionState.Error("Giriş yapılamadı")
                return Result.failure(signInResult.exceptionOrNull() ?: Exception("Auth failed"))
            }
            authService.userId!!
        }

        val userName = authService.displayName

        val result = matchmaking.findOrCreateRoom(userId, userName)

        return result.fold(
            onSuccess = { roomId ->
                currentRoomId = roomId
                _connectionState.value = OnlineConnectionState.WaitingForOpponent

                // Oda değişikliklerini dinle
                scope.launch {
                    observeRoom(roomId)
                }

                Result.success(roomId)
            },
            onFailure = { error ->
                _connectionState.value = OnlineConnectionState.Error(error.message ?: "Bağlantı hatası")
                Result.failure(error)
            }
        )
    }

    /**
     * Oda durumunu dinle
     */
    private suspend fun observeRoom(roomId: String) {
        matchmaking.observeRoom(roomId).collect { state ->
            _roomState.value = state

            when {
                state.isPlaying && _connectionState.value != OnlineConnectionState.Playing -> {
                    // Rakip bulundu, oyun başlıyor!
                    _connectionState.value = OnlineConnectionState.Playing
                    initializeOnlineGame(state)
                }
                state.isFinished -> {
                    _connectionState.value = OnlineConnectionState.GameOver(state.winnerId)
                    handleGameOver(state)
                }
            }
        }
    }

    /**
     * Online oyunu başlat
     */
    private fun initializeOnlineGame(room: RoomState) {
        val userId = authService.userId ?: return
        val isPlayer1 = userId == room.player1Id

        val gameState = engine.createNewGame(
            player1Name = room.player1Name,
            player2Name = room.player2Name,
            gameMode = GameMode.ONLINE,
            stoneStyle = StoneStyle.REALISTIC
        )

        _onlineGameState.value = gameState
    }

    /**
     * Online hamle gönder
     */
    suspend fun sendMove(moveType: MoveType, stoneIds: List<Int>): Result<Unit> {
        val roomId = currentRoomId ?: return Result.failure(Exception("Oda bulunamadı"))
        val userId = authService.userId ?: return Result.failure(Exception("Kullanıcı bulunamadı"))

        return try {
            val move = GameMove(
                type = moveType,
                stoneIds = stoneIds,
                playerId = userId
            )
            matchmaking.sendMove(roomId, move)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Sırayı değiştir (online)
     */
    suspend fun switchTurn(nextRound: GameRound) {
        val roomId = currentRoomId ?: return
        val room = _roomState.value ?: return
        val userId = authService.userId ?: return

        val nextPlayerId = if (userId == room.player1Id) room.player2Id else room.player1Id
        matchmaking.switchTurn(roomId, nextPlayerId, nextRound.name)
    }

    /**
     * Oyun bitti - sonucu bildir
     */
    suspend fun declareWinner(winnerId: String) {
        val roomId = currentRoomId ?: return
        matchmaking.endGame(roomId, winnerId)
    }

    /**
     * Oyun sonucu işle - istatistikleri güncelle
     */
    private suspend fun handleGameOver(room: RoomState) {
        val userId = authService.userId ?: return
        val userName = authService.displayName
        val won = room.winnerId == userId

        // Leaderboard güncelle
        leaderboard.updatePlayerStats(userId, userName, won)
    }

    /**
     * Sıranın bizde olup olmadığını kontrol et
     */
    fun isMyTurn(): Boolean {
        val userId = authService.userId ?: return false
        val room = _roomState.value ?: return false
        return room.currentTurn == userId
    }

    /**
     * Odadan ayrıl
     */
    suspend fun leaveMatch() {
        val roomId = currentRoomId ?: return
        val userId = authService.userId ?: return

        matchmaking.leaveRoom(roomId, userId)
        currentRoomId = null
        _connectionState.value = OnlineConnectionState.Disconnected
        _roomState.value = null
        _onlineGameState.value = null
    }

    /**
     * Bağlantıyı sıfırla
     */
    fun reset() {
        currentRoomId = null
        _connectionState.value = OnlineConnectionState.Disconnected
        _roomState.value = null
        _onlineGameState.value = null
    }
}

/**
 * Online bağlantı durumları
 */
sealed class OnlineConnectionState {
    object Disconnected : OnlineConnectionState()
    object Searching : OnlineConnectionState()
    object WaitingForOpponent : OnlineConnectionState()
    object Playing : OnlineConnectionState()
    data class GameOver(val winnerId: String?) : OnlineConnectionState()
    data class Error(val message: String) : OnlineConnectionState()
}
