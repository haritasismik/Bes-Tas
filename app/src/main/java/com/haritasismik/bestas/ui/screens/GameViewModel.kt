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

            val needed = state.currentRound.pickCount
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

        val needed = state.currentRound.pickCount
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
            _message.value = "Yapay Zeka düşünüyor..."

            aiPlayer.thinkingDelay()

            val state = _gameState.value
            if (state.isGameOver) return@launch

            // AI taş seçer ve fırlatır
            val stoneToThrow = aiPlayer.chooseStoneToThrow(state)
            val stateAfterThrow = engine.throwStone(state, stoneToThrow)
            _gameState.value = stateAfterThrow

            delay(500)

            // AI taşları toplar
            val stonesToPick = aiPlayer.chooseStonesToPick(stateAfterThrow)
            if (stonesToPick.isNotEmpty() && aiPlayer.isMoveSucessful()) {
                val (stateAfterPick, pickResult) = engine.pickUpStones(stateAfterThrow, stonesToPick)

                delay(300)

                // AI havadaki taşı yakalar
                if (aiPlayer.isMoveSucessful()) {
                    val (finalState, _) = engine.catchStone(stateAfterPick, Position(500f, 500f))
                    _gameState.value = finalState

                    if (finalState.isGameOver) {
                        _message.value = "Yapay Zeka kazandı!"
                    } else if (pickResult == MoveResult.ROUND_COMPLETE) {
                        _message.value = "AI tur tamamladı! Sonraki: ${finalState.currentRound.displayName}"
                        // AI devam eder
                        delay(800)
                        performAITurn()
                    } else {
                        // AI devam eder
                        delay(500)
                        performAITurn()
                    }
                } else {
                    // AI yakalayamadı
                    val failState = engine.failMove(stateAfterPick)
                    _gameState.value = failState
                    _message.value = "AI kaçırdı! Senin sıran!"
                }
            } else {
                // AI toplayamadı
                val failState = engine.failMove(stateAfterThrow)
                _gameState.value = failState
                _message.value = "AI başaramadı! Senin sıran!"
            }
        }
    }
}
