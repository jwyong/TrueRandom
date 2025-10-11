package com.truerandom.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * Defines the database table structure for a single Liked Track.
 *
 * NOTE: We use only the essential fields here. The Spotify track URI is critical
 * for playback and is used as the primary key.
 */
@Entity(tableName = "liked_tracks")
data class LikedTrackEntity(
    // The Spotify URI is unique and perfect as a Primary Key
    @PrimaryKey
    val trackUri: String,

    val isPlayable: Boolean?,
    val isLocal: Boolean?,
    
    // Track information for display
    val trackName: String?,
    val artistName: String?,
    val albumCoverUrl: String? = null, // TODO: JAY_LOG - empty for now
    
    // Metadata for ordering/display
    val addedAt: String?, // ISO 8601 timestamp from Spotify

    // Play count!
    val playCount: Int = 0
)
