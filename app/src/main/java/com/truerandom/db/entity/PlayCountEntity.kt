package com.truerandom.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import kotlinx.serialization.Serializable
/**
 * play count tied to trackUri (primary key in liked tracks table)
 */
@Serializable // Required for Supabase
@Entity(tableName = "play_count")
data class PlayCountEntity(
    // The Spotify URI is unique and perfect as a Primary Key
    @PrimaryKey
    val trackUri: String,

    // Play count!
    val playCount: Int = 0
)
