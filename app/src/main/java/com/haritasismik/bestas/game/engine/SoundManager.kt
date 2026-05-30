package com.haritasismik.bestas.game.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool

/**
 * Ses efektleri yöneticisi
 * SoundPool ile kısa efektler, MediaPlayer ile arka plan müziği
 */
class SoundManager(private val context: Context) {

    private var soundPool: SoundPool? = null
    private var mediaPlayer: MediaPlayer? = null

    // Ses ID'leri
    private var stoneThrowId: Int = 0
    private var stoneCatchId: Int = 0
    private var stoneDropId: Int = 0
    private var stonePickUpId: Int = 0
    private var stoneCollideId: Int = 0
    private var roundCompleteId: Int = 0
    private var gameWinId: Int = 0
    private var gameLoseId: Int = 0
    private var buttonClickId: Int = 0
    private var matchFoundId: Int = 0

    // Ayarlar
    private var soundEnabled: Boolean = true
    private var musicEnabled: Boolean = true
    private var soundVolume: Float = 0.8f
    private var musicVolume: Float = 0.4f

    /**
     * Ses sistemini başlat ve ses dosyalarını yükle
     */
    fun initialize() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()

        soundPool = SoundPool.Builder()
            .setMaxStreams(8)
            .setAudioAttributes(audioAttributes)
            .build()

        // Ses dosyaları eklenince burası aktif edilecek
        // loadSounds()
    }

    /** Taş/fıstık çarpma sesi (serpme sırasında) */
    fun playCollide() { playSound(stoneCollideId, volume = soundVolume * 0.6f) }

    /** Kenara çarpma sesi */
    fun playWallBounce() { playSound(stoneDropId, volume = soundVolume * 0.4f) }

    fun playThrow() { playSound(stoneThrowId) }
    fun playCatch() { playSound(stoneCatchId) }
    fun playDrop() { playSound(stoneDropId) }
    fun playPickUp() { playSound(stonePickUpId, volume = soundVolume * 0.7f) }
    fun playRoundComplete() { playSound(roundCompleteId, volume = soundVolume * 1.2f) }
    fun playWin() { playSound(gameWinId, volume = 1f) }
    fun playLose() { playSound(gameLoseId, volume = 0.8f) }
    fun playButtonClick() { playSound(buttonClickId, volume = soundVolume * 0.5f) }
    fun playMatchFound() { playSound(matchFoundId, volume = 1f) }

    fun startBackgroundMusic() {
        // Gerçek müzik dosyası eklenince aktif edilecek
    }

    fun stopBackgroundMusic() {
        mediaPlayer?.apply {
            if (isPlaying) stop()
            release()
        }
        mediaPlayer = null
    }

    fun pauseBackgroundMusic() {
        mediaPlayer?.let { if (it.isPlaying) it.pause() }
    }

    fun resumeBackgroundMusic() {
        if (!musicEnabled) return
        mediaPlayer?.start()
    }

    fun setSoundEnabled(enabled: Boolean) { soundEnabled = enabled }
    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) pauseBackgroundMusic() else resumeBackgroundMusic()
    }
    fun setSoundVolume(volume: Float) { soundVolume = volume.coerceIn(0f, 1f) }
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }

    private fun playSound(soundId: Int, volume: Float = soundVolume) {
        if (!soundEnabled || soundId == 0) return
        val clampedVolume = volume.coerceIn(0f, 1f)
        soundPool?.play(soundId, clampedVolume, clampedVolume, 1, 0, 1f)
    }

    fun release() {
        soundPool?.release()
        soundPool = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
