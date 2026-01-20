package com.truerandom.di

import android.content.Context
import androidx.room.Room
import com.truerandom.dao.PlayCountDao
import com.truerandom.dao.TrackDao
import com.truerandom.db.AppDatabase
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

/**
 * Hilt module to provide the database and DAO instances as singletons.
 * This makes them injectable anywhere in the application.
 */
@Module
@InstallIn(SingletonComponent::class)
object DatabaseModule {

    /**
     * Provides a singleton instance of the Room database.
     */
    @Provides
    @Singleton
    fun provideDatabase(@ApplicationContext context: Context): AppDatabase {
        return Room.databaseBuilder(
            context,
            AppDatabase::class.java,
            AppDatabase.DATABASE_NAME
        )
            .addMigrations(AppDatabase.MIGRATION_1_2, AppDatabase.MIGRATION_2_3)
            .build()
    }

    /**
     * Provides the DAO instance, obtaining it from the provided AppDatabase.
     */
    @Provides
    fun provideTrackDao(database: AppDatabase): TrackDao {
        return database.trackDao()
    }
    @Provides
    fun providePlayCountDao(database: AppDatabase): PlayCountDao {
        return database.playCountDao()
    }
}
