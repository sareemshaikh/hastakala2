package com.hastakala.testshop.repository

interface AuthRepository {
    suspend fun signUp(email: String, password: String): Result<Unit>
    suspend fun login(email: String, password: String): Result<Unit>
    suspend fun forgotPassword(email: String): Result<Unit>
    suspend fun signInWithGoogle(idToken: String): Result<Unit>
    fun logout()
    fun isLoggedIn(): Boolean
    fun getCurrentUserId(): String?
}
