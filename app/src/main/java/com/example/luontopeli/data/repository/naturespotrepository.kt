package com.example.luontopeli.data.repository






import com.example.luontopeli.data.local.dao.NatureSpotDao
import com.example.luontopeli.data.local.entity.NatureSpot
import com.example.luontopeli.data.remote.AuthManager
import com.example.luontopeli.data.remote.firestoreManager
import com.example.luontopeli.data.remote.StorageManager

import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class NatureSpotRepository @Inject constructor(
    private val dao: NatureSpotDao,


) {
    val allSpots = dao.getAllSpots()

    val spotsWithLocation: Flow<List<NatureSpot>> = dao.getSpotsWithLocation()
    suspend fun insertSpot(spot: NatureSpot) {
        dao.insert(spot)
    }/*
    suspend fun insertSpot(spot: NatureSpot) {
        val spotWithUser = spot.copy(userId = authManager.currentUserId, synced = true)
        dao.insert(spotWithUser)
        syncSpotToFirebase(spotWithUser)
    }*/
  /*
    private suspend fun syncSpotToFirebase(spot: NatureSpot) {
        try {

            val firebaseImageUrl = spot.imageLocalPath?.let { localPath ->
                storageManager.uploadImage(localPath, spot.id).getOrNull()
            }


            val spotWithUrl = spot.copy(imageFirebaseUrl = firebaseImageUrl)
           // firestoreManager.saveSpot(spotWithUrl).getOrThrow()


            dao.markSynced(spot.id, firebaseImageUrl ?: "")
        } catch (e: Exception) {

        }
    }
*/
    suspend fun deleteSpot(spot: NatureSpot) {
        dao.delete(spot)
    }

    suspend fun getUnsyncedSpots(): List<NatureSpot> {
        return dao.getUnsyncedSpots()
    }
}
