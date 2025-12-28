package com.truerandom.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.truerandom.dao.TrackDao
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.db.entity.PlayCountEntity

/**
 * The main database class, defined as an abstract class that extends RoomDatabase.
 *
 * entities: Lists all Entity classes (tables) included in the database.
 * version: Must be incremented whenever the database schema changes.
 * exportSchema: We set this to false for simplicity in development.
 */
@Database(
    entities = [LikedTrackEntity::class, PlayCountEntity::class],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    
    // Abstract function to expose the DAO
    abstract fun trackDao(): TrackDao

    companion object {
        const val DATABASE_NAME = "spotify_randomizer_db"

        // v1 to v2: move playCount column from liked_tracks table to new play_count table
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                // 1. Create the new play_count table
                db.execSQL("""
                    CREATE TABLE IF NOT EXISTS `play_count` (
                        `trackUri` TEXT NOT NULL, 
                        `playCount` INTEGER NOT NULL DEFAULT 0, 
                        PRIMARY KEY(`trackUri`)
                    )
                """)

                // 2. Copy playCount data from liked_tracks into the new table
                db.execSQL("""
                    INSERT INTO play_count (trackUri, playCount)
                    SELECT trackUri, playCount FROM liked_tracks
                """)

                // 3. Create a temporary version of liked_tracks without the playCount column
                db.execSQL("""
                    CREATE TABLE `liked_tracks_new` (
                        `trackUri` TEXT NOT NULL, 
                        `isPlayable` INTEGER, 
                        `isLocal` INTEGER, 
                        `trackName` TEXT, 
                        `artistName` TEXT, 
                        `albumCoverUrl` TEXT, 
                        `addedAt` TEXT, 
                        PRIMARY KEY(`trackUri`)
                    )
                """)

                // 4. Copy data into the new liked_tracks table
                db.execSQL("""
                    INSERT INTO liked_tracks_new (trackUri, isPlayable, isLocal, trackName, artistName, albumCoverUrl, addedAt)
                    SELECT trackUri, isPlayable, isLocal, trackName, artistName, albumCoverUrl, addedAt FROM liked_tracks
                """)

                // 5. Delete the old table and rename the new one
                db.execSQL("DROP TABLE liked_tracks")
                db.execSQL("ALTER TABLE liked_tracks_new RENAME TO liked_tracks")
            }
        }
    }
}
