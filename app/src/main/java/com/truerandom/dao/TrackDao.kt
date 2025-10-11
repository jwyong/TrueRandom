package com.truerandom.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.truerandom.db.entity.LikedTrackEntity

/**
 * Data Access Object for the LikedTrackEntity.
 * Defines the methods for interacting with the database.
 */
@Dao
interface TrackDao {
    // Get tracks count
    @Query("SELECT COUNT(trackUri) FROM liked_tracks")
    suspend fun getTrackCount(): Int

    /**
     * Inserts a list of LikedTrackEntity objects into the database.
     * If a conflict occurs (based on the primary key, which is the track ID),
     * the existing row is replaced.
     */
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(tracks: List<LikedTrackEntity>)

    /**
     * Increments the playCount of a specific track identified by its URI.
     *
     * @param trackUri The unique Spotify URI of the track to update.
     * @return The number of rows updated (should be 1 if successful).
     */
    @Query("UPDATE liked_tracks SET playCount = playCount + 1 WHERE trackUri = :trackUri")
    suspend fun incrementPlayCount(trackUri: String): Int

    /**
     * OPTIMIZED QUERY: Fetches only the URIs of tracks that have the minimum playCount.
     * This minimizes the data transfer overhead for large "unplayed" pools.
     *
     * @return A list of track URIs (String) sharing the lowest play count.
     */
    @Query("""
        SELECT trackUri FROM liked_tracks 
        WHERE playCount = (SELECT MIN(playCount) FROM liked_tracks)
    """)
    suspend fun getLeastPlayedTrackUris(): List<String>

}
