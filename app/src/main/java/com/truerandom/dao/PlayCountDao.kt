package com.truerandom.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Transaction
import com.truerandom.db.entity.PlayCountEntity

@Dao
interface PlayCountDao {
    // Get ALL items
    @Query("SELECT * FROM play_count")
    suspend fun getAllPlayCounts(): List<PlayCountEntity>

    // Get list of uris in playCount but NOT in the liked_tracks table
    @Query("SELECT trackUri FROM play_count WHERE trackUri NOT IN (:likedUris)")
    suspend fun getOrphanedPlayCountUris(likedUris: List<String>): List<String>

    // Insert new item
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPlayCount(playCount: PlayCountEntity): Long

    // Update existing item with +1
    @Query("UPDATE play_count SET playCount = playCount + 1 WHERE trackUri = :trackUri")
    suspend fun incrementExistingCount(trackUri: String): Int

    // Batch upsert playCounts
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun upsertAll(playCounts: List<PlayCountEntity>): List<Long>

    // Remove old trackUris (not in liked_tracks)
    @Query("DELETE FROM play_count WHERE trackUri NOT IN (:likedUris)")
    suspend fun deletePlayCountsNotInList(likedUris: List<String>): Int

    @Query("SELECT * FROM play_count WHERE trackUri = :trackUri")
    suspend fun getPlayCountByUri(trackUri: String): PlayCountEntity?

    /**
     * Increments the playCount of a specific track identified by its URI. Adds a new row with count 1
     * if the trackUri doesn't exist yet.
     *
     * @param trackUri The unique Spotify URI of the track to update.
     * @return The number of rows updated (should be 1 if successful).
     */
    @Transaction
    suspend fun incrementPlayCount(trackUri: String): PlayCountEntity? {
        // 1. Perform the increment logic
        val rowsUpdated = incrementExistingCount(trackUri)

        if (rowsUpdated == 0) {
            insertPlayCount(PlayCountEntity(trackUri, 1))
        }

        // 2. Fetch the newly updated/inserted item
        return getPlayCountByUri(trackUri)
    }

}