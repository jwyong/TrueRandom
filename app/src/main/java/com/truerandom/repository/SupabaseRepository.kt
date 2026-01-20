package com.truerandom.repository

import com.truerandom.db.entity.PlayCountEntity
import com.truerandom.util.Resource
import io.github.jan.supabase.SupabaseClient
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.postgrest
import io.github.jan.supabase.postgrest.query.filter.FilterOperator
import javax.inject.Inject
import javax.inject.Singleton

private const val PLAY_COUNT_TABLE = "play_count"

@Singleton
class SupabaseRepository @Inject constructor(private val client: SupabaseClient) {
    /**
     * Fetches all records from the 'play_count' table.
     * Returns a List of PlayCountEntity or an empty list if an error occurs.
     */
    suspend fun getAllPlayCounts(): List<PlayCountEntity> {
        return try {
            // 1. Target the table 'play_count'
            // 2. Select all columns
            // 3. Decode the JSON response into our Data Class list
            client.postgrest[PLAY_COUNT_TABLE]
                .select()
                .decodeList<PlayCountEntity>()
        } catch (e: Exception) {
            println("Error fetching from Supabase: ${e.message}")
            emptyList()
        }
    }

    /**
     * Batch uploads multiple play counts to Supabase.
     * If a trackUri already exists, it updates the playCount (REPLACE logic).
     */
    suspend fun upsertPlayCounts(playCounts: List<PlayCountEntity>): Resource<Int> {
        if (playCounts.isEmpty()) return Resource.Error(message = "Play counts list is empty")

        try {
            client.postgrest[PLAY_COUNT_TABLE].upsert(playCounts) {
                // Specify the conflict column inside this block
                onConflict = "trackUri"
            }
            return Resource.Success(playCounts.size)
        } catch (e: Exception) {
            e.printStackTrace()
            return Resource.Error(message = e.message ?: "Unknown error")
        }
    }

    // Delete list of items from supabase (cleanup old tracks)
    suspend fun deletePlayCountsFromCloud(trackUris: List<String>) {
        if (trackUris.isEmpty()) return

        // Postgrest requires the list to be a string formatted like (item1,item2)
        val formattedList = trackUris.joinToString(prefix = "(", postfix = ")", separator = ",")

        client.from(PLAY_COUNT_TABLE).delete {
            filter {
                // Raw filter: column name, operator, and the formatted string
                filter("trackUri", FilterOperator.IN, formattedList)
            }
        }
    }

    // Upsert 1 playCountEntity (at the end of playback)
    suspend fun upsertPlayCount(entity: PlayCountEntity) {
        try {
            // This looks for a row where 'trackUri' matches.
            // If found, it updates 'playCount'. If not, it creates a new row.
            client.postgrest[PLAY_COUNT_TABLE].upsert(entity)
            println("Successfully synced ${entity.trackUri} to Supabase")
        } catch (e: Exception) {
            println("Failed to sync to Supabase: ${e.message}")
        }
    }
}