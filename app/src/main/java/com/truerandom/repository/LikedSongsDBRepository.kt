package com.truerandom.repository

import com.truerandom.dao.TrackDao
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.model.TrackUIDetails
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Repository layer for managing persistent liked songs data via Room.
 *
 * This layer abstracts the data source (TrackDao) from the ViewModel,
 * providing a cleaner API for data operations.
 */
@Singleton
class LikedSongsDBRepository @Inject constructor(
    private val trackDao: TrackDao
) {
    // Check if already synced liked songs (TODO: JAY_LOG - for now just check count)
    suspend fun isLikedSongsSynced(): Boolean {
        return withContext(Dispatchers.IO) {
            trackDao.getTrackCount() > 0
        }
    }

    /**
     * Saves a list of tracks into the database.
     * This is typically called after successfully fetching data from the Spotify Web API.
     *
     * @param tracks The list of track entities to insert or replace.
     */
    suspend fun saveLikedTracks(tracks: List<LikedTrackEntity>) {
        withContext(Dispatchers.IO) {
            trackDao.insertAll(tracks)
        }
    }

    /**
     * Increments the play count for a specific track URI.
     *
     * @param trackUri The unique Spotify URI of the track that was just played.
     */
    suspend fun incrementTrackPlayCount(trackUri: String): Int {
        return withContext(Dispatchers.IO) {
            // The DAO executes the UPDATE query to increment the count
            trackDao.incrementPlayCount(trackUri)
        }
    }

    /**
     * Fetches the list of tracks with the lowest play count, randomly selects one,
     * and returns its URI for playback. This implements the "Least Played" random selection.
     *
     * OPTIMIZATION: Fetches only URIs to minimize data transfer overhead.
     *
     * @return The track URI (String) of a random track from the least-played pool, or null if the DB is empty.
     */
    suspend fun getRandomLeastPlayedTrackUri(): String? {
        return withContext(Dispatchers.IO) {
            // 1. Use the optimized DAO function to get only URIs
            val leastPlayedTrackUris = trackDao.getLeastPlayedTrackUris()

            // 2. Randomly select one URI from this pool.
            // randomOrNull() ensures safety if the list is empty (which shouldn't happen
            // if isLikedSongsSynced() is checked, but safety first).
            leastPlayedTrackUris.randomOrNull()
        }
    }

    suspend fun getTrackDetailsByTrackUri(trackUri: String): TrackUIDetails? {
        return withContext(Dispatchers.IO) {
            trackDao.getTrackDetailsByUri(trackUri)
        }
    }
}
