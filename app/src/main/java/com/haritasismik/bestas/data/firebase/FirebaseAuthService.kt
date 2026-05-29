package com.haritasismik.bestas.data.firebase

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.FirebaseUser
import com.google.firebase.auth.GoogleAuthProvider
import kotlinx.coroutines.tasks.await

/**
 * Firebase Authentication servisi
 * Google Sign-In ve anonim giriş desteği
 */
class FirebaseAuthService {

    private val auth = FirebaseAuth.getInstance()

    val currentUser: FirebaseUser?
        get() = auth.currentUser

    val isLoggedIn: Boolean
        get() = auth.currentUser != null

    val userId: String?
        get() = auth.currentUser?.uid

    val displayName: String
        get() = auth.currentUser?.displayName ?: "Misafir"

    /**
     * Google token ile giriş yap
     */
    suspend fun signInWithGoogle(idToken: String): Result<FirebaseUser> {
        return try {
            val credential = GoogleAuthProvider.getCredential(idToken, null)
            val result = auth.signInWithCredential(credential).await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Kullanıcı bilgisi alınamadı"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Anonim giriş - online oynamak istemeyenler için
     */
    suspend fun signInAnonymously(): Result<FirebaseUser> {
        return try {
            val result = auth.signInAnonymously().await()
            result.user?.let {
                Result.success(it)
            } ?: Result.failure(Exception("Anonim giriş başarısız"))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Çıkış yap
     */
    fun signOut() {
        auth.signOut()
    }

    /**
     * Hesabı sil
     */
    suspend fun deleteAccount(): Result<Unit> {
        return try {
            auth.currentUser?.delete()?.await()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
}
