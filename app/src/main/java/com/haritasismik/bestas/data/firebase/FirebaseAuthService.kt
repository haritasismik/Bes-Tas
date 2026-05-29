package com.haritasismik.bestas.data.firebase

/**
 * Firebase Authentication servisi - STUB
 * Firebase kurulduktan sonra gerçek implementasyonla değiştirilecek.
 */
class FirebaseAuthService {

    val isLoggedIn: Boolean = false
    val userId: String? = "local_user"
    val displayName: String = "Misafir"

    suspend fun signInAnonymously(): Result<Any> {
        return Result.success(Unit)
    }

    fun signOut() {}
}
