package com.haritasismik.bestas.game.engine

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import com.haritasismik.bestas.R

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
            .setMaxStreams(6)
            .setAudioAttributes(audioAttributes)
            .build()

        loadSounds()
    }

    /**
     * Ses dosyalarını yükle
     * NOT: Bu ses dosyaları res/raw klasörüne eklenmelidir.
     * Placeholder olarak tanımlanmıştır - gerçek .ogg/.mp3 dosyaları sonra eklenecek.
     */
    private fun loadSounds() {
        soundPool?.let { pool ->
            try {
                stoneThrowId = pool.load(context, R.raw.stone_throw, 1)
                stoneCatchId = pool.load(context, R.raw.stone_catch, 1)
                stoneDropId = pool.load(context, R.raw.stone_drop, 1)
                stonePickUpId = pool.load(context, R.raw.stone_pickup, 1)
                stoneCollideId = pool.load(context, R.raw.stone_collide, 1)
                roundCompleteId = pool.load(context, R.raw.round_complete, 1)
                gameWinId = pool.load(context, R.raw.game_win, 1)
                gameLoseId = pool.load(context, R.raw.game_lose, 1)
                buttonClickId = pool.load(context, R.raw.button_click, 1)
                matchFoundId = pool.load(context, R.raw.match_found, 1)
            } catch (e: Exception) {
                // Ses dosyaları henüz eklenmemişse sessiz devam et
                e.printStackTrace()
            }
        }
    }

    // --- Ses Efekti Çalma Fonksiyonları ---

    /**
     * Taş fırlatma sesi
     */
    fun playThrow() {
        playSound(stoneThrowId)
    }

    /**
     * Taş yakalama sesi
     */
    fun playCatch() {
        playSound(stoneCatchId)
    }

    /**
     * Taş düşürme sesi (başarısız hamle)
     */
    fun playDrop() {
        playSound(stoneDropId)
    }

    /**
     * Taş toplama sesi
     */
    fun playPickUp() {
        playSound(stonePickUpId, volume = soundVolume * 0.7f)
    }

    /**
     * Taşların birbirine çarpma sesi
     */
    fun playCollide() {
        playSound(stoneCollideId, volume = soundVolume * 0.5f)
    }

    /**
     * Tur tamamlama sesi
     */
    fun playRoundComplete() {
        playSound(roundCompleteId, volume = soundVolume * 1.2f)
    }

    /**
     * Oyun kazanma sesi
     */
    fun playWin() {
        playSound(gameWinId, volume = 1f)
    }

    /**
     * Oyun kaybetme sesi
     */
    fun playLose() {
        playSound(gameLoseId, volume = 0.8f)
    }

    /**
     * Buton tıklama sesi
     */
    fun playButtonClick() {
        playSound(buttonClickId, volume = soundVolume * 0.5f)
    }

    /**
     * Online eşleşme bulundu sesi
     */
    fun playMatchFound() {
        playSound(matchFoundId, volume = 1f)
    }

    // --- Arka Plan Müziği ---

    /**
     * Arka plan müziğini başlat
     */
    fun startBackgroundMusic() {
        if (!musicEnabled) return

        try {
            mediaPlayer = MediaPlayer.create(context, R.raw.background_music)?.apply {
                isLooping = true
                setVolume(musicVolume, musicVolume)
                start()
            }
        } catch (e: Exception) {
            // Müzik dosyası yoksa sessiz devam et
            e.printStackTrace()
        }
    }

    /**
     * Arka plan müziğini durdur
     */
    fun stopBackgroundMusic() {
        mediaPlayer?.apply {
            if (isPlaying) {
                stop()
            }
            release()
        }
        mediaPlayer = null
    }

    /**
     * Arka plan müziğini duraklat
     */
    fun pauseBackgroundMusic() {
        mediaPlayer?.let {
            if (it.isPlaying) {
                it.pause()
            }
        }
    }

    /**
     * Arka plan müziğini devam ettir
     */
    fun resumeBackgroundMusic() {
        if (!musicEnabled) return
        mediaPlayer?.start()
    }

    // --- Ayarlar ---

    /**
     * Ses efektlerini aç/kapa
     */
    fun setSoundEnabled(enabled: Boolean) {
        soundEnabled = enabled
    }

    /**
     * Müziği aç/kapa
     */
    fun setMusicEnabled(enabled: Boolean) {
        musicEnabled = enabled
        if (!enabled) {
            pauseBackgroundMusic()
        } else {
            resumeBackgroundMusic()
        }
    }

    /**
     * Ses seviyesini ayarla (0.0 - 1.0)
     */
    fun setSoundVolume(volume: Float) {
        soundVolume = volume.coerceIn(0f, 1f)
    }

    /**
     * Müzik seviyesini ayarla (0.0 - 1.0)
     */
    fun setMusicVolume(volume: Float) {
        musicVolume = volume.coerceIn(0f, 1f)
        mediaPlayer?.setVolume(musicVolume, musicVolume)
    }

    // --- Dahili Fonksiyonlar ---

    private fun playSound(soundId: Int, volume: Float = soundVolume) {
        if (!soundEnabled || soundId == 0) return
        val clampedVolume = volume.coerceIn(0f, 1f)
        soundPool?.play(soundId, clampedVolume, clampedVolume, 1, 0, 1f)
    }

    /**
     * Kaynakları serbest bırak
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        mediaPlayer?.release()
        mediaPlayer = null
    }
}
