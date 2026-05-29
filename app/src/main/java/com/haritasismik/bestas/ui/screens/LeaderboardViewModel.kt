package com.haritasismik.bestas.ui.screens

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.haritasismik.bestas.data.firebase.FirebaseAuthService
import com.haritasismik.bestas.data.firebase.FirebaseLeaderboard
import com.haritasismik.bestas.data.firebase.LeaderboardEntry
import com.haritasismik.bestas.data.firebase.PlayerStats
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class LeaderboardViewModel : ViewModel() {

    private val leaderboard = FirebaseLeaderboard()
    private val authService = FirebaseAuthService()

    private val _entries = MutableStateFlow<List<LeaderboardEntry>>(emptyList())
    val entries: StateFlow<List<LeaderboardEntry>> = _entries.asStateFlow()

    private val _myStats = MutableStateFlow<PlayerStats?>(null)
    val myStats: StateFlow<PlayerStats?> = _myStats.asStateFlow()

    private val _myRank = MutableStateFlow<LeaderboardEntry?>(null)
    val myRank: StateFlow<LeaderboardEntry?> = _myRank.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    init {
        loadLeaderboard()
    }

    fun loadLeaderboard() {
        viewModelScope.launch {
            _isLoading.value = true
            _errorMessage.value = null

            // Top 50 oyuncuyu yükle
            val result = leaderboard.getTopPlayers(50)
            result.fold(
                onSuccess = { list ->
                    _entries.value = list
                },
                onFailure = { error ->
                    _errorMessage.value = error.message ?: "Sıralama yüklenemedi"
                }
            )

            // Kendi sıralamamızı yükle
            val userId = authService.userId
            if (userId != null) {
                val rankResult = leaderboard.getPlayerRank(userId)
                rankResult.fold(
                    onSuccess = { rank -> _myRank.value = rank },
                    onFailure = { }
                )

                val statsResult = leaderboard.getPlayerStats(userId)
                statsResult.fold(
                    onSuccess = { stats -> _myStats.value = stats },
                    onFailure = { }
                )
            }

            _isLoading.value = false
        }
    }

    fun refresh() {
        loadLeaderboard()
    }
}
