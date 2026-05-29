package com.haritasismik.bestas.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haritasismik.bestas.game.ai.AIPlayer
import com.haritasismik.bestas.game.engine.GameEngine
import com.haritasismik.bestas.game.models.*
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class GameViewModel : ViewModel() {

    private val engine = GameEngine()
    private val aiPlayer = AIPlayer(AIPlayer.AIDifficulty.MEDIUM)

    private val _gameState = MutableStateFlow(
        GameState(
            player1 = Player(id = "player1", name = "Oyuncu 1"),
            player2 = Player(id = "player2", name = "Oyuncu 2"),
            currentPlayerId = "player1"
        )
    )
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()

    private val _message = MutableStateFlow("")
    val message: StateFlow<String> = _message.asStateFlow()

    private var selectedStones = mutableListOf<Int>()
    private var gameMode = GameMode.LOCAL

    fun startGame(mode: GameMode, stoneStyle: StoneStyle) {
        gameMode = mode
        selectedStones.clear()

        val player2Name = when (mode) {
            GameMode.VS_AI -> "Yapay Zeka"
            GameMode.LOCAL -> "Oyuncu 2"
            GameMode.ONLINE -> "Rakip"
        }

        val newState = engine.createNewGame(
            player1Name = "Oyuncu 1",
            player2Name = player2Name,
            gameMode = mode,
            stoneStyle = stoneStyle
        )

        _gameState.value = newState
        _message.value = "Bir taşa dokun ve fırlat!"
    }

    fun onStoneClicked(stoneId: Int) {
        val state = _gameState.value
        if (state.isGameOver) return

        // AI sırasında tıklama engelle
        if (gameMode == GameMode.VS_AI && !state.isPlayer1Turn) return

        val stone = state.stones.find { it.id == stoneId } ?: return
        if (stone.isPickedUp) return
        // Havadaki kapçık tekrar seçilemez
        if (stone.isInAir) return

        if (state.thrownStone == null) {
            // Henüz taş atılmadı - bu taşı seç fırlatmak için
            selectedStones.clear()
            selectedStones.add(stoneId)
            _message.value = "Taşı fırlatmak için 'Fırlat' butonuna bas!"
        } else {
            // Taş havada - yerdeki taşları topla
            if (selectedStones.contains(stoneId)) {
                selectedStones.remove(stoneId)
            } else {
                selectedStones.add(stoneId)
            }

            // Yerde toplanabilir taş sayısı (kapçık hariç)
            val groundCount = state.stones.count { !it.isPickedUp && !it.isInAir }
            val needed = minOf(state.currentRound.pickCount.coerceAtLeast(1), groundCount)
            _message.value = "${selectedStones.size}/$needed taş seçildi"

            if (selectedStones.size == needed) {
                performPickUp()
            }
        }
    }

    fun onBoardClicked(position: Position) {
        // Boş alana tıklama - seçimi temizle
        if (_gameState.value.thrownStone == null) {
            selectedStones.clear()
            _message.value = "Bir taşa dokun ve fırlat!"
        }
    }

    fun onThrowStone() {
        val state = _gameState.value
        if (state.isGameOver || state.thrownStone != null) return
        if (selectedStones.isEmpty()) {
            _message.value = "Önce fırlatılacak taşı seç!"
            return
        }

        val stoneToThrow = selectedStones.first()
        val newState = engine.throwStone(state, stoneToThrow)
        _gameState.value = newState
        selectedStones.clear()

        val groundCount = newState.stones.count { !it.isPickedUp && !it.isInAir }
        val needed = minOf(newState.currentRound.pickCount.coerceAtLeast(1), groundCount)
        _message.value = "Şimdi $needed taş seç ve topla!"
    }

    fun onCatchStone() {
        val state = _gameState.value
        if (state.thrownStone == null) return

        val (newState, result) = engine.catchStone(state, Position(500f, 500f))

        when (result) {
            MoveResult.SUCCESS -> {
                _gameState.value = newState
                handleTurnEnd(newState)
            }
            MoveResult.FAIL -> {
                handleFail()
            }
            else -> {
                _gameState.value = newState
            }
        }
    }

    private fun performPickUp() {
        val state = _gameState.value
        val (newState, result) = engine.pickUpStones(state, selectedStones.toList())
        selectedStones.clear()

        when (result) {
            MoveResult.SUCCESS -> {
                _gameState.value = newState
                _message.value = "Güzel! Şimdi havadaki taşı yakala!"
            }
            MoveResult.ROUND_COMPLETE -> {
                _gameState.value = newState
                if (newState.isGameOver) {
                    _message.value = "${newState.currentPlayer.name} kazandı! 🏆"
                } else {
                    _message.value = "Tur tamamlandı! Sonraki: ${newState.currentRound.displayName}"
                    checkAITurn(newState)
                }
            }
            MoveResult.FAIL -> {
                handleFail()
            }
            else -> {}
        }
    }

    private fun handleTurnEnd(state: GameState) {
        if (state.isGameOver) {
            _message.value = "${state.currentPlayer.name} kazandı! 🏆"
            return
        }

        // Sıra hala aynı oyuncuda, yeni taş atması lazım
        _message.value = "Bir taşa dokun ve fırlat!"
        checkAITurn(state)
    }

    private fun handleFail() {
        val newState = engine.failMove(_gameState.value)
        _gameState.value = newState
        _message.value = "Kaçırdın! Sıra ${newState.currentPlayer.name}'a geçti."

        checkAITurn(newState)
    }

    private fun checkAITurn(state: GameState) {
        if (gameMode == GameMode.VS_AI && !state.isPlayer1Turn && !state.isGameOver) {
            performAITurn()
        }
    }

    private fun performAITurn() {
        viewModelScope.launch {
            // AI başarısız olana veya oyunu bitirene kadar oynar
            while (true) {
                val state = _gameState.value
                if (state.isGameOver || state.isPlayer1Turn) return@launch

                _message.value = "Yapay Zeka düşünüyor..."
                aiPlayer.thinkingDelay()

                // 1) Taş fırlat
                val stoneToThrow = aiPlayer.chooseStoneToThrow(state)
                val afterThrow = engine.throwStone(state, stoneToThrow)
                _gameState.value = afterThrow
                delay(500)

                // Bu hamle başarılı mı? (zorluk seviyesine göre)
                if (!aiPlayer.isMoveSucessful()) {
                    val failState = engine.failMove(afterThrow)
                    _gameState.value = failState
                    _message.value = "Yapay Zeka şaşırdı! Senin sıran. 🎯"
                    return@launch
                }

                // 2) Yerden taş topla
                val stonesToPick = aiPlayer.chooseStonesToPick(afterThrow)
                if (stonesToPick.isEmpty()) {
                    val failState = engine.failMove(afterThrow)
                    _gameState.value = failState
                    _message.value = "Yapay Zeka şaşırdı! Senin sıran. 🎯"
                    return@launch
                }

                val (afterPick, pickResult) = engine.pickUpStones(afterThrow, stonesToPick)
                _gameState.value = afterPick
                delay(300)

                when (pickResult) {
                    MoveResult.ROUND_COMPLETE -> {
                        if (afterPick.isGameOver) {
                            _message.value = "Yapay Zeka kazandı! 🏆"
                            return@launch
                        }
                        _message.value = "Yapay Zeka turu geçti! Sıradaki: ${afterPick.currentRound.displayName}"
                        delay(800)
                        // Döngü devam eder - AI sonraki turda oynamaya devam eder
                    }
                    MoveResult.SUCCESS -> {
                        // 3) Havadaki kapçığı yakala, sonra tekrar fırlatmak için döngü devam eder
                        val (afterCatch, _) = engine.catchStone(afterPick, Position(500f, 500f))
                        _gameState.value = afterCatch
                        delay(400)
                    }
                    else -> {
                        val failState = engine.failMove(afterPick)
                        _gameState.value = failState
                        _message.value = "Yapay Zeka şaşırdı! Senin sıran. 🎯"
                        return@launch
                    }
                }
            }
        }
    }
}
