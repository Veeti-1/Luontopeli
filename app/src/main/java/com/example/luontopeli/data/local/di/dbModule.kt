package com.example.luontopeli.data.local.di

import android.content.Context
import androidx.room.Room
import com.example.luontopeli.data.local.AppDatabase
import com.example.luontopeli.data.local.dao.NatureSpotDao
import com.example.luontopeli.data.local.dao.WalkSessionDao
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    @Provides
    @Singleton
    fun provideDatabase(
        @ApplicationContext context: Context
    ): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            "luontopeli_database"
        )
            .fallbackToDestructiveMigration()
            .build()
    }

    @Provides
    fun provideNatureSpotDao(db: AppDatabase): NatureSpotDao =
        db.natureSpotDao()

    @Provides
    fun provideWalkSessionDao(db: AppDatabase): WalkSessionDao =
        db.walkSessionDao()
}