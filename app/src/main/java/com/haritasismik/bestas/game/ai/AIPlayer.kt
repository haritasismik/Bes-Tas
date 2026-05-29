package com.haritasismik.bestas.game.ai

import com.haritasismik.bestas.game.models.*
import com.haritasismik.bestas.game.engine.GameEngine
import kotlinx.coroutines.delay
import kotlin.random.Random

/**
 * Yapay zeka oyuncu - farklı zorluk seviyeleri
 */
class AIPlayer(
    private val difficulty: AIDifficulty = AIDifficulty.MEDIUM,
    private val engine: GameEngine = GameEngine()
) {

    enum class AIDifficulty(val successRate: Float, val thinkingTimeMs: Long) {
        EASY(0.5f, 800L),      // %50 başarı oranı
        MEDIUM(0.7f, 1200L),   // %70 başarı oranı
        HARD(0.9f, 1500L)      // %90 başarı oranı
    }

    /**
     * AI'ın düşünme süresi (gerçekçi his için)
     */
    suspend fun thinkingDelay() {
        val variation = Random.nextLong(-200, 200)
        delay(difficulty.thinkingTimeMs + variation)
    }

    /**
     * AI havaya atacağı taşı seçer
     * Strateji: En dıştaki taşı seçer (toplamayı kolaylaştırmak için)
     */
    fun chooseStoneToThrow(state: GameState): Int {
        val availableStones = state.stones.filter { !it.isPickedUp && !it.isInAir }
        if (availableStones.isEmpty()) return 0

        // En kenar taşı seç (diğerlerinden en uzak olan)
        return when (difficulty) {
            AIDifficulty.EASY -> availableStones.random().id
            AIDifficulty.MEDIUM, AIDifficulty.HARD -> {
                findOutermostStone(availableStones)
            }
        }
    }

    /**
     * AI toplayacağı taşları seçer
     */
    fun chooseStonesToPick(state: GameState): List<Int> {
        val availableStones = state.stones.filter { !it.isPickedUp && !it.isInAir }
        val pickCount = state.currentRound.pickCount

        if (availableStones.size < pickCount) return emptyList()

        return when (difficulty) {
            AIDifficulty.EASY -> {
                // Rastgele seç
                availableStones.shuffled().take(pickCount).map { it.id }
            }
            AIDifficulty.MEDIUM, AIDifficulty.HARD -> {
                // En yakın olanları seç (birbirine en yakın taşları grupla)
                findClosestGroup(availableStones, pickCount)
            }
        }
    }

    /**
     * AI'ın hamlesi başarılı mı? (zorluk seviyesine göre)
     */
    fun isMoveSucessful(): Boolean {
        return Random.nextFloat() < difficulty.successRate
    }

    /**
     * Köprü turu için taş seçimi
     */
    fun chooseBridgeStones(state: GameState): Triple<Int, Int, Int>? {
        val availableStones = state.stones.filter { !it.isPickedUp && !it.isInAir }
        if (availableStones.size < 3) return null

        // En yakın iki taşı köprü olarak seç, üçüncüyü geçir
        val sorted = availableStones.sortedBy { it.position.x }
        return Triple(sorted[0].id, sorted[1].id, sorted[2].id)
    }

    /**
     * En dıştaki taşı bul
     */
    private fun findOutermostStone(stones: List<Stone>): Int {
        val centerX = stones.map { it.position.x }.average().toFloat()
        val centerY = stones.map { it.position.y }.average().toFloat()
        val center = Position(centerX, centerY)

        return stones.maxByOrNull { engine.distanceBetween(it.position, center) }?.id ?: stones.first().id
    }

    /**
     * Birbirine en yakın taş grubunu bul
     */
    private fun findClosestGroup(stones: List<Stone>, count: Int): List<Int> {
        if (stones.size <= count) return stones.map { it.id }

        var bestGroup = stones.take(count)
        var bestDistance = Float.MAX_VALUE

        // Brute force (5 taş için yeterli performans)
        val combinations = getCombinations(stones, count)
        for (combo in combinations) {
            val totalDistance = calculateGroupSpread(combo)
            if (totalDistance < bestDistance) {
                bestDistance = totalDistance
                bestGroup = combo
            }
        }

        return bestGroup.map { it.id }
    }

    /**
     * Grup yayılımını hesapla (ne kadar düşükse taşlar o kadar yakın)
     */
    private fun calculateGroupSpread(stones: List<Stone>): Float {
        var total = 0f
        for (i in stones.indices) {
            for (j in i + 1 until stones.size) {
                total += engine.distanceBetween(stones[i].position, stones[j].position)
            }
        }
        return total
    }

    /**
     * Basit kombinasyon üreteci
     */
    private fun getCombinations(stones: List<Stone>, count: Int): List<List<Stone>> {
        if (count == 1) return stones.map { listOf(it) }
        if (count == stones.size) return listOf(stones)

        val result = mutableListOf<List<Stone>>()
        for (i in 0..stones.size - count) {
            val subCombinations = getCombinations(stones.subList(i + 1, stones.size), count - 1)
            for (sub in subCombinations) {
                result.add(listOf(stones[i]) + sub)
            }
        }
        return result
    }
}
