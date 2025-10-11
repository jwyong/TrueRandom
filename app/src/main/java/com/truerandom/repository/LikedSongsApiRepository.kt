package com.truerandom.repository

import android.util.Log
import com.google.gson.Gson
import com.truerandom.model.LikedTracksFullResponse
import com.truerandom.ui.TAG
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

@Singleton
class LikedSongsApiRepository @Inject constructor(
    private val okHttpClient: OkHttpClient,
    private val gson: Gson,
) {
    // The initial endpoint to fetch the first page of liked tracks

    /**
     * Fetches a single page of liked songs from the Spotify API.
     *
     * This is a clean, one-shot network function that uses coroutines to convert
     * OkHttp's asynchronous Callback into a synchronous suspend function call.
     *
     * @param accessToken The Bearer token for authorization.
     * @param url The API endpoint to fetch (can be INITIAL_URL or a 'next' URL).
     * @return The parsed LikedTracksFullResponse object containing the results and the next page URL.
     * @throws IOException if the network call fails or the response is not successful.
     */
    suspend fun fetchLikedSongsPage(accessToken: String, url: String): LikedTracksFullResponse {
        Log.d(TAG, "Fetching liked songs page from: $url")

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $accessToken")
            .build()

        return withContext(Dispatchers.IO) {
            suspendCoroutine { continuation ->
                okHttpClient.newCall(request).enqueue(object : Callback {
                    override fun onFailure(call: Call, e: IOException) {
                        Log.e(TAG, "Failed to get liked songs from $url", e)
                        // Resume coroutine with exception for structured concurrency error handling
                        continuation.resumeWithException(e)
                    }

                    override fun onResponse(call: Call, response: Response) {
                        if (!response.isSuccessful) {
                            Log.e(
                                TAG,
                                "API call failed (${response.code}) for $url. Message: ${response.message}"
                            )
                            // Resume coroutine with a structured exception containing response details
                            continuation.resumeWithException(IOException("API Error ${response.code}: ${response.message}"))
                            return
                        }

                        val body = response.body?.string()
                        if (body.isNullOrEmpty()) {
                            Log.w(TAG, "Empty response body for $url")
                            continuation.resumeWithException(IOException("API Error ${response.code}: ${response.message}"))
                            return
                        }

                        try {
                            // Deserialize the JSON body into the data model
                            val likedSongsResponse =
                                gson.fromJson(body, LikedTracksFullResponse::class.java)
                            continuation.resume(likedSongsResponse)
                        } catch (e: Exception) {
                            Log.e(TAG, "JSON parsing error: ${e.message}", e)
                            continuation.resumeWithException(e)
                        }
                    }
                })
            }
        }
    }

// TODO: JAY_LOG - remove in unnecessary
//    suspend fun fetchLikedSongsSinglePage() {
//        val url = "https://api.spotify.com/v1/me/tracks?limit=50"
//
//        val request = Request.Builder()
//            .url(url)
//            .addHeader("Authorization", "Bearer $accessToken")
//            .build()
//
//        OkHttpClient().newCall(request).enqueue(object : Callback {
//            override fun onFailure(call: Call, e: IOException) {
//                Log.e(TAG, "Failed to get liked songs", e)
//            }
//
//            override fun onResponse(call: Call, response: Response) {
//                val body = response.body?.string()
//                Log.d(TAG, "Liked Songs JSON: $body")
//
//                val likedSongsResponse = gson.fromJson(body, LikedTracksFullResponse::class.java)
//                likedSongsResponse.items?.lastOrNull()?.track?.uri?.let { trackUri ->
//                    playTrack(trackUri)
//                }
//            }
//        })
//    }
}