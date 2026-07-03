package com.lksnext.ParkingJDorronsoro.model.service

interface AccountService {
    val currentUserId: String
    val hasUser: Boolean

    suspend fun authenticate(email: String, password: String)
    suspend fun createAccount(email: String, password: String)
    suspend fun linkAccount(email: String, password: String)
    suspend fun createAnonymousAccount()
    suspend fun sendRecoveryEmail(email: String)
    suspend fun deleteAccount()
    suspend fun signOut()
}