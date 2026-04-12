package com.example.luontopeli.data.remote

import javax.inject.Inject

class AuthManager @Inject constructor() {

    private val localUserId: String = java.util.UUID.randomUUID().toString()


    val currentUserId: String
        get() = localUserId


    val isSignedIn: Boolean
        get() = true


    suspend fun signInAnonymously(): Result<String> {
        return Result.success(localUserId)
    }


    fun signOut() {
        // No-op offline-tilassa
    }
}