package com.haritasismik.bestas.game.engine

import com.haritasismik.bestas.game.models.*
import kotlin.math.abs
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Beş Taş oyun motoru - oyun kurallarını ve mantığını yönetir
 */
class GameEngine {

    companion object {
        const val BOARD_WIDTH = 1000f
        const val BOARD_HEIGHT = 1400f
        const val STONE_SIZE = 60f
        const val CATCH_TOLERANCE = 80f
        val SCATTER_AREA_X = 200f..800f
        val SCATTER_AREA_Y = 600f..1100f
    }

    /**
     * Yeni bir oyun başlatır
     */
    fun createNewGame(
        player1Name: String,
        player2Name: String,
        gameMode: GameMode,
        stoneStyle: StoneStyle
    ): GameState {
        val player1 = Player(
            id = "player1",
            name = player1Name,
            isLocal = true
        )
        val player2 = Player(
            id = "player2",
            name = player2Name,
            isLocal = gameMode != GameMode.ONLINE
        )

        val stones = scatterStones()

        return GameState(
            player1 = player1,
            player2 = player2,
            currentPlayerId = player1.id,
            currentRound = GameRound.ONES,
            stones = stones,
            gameMode = gameMode,
            stoneStyle = stoneStyle
        )
    }

    /**
     * Taşları rastgele yere dağıtır (oyun başlangıcı veya yeni tur)
     */
    fun scatterStones(): List<Stone> {
        return (0 until 5).map { id ->
            Stone(
                id = id,
                position = Position(
                    x = Random.nextFloat() * (SCATTER_AREA_X.endInclusive - SCATTER_AREA_X.start) + SCATTER_AREA_X.start,
                    y = Random.nextFloat() * (SCATTER_AREA_Y.endInclusive - SCATTER_AREA_Y.start) + SCATTER_AREA_Y.start
                ),
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    /**
     * Taşı havaya at (fırlatma hamlesi)
     */
    fun throwStone(state: GameState, stoneId: Int): GameState {
        val stone = state.stones.find { it.id == stoneId } ?: return state
        val updatedStones = state.stones.map {
            if (it.id == stoneId) it.copy(isInAir = true) else it
        }
        return state.copy(
            stones = updatedStones,
            thrownStone = stone.copy(isInAir = true)
        )
    }

    /**
     * Yerdeki taşları topla
     * @param stoneIds toplanacak taşların id'leri
     * @return Güncellenmiş oyun durumu ve hamle sonucu
     */
    fun pickUpStones(state: GameState, stoneIds: List<Int>): Pair<GameState, MoveResult> {
        val round = state.currentRound

        // Doğru sayıda taş mı toplandı?
        if (round != GameRound.BRIDGE && stoneIds.size != round.pickCount) {
            return Pair(state, MoveResult.FAIL)
        }

        // Taşları topla
        val updatedStones = state.stones.map { stone ->
            if (stone.id in stoneIds) stone.copy(isPickedUp = true) else stone
        }

        val newPickedCount = state.stonesPickedThisTurn + stoneIds.size
        val remainingStones = updatedStones.count { !it.isPickedUp && !it.isInAir }

        val newState = state.copy(
            stones = updatedStones,
            stonesPickedThisTurn = newPickedCount
        )

        // Tüm taşlar toplandı mı? (tur tamamlandı)
        return if (remainingStones == 0) {
            Pair(completeRound(newState), MoveResult.ROUND_COMPLETE)
        } else {
            Pair(newState, MoveResult.SUCCESS)
        }
    }

    /**
     * Havadaki taşı yakala
     * @param catchPosition oyuncunun yakalama pozisyonu
     */
    fun catchStone(state: GameState, catchPosition: Position): Pair<GameState, MoveResult> {
        val thrownStone = state.thrownStone ?: return Pair(state, MoveResult.FAIL)

        // Basit yakalama kontrolü - gerçek uygulamada animasyon zamanlamasıyla olacak
        // Burada basitleştirilmiş versiyon
        val newState = state.copy(
            thrownStone = null,
            stones = state.stones.map {
                if (it.id == thrownStone.id) it.copy(isInAir = false, isPickedUp = true) else it
            }
        )

        return Pair(newState, MoveResult.SUCCESS)
    }

    /**
     * Oyuncunun hamlesi başarısız oldu (taş düştü vb.)
     */
    fun failMove(state: GameState): GameState {
        // Sıra diğer oyuncuya geçer
        val nextPlayerId = if (state.isPlayer1Turn) state.player2.id else state.player1.id

        // Taşları yeniden dağıt
        val newStones = scatterStones()

        return state.copy(
            currentPlayerId = nextPlayerId,
            stones = newStones,
            thrownStone = null,
            stonesPickedThisTurn = 0,
            consecutiveSuccesses = 0
        )
    }

    /**
     * Tur tamamlandı - bir sonraki tura geç
     */
    private fun completeRound(state: GameState): GameState {
        val nextRound = state.currentRound.next()

        // Oyun bitti mi?
        if (nextRound == null) {
            val updatedPlayer = if (state.isPlayer1Turn) {
                state.player1.copy(score = state.player1.score + 1)
            } else {
                state.player2.copy(score = state.player2.score + 1)
            }

            return state.copy(
                player1 = if (state.isPlayer1Turn) updatedPlayer else state.player1,
                player2 = if (!state.isPlayer1Turn) updatedPlayer else state.player2,
                isGameOver = true,
                winnerId = state.currentPlayerId
            )
        }

        // Sonraki tura geç
        val newStones = scatterStones()
        return state.copy(
            currentRound = nextRound,
            stones = newStones,
            thrownStone = null,
            stonesPickedThisTurn = 0,
            consecutiveSuccesses = state.consecutiveSuccesses + 1
        )
    }

    /**
     * Köprü turu mantığı - iki taş arasından geçirme
     */
    fun performBridge(state: GameState, bridgeStone1Id: Int, bridgeStone2Id: Int, passStoneId: Int): Pair<GameState, MoveResult> {
        if (state.currentRound != GameRound.BRIDGE) {
            return Pair(state, MoveResult.FAIL)
        }

        val stone1 = state.stones.find { it.id == bridgeStone1Id }
        val stone2 = state.stones.find { it.id == bridgeStone2Id }
        val passStone = state.stones.find { it.id == passStoneId }

        if (stone1 == null || stone2 == null || passStone == null) {
            return Pair(state, MoveResult.FAIL)
        }

        // Köprü başarılı - oyunu kazandı
        val updatedPlayer = if (state.isPlayer1Turn) {
            state.player1.copy(score = state.player1.score + 1)
        } else {
            state.player2.copy(score = state.player2.score + 1)
        }

        val finalState = state.copy(
            player1 = if (state.isPlayer1Turn) updatedPlayer else state.player1,
            player2 = if (!state.isPlayer1Turn) updatedPlayer else state.player2,
            isGameOver = true,
            winnerId = state.currentPlayerId
        )

        return Pair(finalState, MoveResult.GAME_OVER)
    }

    /**
     * İki nokta arasındaki mesafeyi hesapla
     */
    fun distanceBetween(p1: Position, p2: Position): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
}
