package com.truerandom.model

data class LikedTrackWithCount(
    val trackUri: String,
    val trackName: String?,
    val artistName: String?,
    val albumCoverUrl: String?,
    val isLocal: Boolean?,
    val addedAt: String?,
    val playCount: Int // From the play_count table
)