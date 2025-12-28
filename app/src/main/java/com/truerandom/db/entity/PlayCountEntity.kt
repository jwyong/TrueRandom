package com.truerandom.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

/**
 * play count tied to trackUri (primary key in liked tracks table)
 */
@Entity(tableName = "play_count")
data class PlayCountEntity(
    // The Spotify URI is unique and perfect as a Primary Key
    @PrimaryKey
    val trackUri: String,

    // Play count!
    val playCount: Int = 0
)
