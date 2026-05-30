package com.haritasismik.bestas.game.models

/**
 * Beş Taş oyunundaki taş stilleri
 */
enum class StoneStyle {
    REALISTIC,  // Gerçekçi taş
    PISTACHIO   // Fıstık
}

/**
 * Oyun modları
 */
enum class GameMode {
    VS_AI,      // Yapay zekaya karşı
    LOCAL,      // Yerel iki kişi
    ONLINE      // Online eşleşme
}

/**
 * Oyun turları (roundlar)
 */
enum class GameRound(val displayName: String, val pickCount: Int) {
    ONES("Birler", 1),
    TWOS("İkiler", 2),
    THREES("Üçler", 3),
    FOURS("Dörtler", 4),
    BRIDGE("Köprü", 1);   // Özel final tur (birer birer toplanır)

    fun next(): GameRound? = when (this) {
        ONES -> TWOS
        TWOS -> THREES
        THREES -> FOURS
        FOURS -> BRIDGE
        BRIDGE -> null  // Oyun bitti
    }
}

/**
 * 2D pozisyon (taşların ve fıstıkların konumu)
 */
data class Position(
    val x: Float,
    val y: Float
)

/**
 * Oyundaki bir taş/fıstık nesnesi
 */
data class Stone(
    val id: Int,
    val position: Position,
    val isInAir: Boolean = false,
    val isPickedUp: Boolean = false,
    val rotation: Float = 0f,
    val isHeneke: Boolean = false  // Bu taş heneke (kapçık) mi?
)

/**
 * Oyuncu bilgisi
 */
data class Player(
    val id: String,
    val name: String,
    val score: Int = 0,
    val isLocal: Boolean = true
)

/**
 * Hamle sonucu
 */
enum class MoveResult {
    SUCCESS,        // Başarılı hamle
    FAIL,           // Taş düşürdü veya yakalayamadı
    ROUND_COMPLETE, // Tur tamamlandı
    GAME_OVER       // Oyun bitti
}

/**
 * Oyun durumu
 */
data class GameState(
    val player1: Player,
    val player2: Player,
    val currentPlayerId: String,
    val currentRound: GameRound = GameRound.ONES,
    val stones: List<Stone> = emptyList(),
    val thrownStone: Stone? = null,
    val henekeId: Int? = null,            // Kapçık taşı (tüm oyun boyunca sabit)
    val isHenekeSelected: Boolean = false, // Heneke seçildi mi?
    val isGameOver: Boolean = false,
    val winnerId: String? = null,
    val gameMode: GameMode = GameMode.LOCAL,
    val stoneStyle: StoneStyle = StoneStyle.REALISTIC,
    val stonesPickedThisTurn: Int = 0,
    val consecutiveSuccesses: Int = 0,
    // Köprü modu alanları
    val bridgePosition: Position? = null,  // Köprü (el) konumu
    val isBridgePlaced: Boolean = false,   // Köprü yerleştirildi mi?
    val ebeStoneId: Int? = null,           // Ebe taşı (karşı oyuncu seçer)
    val isEbeSelected: Boolean = false,    // Ebe seçildi mi?
    val bridgePhase: BridgePhase = BridgePhase.NONE  // Köprü aşaması
) {
    val currentPlayer: Player
        get() = if (currentPlayerId == player1.id) player1 else player2

    val opponentPlayer: Player
        get() = if (currentPlayerId == player1.id) player2 else player1

    val isPlayer1Turn: Boolean
        get() = currentPlayerId == player1.id

    /** Yerdeki toplanabilir taşlar (heneke ve toplananlar hariç) */
    val groundStones: List<Stone>
        get() = stones.filter { !it.isPickedUp && !it.isInAir && it.id != henekeId }

    /** Köprü modunda geçirilecek taşlar (ebe hariç) */
    val bridgePassableStones: List<Stone>
        get() = groundStones.filter { it.id != ebeStoneId }
}

/**
 * Köprü turu aşamaları
 */
enum class BridgePhase {
    NONE,              // Normal tur (köprü değil)
    PLACE_BRIDGE,      // Oyuncu köprüyü yerleştiriyor
    SELECT_EBE,        // Karşı oyuncu ebe seçiyor
    PASS_STONES,       // Taşları köprü altından geçirme
    PASS_EBE,          // Son: Ebeyi geçirme
    COMPLETE           // Tamamlandı
}

/**
 * Online oyun odası
 */
data class GameRoom(
    val roomId: String = "",
    val player1Id: String = "",
    val player2Id: String = "",
    val gameState: GameState? = null,
    val isWaitingForPlayer: Boolean = true,
    val createdAt: Long = System.currentTimeMillis()
)
