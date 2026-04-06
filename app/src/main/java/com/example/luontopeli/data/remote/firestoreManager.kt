package com.example.luontopeli.data.remote

// 📁 data/remote/firebase/FirestoreManager.kt

import com.example.luontopeli.data.local.entity.NatureSpot
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf


class FirestoreManager {


    suspend fun saveSpot(spot: NatureSpot): Result<Unit> {
        return Result.success(Unit)
    }

    /**
     * Simuloi käyttäjän löytöjen hakemista Firestoresta.
     * Palauttaa tyhjän listan.
     */
    fun getUserSpots(userId: String): Flow<List<NatureSpot>> {
        return flowOf(emptyList())
    }
}