package com.truerandom.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.truerandom.dao.TrackDao
import com.truerandom.db.entity.LikedTrackEntity

/**
 * The main database class, defined as an abstract class that extends RoomDatabase.
 *
 * entities: Lists all Entity classes (tables) included in the database.
 * version: Must be incremented whenever the database schema changes.
 * exportSchema: We set this to false for simplicity in development.
 */
@Database(
    entities = [LikedTrackEntity::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    // Abstract function to expose the DAO
    abstract fun trackDao(): TrackDao

    companion object {
        const val DATABASE_NAME = "spotify_randomizer_db"
    }
}
