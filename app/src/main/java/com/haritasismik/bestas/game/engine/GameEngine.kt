package com.haritasismik.bestas.game.engine

import com.haritasismik.bestas.game.models.*
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Beş Taş oyun motoru - gerçek kurallarla
 *
 * KURALLAR:
 * 1. Oyuncu 5 taşı yere serpiştirir
 * 2. Bir taşı "heneke" (kapçık) olarak seçer - tüm oyun boyunca aynı kalır
 * 3. Birler: Henekeyi at, 1 taş topla, henekeyi yakala. 4 kez tekrarla.
 * 4. İkiler: Henekeyi at, 2 taş birden topla, yakala. 2 kez tekrarla.
 * 5. Üçler: Henekeyi at, 3 taş topla, yakala. Sonra kalan 1'i de topla.
 * 6. Dörtler: Henekeyi at, 4 taşı birden topla, yakala.
 * 7. Köprü: İki taşı yere koy, aralarından üçüncüyü geçir.
 * 8. Taş toplarken başka taşa dokunursan → sıra karşıya geçer.
 */
class GameEngine {

    companion object {
        const val BOARD_WIDTH = 1000f
        const val BOARD_HEIGHT = 1400f
        const val STONE_SIZE = 60f
        const val TOUCH_RADIUS = 70f       // Taşa dokunma yarıçapı
        const val PROXIMITY_LIMIT = 90f    // Yakınlık limiti - bundan yakınsa dokunma riski var
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
        val player1 = Player(id = "player1", name = player1Name, isLocal = true)
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
            stoneStyle = stoneStyle,
            isHenekeSelected = false,
            henekeId = null
        )
    }

    /**
     * Taşları rastgele yere serpiştirir - birbirinden minimum mesafede
     */
    fun scatterStones(): List<Stone> {
        val positions = mutableListOf<Position>()
        val minDistance = STONE_SIZE * 1.2f  // Minimum mesafe (çakışmasın)

        for (i in 0 until 5) {
            var attempts = 0
            var pos: Position
            do {
                pos = Position(
                    x = Random.nextFloat() * (SCATTER_AREA_X.endInclusive - SCATTER_AREA_X.start) + SCATTER_AREA_X.start,
                    y = Random.nextFloat() * (SCATTER_AREA_Y.endInclusive - SCATTER_AREA_Y.start) + SCATTER_AREA_Y.start
                )
                attempts++
            } while (attempts < 50 && positions.any { distanceBetween(it, pos) < minDistance })

            positions.add(pos)
        }

        return positions.mapIndexed { index, position ->
            Stone(
                id = index,
                position = position,
                rotation = Random.nextFloat() * 360f
            )
        }
    }

    /**
     * Heneke seç - oyuncu ilk seçtiği taş kapçık olur
     */
    fun selectHeneke(state: GameState, stoneId: Int): GameState {
        return state.copy(
            henekeId = stoneId,
            isHenekeSelected = true,
            stones = state.stones.map {
                if (it.id == stoneId) it.copy(isHeneke = true) else it
            }
        )
    }

    /**
     * Henekeyi havaya at
     */
    fun throwHeneke(state: GameState): GameState {
        val henekeId = state.henekeId ?: return state
        val updatedStones = state.stones.map {
            if (it.id == henekeId) it.copy(isInAir = true) else it
        }
        val heneke = updatedStones.find { it.id == henekeId }
        return state.copy(
            stones = updatedStones,
            thrownStone = heneke
        )
    }

    /**
     * Yerdeki taşları topla - yakınlık kontrolü ile
     * Eğer toplanan taş diğerine çok yakınsa → dokunma riski (başarısızlık)
     *
     * @return Pair(yeniState, sonuç)
     */
    fun pickUpStones(state: GameState, stoneIds: List<Int>): Pair<GameState, MoveResult> {
        val round = state.currentRound

        // Toplanabilir taşlar (heneke hariç)
        val groundStones = state.groundStones
        if (groundStones.isEmpty()) return Pair(state, MoveResult.FAIL)

        // Bu turda kaç taş toplanmalı?
        val required = minOf(round.pickCount.coerceAtLeast(1), groundStones.size)

        if (stoneIds.size != required) {
            return Pair(state, MoveResult.FAIL)
        }

        // Yakınlık kontrolü: toplanan taşlar diğer yerdeki taşlara dokunuyor mu?
        val remainingAfterPick = groundStones.filter { it.id !in stoneIds }
        for (pickedId in stoneIds) {
            val pickedStone = state.stones.find { it.id == pickedId } ?: continue
            for (other in remainingAfterPick) {
                val dist = distanceBetween(pickedStone.position, other.position)
                if (dist < PROXIMITY_LIMIT) {
                    // Çok yakın! Dokunma riski - %60 ihtimalle fail
                    if (Random.nextFloat() < 0.6f) {
                        return Pair(state, MoveResult.FAIL)
                    }
                }
            }
        }

        // Taşları topla
        val updatedStones = state.stones.map { stone ->
            if (stone.id in stoneIds) stone.copy(isPickedUp = true) else stone
        }

        val newPickedCount = state.stonesPickedThisTurn + stoneIds.size

        val newState = state.copy(
            stones = updatedStones,
            stonesPickedThisTurn = newPickedCount
        )

        // Yerde kalan taş var mı? (heneke hariç)
        val remainingGround = newState.groundStones
        return if (remainingGround.isEmpty()) {
            // Tüm taşlar toplandı → tur tamamlandı
            Pair(completeRound(newState), MoveResult.ROUND_COMPLETE)
        } else {
            Pair(newState, MoveResult.SUCCESS)
        }
    }

    /**
     * Henekeyi yakala - yere düşmeden önce
     */
    fun catchHeneke(state: GameState): Pair<GameState, MoveResult> {
        val thrownStone = state.thrownStone ?: return Pair(state, MoveResult.FAIL)

        // Heneke yakalandı - tekrar yerde, havada değil
        val newState = state.copy(
            thrownStone = null,
            stones = state.stones.map {
                if (it.id == thrownStone.id) it.copy(isInAir = false) else it
            }
        )

        return Pair(newState, MoveResult.SUCCESS)
    }

    /**
     * Eski catchStone fonksiyonu (uyumluluk için)
     */
    fun catchStone(state: GameState, catchPosition: Position): Pair<GameState, MoveResult> {
        return catchHeneke(state)
    }

    /**
     * Eski throwStone fonksiyonu (uyumluluk için)
     */
    fun throwStone(state: GameState, stoneId: Int): GameState {
        return throwHeneke(state)
    }

    /**
     * Oyuncunun hamlesi başarısız oldu (dokunma, düşürme vb.)
     * Sıra diğer oyuncuya geçer
     */
    fun failMove(state: GameState): GameState {
        val nextPlayerId = if (state.isPlayer1Turn) state.player2.id else state.player1.id
        val newStones = scatterStones()

        return state.copy(
            currentPlayerId = nextPlayerId,
            stones = newStones,
            thrownStone = null,
            stonesPickedThisTurn = 0,
            consecutiveSuccesses = 0,
            // Diğer oyuncu kendi henekesini seçecek
            henekeId = null,
            isHenekeSelected = false
        )
    }

    /**
     * Tur tamamlandı - bir sonraki tura geç
     */
    private fun completeRound(state: GameState): GameState {
        val nextRound = state.currentRound.next()

        if (nextRound == null) {
            // Tüm turlar bitti - oyuncu kazandı!
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

        // Sonraki tura geç - taşlar yeniden dağıtılır, heneke aynı kalır
        val newStones = scatterStones().mapIndexed { index, stone ->
            if (index == state.henekeId) stone.copy(isHeneke = true) else stone
        }

        return state.copy(
            currentRound = nextRound,
            stones = newStones,
            thrownStone = null,
            stonesPickedThisTurn = 0,
            consecutiveSuccesses = state.consecutiveSuccesses + 1
        )
    }

    /**
     * Köprü turu - iki taş arasından geçirme
     */
    fun performBridge(state: GameState, bridgeStone1Id: Int, bridgeStone2Id: Int, passStoneId: Int): Pair<GameState, MoveResult> {
        if (state.currentRound != GameRound.BRIDGE) {
            return Pair(state, MoveResult.FAIL)
        }

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
     * İki taşın birbirine yakın olup olmadığını kontrol et
     */
    fun areStonesClose(stone1: Stone, stone2: Stone): Boolean {
        return distanceBetween(stone1.position, stone2.position) < PROXIMITY_LIMIT
    }

    /**
     * İki nokta arasındaki mesafe
     */
    fun distanceBetween(p1: Position, p2: Position): Float {
        val dx = p1.x - p2.x
        val dy = p1.y - p2.y
        return sqrt(dx * dx + dy * dy)
    }
}
