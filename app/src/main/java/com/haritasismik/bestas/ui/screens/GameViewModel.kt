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

    private val _henekeTimeLeft = MutableStateFlow(0f)  // 0-1 arası (1=tam süre, 0=düştü)
    val henekeTimeLeft: StateFlow<Float> = _henekeTimeLeft.asStateFlow()

    private val _isScattering = MutableStateFlow(false)
    val isScattering: StateFlow<Boolean> = _isScattering.asStateFlow()

    private var selectedStones = mutableListOf<Int>()
    private var gameMode = GameMode.LOCAL
    private var henekeTimerJob: kotlinx.coroutines.Job? = null

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
        _message.value = "Heneke (kapçık) taşını seç! 🪨"
    }

    fun onStoneClicked(stoneId: Int) {
        val state = _gameState.value
        if (state.isGameOver) return

        // AI sırasında tıklama engelle
        if (gameMode == GameMode.VS_AI && !state.isPlayer1Turn) return

        val stone = state.stones.find { it.id == stoneId } ?: return
        if (stone.isPickedUp) return
        if (stone.isInAir) return

        // ADIM 1: Heneke seçimi (oyunun başında)
        if (!state.isHenekeSelected) {
            val newState = engine.selectHeneke(state, stoneId)
            _gameState.value = newState
            _message.value = "Heneke seçildi! ✓ Şimdi 'Fırlat' butonuna bas."
            return
        }

        // ADIM 2: Heneke havada değilse - fırlatma bekleniyor
        if (state.thrownStone == null) {
            _message.value = "Henekeyi fırlatmak için 'Fırlat' butonuna bas!"
            return
        }

        // ADIM 3: Heneke havada - yerdeki taşları seç (heneke hariç)
        if (stoneId == state.henekeId) {
            _message.value = "Heneke zaten havada! Yerdeki taşları seç."
            return
        }

        // Taş seçimi
        if (selectedStones.contains(stoneId)) {
            selectedStones.remove(stoneId)
        } else {
            selectedStones.add(stoneId)
        }

        val groundCount = state.groundStones.size
        val needed = minOf(state.currentRound.pickCount.coerceAtLeast(1), groundCount)
        _message.value = "${selectedStones.size}/$needed taş seçildi"

        // Yeterli taş seçildi → otomatik topla
        if (selectedStones.size == needed) {
            performPickUp()
        }
    }

    fun onBoardClicked(position: Position) {
        val state = _gameState.value
        if (state.thrownStone == null && state.isHenekeSelected) {
            _message.value = "Henekeyi fırlatmak için 'Fırlat' butonuna bas!"
        }
    }

    /**
     * Parmakla çizme sonucu - swipe ile taş toplama
     * Kullanıcı parmağıyla taşların üzerinden geçti
     */
    fun onSwipeStones(stoneIds: List<Int>) {
        val state = _gameState.value
        if (state.isGameOver) return
        if (state.thrownStone == null) return  // Heneke havada değilse swipe yok
        if (gameMode == GameMode.VS_AI && !state.isPlayer1Turn) return

        // Heneke'yi filtrele
        val validIds = stoneIds.filter { it != state.henekeId }
        if (validIds.isEmpty()) return

        val groundCount = state.groundStones.size
        val needed = minOf(state.currentRound.pickCount.coerceAtLeast(1), groundCount)

        if (validIds.size >= needed) {
            // Yeterli taş çizildi → topla
            selectedStones.clear()
            selectedStones.addAll(validIds.take(needed))
            performPickUp()
        } else {
            _message.value = "${validIds.size}/$needed taş çizildi. Daha fazla çiz!"
        }
    }

    /**
     * Taşları serpme - butonla veya sallama ile
     */
    fun scatterStones() {
        viewModelScope.launch {
            _isScattering.value = true
            _message.value = "Taşlar serpiliyor..."

            // Kısa animasyon beklemesi
            delay(600)

            val state = _gameState.value
            val newStones = engine.scatterStones()
            _gameState.value = state.copy(
                stones = newStones,
                henekeId = null,
                isHenekeSelected = false,
                thrownStone = null,
                stonesPickedThisTurn = 0
            )

            _isScattering.value = false
            _message.value = "Heneke (kapçık) taşını seç! 🪨"
        }
    }

    fun onThrowStone() {
        val state = _gameState.value
        if (state.isGameOver) return
        if (state.thrownStone != null) return

        if (!state.isHenekeSelected) {
            _message.value = "Önce heneke taşını seç!"
            return
        }

        // Henekeyi fırlat
        val newState = engine.throwHeneke(state)
        _gameState.value = newState
        selectedStones.clear()

        val groundCount = newState.groundStones.size
        val needed = minOf(newState.currentRound.pickCount.coerceAtLeast(1), groundCount)
        _message.value = "Parmağınla $needed taşın üzerinden geç! ⏱️"

        // Heneke zamanlayıcı başlat
        startHenekeTimer()
    }

    /**
     * Heneke zamanlayıcı - 2.5 saniye havada kalır, sonra düşer
     */
    private fun startHenekeTimer() {
        henekeTimerJob?.cancel()
        _henekeTimeLeft.value = 1f

        henekeTimerJob = viewModelScope.launch {
            val totalTime = GameEngine.HENEKE_FALL_TIME_MS
            val interval = 50L
            var elapsed = 0L

            while (elapsed < totalTime) {
                delay(interval)
                elapsed += interval
                _henekeTimeLeft.value = 1f - (elapsed.toFloat() / totalTime)
            }

            // Süre doldu - heneke düştü!
            _henekeTimeLeft.value = 0f
            val state = _gameState.value
            if (state.thrownStone != null) {
                _message.value = "Heneke düştü! ⏰ Sıra geçti."
                handleFail()
            }
        }
    }

    /**
     * Heneke zamanlayıcıyı durdur (yakalarken)
     */
    private fun stopHenekeTimer() {
        henekeTimerJob?.cancel()
        _henekeTimeLeft.value = 0f
    }

    fun onCatchStone() {
        val state = _gameState.value
        if (state.thrownStone == null) return

        stopHenekeTimer()

        val (newState, result) = engine.catchHeneke(state)

        when (result) {
            MoveResult.SUCCESS -> {
                _gameState.value = newState
                handleAfterCatch(newState)
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
                _message.value = "Güzel! ✓ Şimdi henekeyi yakala!"
            }
            MoveResult.ROUND_COMPLETE -> {
                _gameState.value = newState
                _message.value = "Güzel! ✓ Henekeyi yakala, tur biter!"
            }
            MoveResult.FAIL -> {
                // Yakındaki taşa dokundun!
                _message.value = "Diğer taşa dokundun! 😱 Sıra geçti."
                handleFail()
            }
            else -> {}
        }
    }

    private fun handleAfterCatch(state: GameState) {
        if (state.isGameOver) {
            _message.value = "${state.currentPlayer.name} kazandı! 🏆"
            return
        }

        // Yerde hala taş var mı?
        val remaining = state.groundStones.size
        if (remaining == 0) {
            // Tur zaten tamamlandı (pickUpStones'da handle edildi)
            // Yeni tur başladı
            if (state.currentRound == GameRound.ONES) {
                _message.value = "Birler bitti! → ${state.currentRound.displayName}. Fırlat!"
            } else {
                _message.value = "${state.currentRound.displayName} turu. Fırlat!"
            }
            checkAITurn(state)
        } else {
            // Aynı turda devam - henekeyi tekrar at
            _message.value = "$remaining taş kaldı. Henekeyi tekrar fırlat!"
        }
    }

    private fun handleFail() {
        val newState = engine.failMove(_gameState.value)
        _gameState.value = newState
        _message.value = "Sıra ${newState.currentPlayer.name}'a geçti. Heneke seç!"

        checkAITurn(newState)
    }

    private fun checkAITurn(state: GameState) {
        if (gameMode == GameMode.VS_AI && !state.isPlayer1Turn && !state.isGameOver) {
            performAITurn()
        }
    }

    private fun performAITurn() {
        viewModelScope.launch {
            var state = _gameState.value
            if (state.isGameOver || state.isPlayer1Turn) return@launch

            // AI heneke seçer
            if (!state.isHenekeSelected) {
                _message.value = "Yapay Zeka heneke seçiyor..."
                delay(600)
                val henekeId = aiPlayer.chooseStoneToThrow(state)
                state = engine.selectHeneke(state, henekeId)
                _gameState.value = state
                delay(400)
            }

            // AI oynamaya başlar
            while (!state.isGameOver && !state.isPlayer1Turn) {
                _message.value = "Yapay Zeka oynuyor..."
                aiPlayer.thinkingDelay()

                // Henekeyi fırlat
                state = engine.throwHeneke(state)
                _gameState.value = state
                delay(500)

                // Başarılı mı?
                if (!aiPlayer.isMoveSucessful()) {
                    state = engine.failMove(state)
                    _gameState.value = state
                    _message.value = "Yapay Zeka başaramadı! Senin sıran. Heneke seç!"
                    return@launch
                }

                // Taşları topla
                val stonesToPick = aiPlayer.chooseStonesToPick(state)
                if (stonesToPick.isEmpty()) {
                    state = engine.failMove(state)
                    _gameState.value = state
                    _message.value = "Yapay Zeka başaramadı! Senin sıran. Heneke seç!"
                    return@launch
                }

                val (afterPick, pickResult) = engine.pickUpStones(state, stonesToPick)
                state = afterPick
                _gameState.value = state
                delay(300)

                if (pickResult == MoveResult.FAIL) {
                    state = engine.failMove(state)
                    _gameState.value = state
                    _message.value = "Yapay Zeka taşa dokundu! Senin sıran. Heneke seç!"
                    return@launch
                }

                // Henekeyi yakala
                val (afterCatch, catchResult) = engine.catchHeneke(state)
                state = afterCatch
                _gameState.value = state
                delay(400)

                if (pickResult == MoveResult.ROUND_COMPLETE) {
                    if (state.isGameOver) {
                        _message.value = "Yapay Zeka kazandı! 🏆"
                        return@launch
                    }
                    _message.value = "Yapay Zeka tur geçti! → ${state.currentRound.displayName}"
                    delay(800)
                    // Yeni turda devam (döngü tekrar eder)
                }
                // SUCCESS ise döngü devam eder (aynı turda devam)
            }
        }
    }
}
