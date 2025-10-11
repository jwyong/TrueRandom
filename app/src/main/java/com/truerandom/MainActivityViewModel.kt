package com.truerandom

import android.app.Activity
import android.content.Intent
import android.util.Log
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.truerandom.model.SpotifyAuth
import okhttp3.Call
import okhttp3.Callback
import okhttp3.FormBody
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okio.IOException

const val AUTH_REQUEST_CODE = 394056

const val CLIENT_ID = "e61d6a48cd14457c97e43850f03eb35c"
const val CLIENT_SECRET = "a699313bb43743029848c2e4d320e448"
const val REDIRECT_URI = "truerandom://auth/spotify-callback"

class MainActivityViewModel : ViewModel() {
    private val SCOPES = arrayOf(
        "user-read-email", "user-read-private", "streaming", "user-library-read", "user-modify-playback-state"
    )
    private val gson = Gson()

    // Change the function signature to return the Intent
    fun createSpotifyAuthorizationIntent(activity: Activity): Intent {
        Log.d(TAG, "Creating Spotify Authorization Intent...")

        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.CODE, // Requesting an access token
            REDIRECT_URI
        )

        builder.setScopes(SCOPES)
        val request = builder.build()

        // use phone browser directly to open the URI (instead of spotify browser)
        return Intent("android.intent.action.VIEW", request.toUri())
    }

    fun onRequestAuthResult(intent: Intent) {
        Log.d("JAY_LOG", "MainActivityViewModel, onRequestAuthResult: dataString = ${intent.dataString}")

        intent.data?.getQueryParameter("code")?.let { code ->
            Log.d("JAY_LOG", "MainActivityViewModel, onRequestAuthResult: code = $code")
            exchangeCodeForToken(code)
        }
    }

    private fun exchangeCodeForToken(code: String) {
        // 🛑 WARNING: EXPOSES CLIENT SECRET. USE ONLY IF YOU ACCEPT THE SECURITY RISK.

        // 1. Prepare Authentication and URL
        val authString = "$CLIENT_ID:$CLIENT_SECRET"
        // Base64 encode the Client ID and Secret for the Basic Auth header
        val encodedAuth = android.util.Base64.encodeToString(authString.toByteArray(), android.util.Base64.NO_WRAP)

        val client = OkHttpClient()
        val url = "https://accounts.spotify.com/api/token" // Spotify Token Endpoint

        // 2. Prepare Request Body (Form URL Encoded)
        val body = FormBody.Builder()
            .add("grant_type", "authorization_code")
            .add("code", code) // The code you successfully received
            .add("redirect_uri", REDIRECT_URI) // Must match the original redirect URI
            .build()

        // 3. Build and Execute the Request
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Basic $encodedAuth")
            .post(body)
            .build()

        client.newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Token exchange failed: ${e.message}")
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    val responseBody = response.body?.string()
                    Log.d(TAG, "Token Exchange Success: $responseBody")

                    val spotifyAuth = gson.fromJson(responseBody, SpotifyAuth::class.java)
                    Log.d(
                        "JAY_LOG",
                        "MainActivityViewModel, onResponse: spotifyAuth = $spotifyAuth"
                    )
                    spotifyAuth.accessToken?.let { accessToken ->
                        Log.d(
                            "JAY_LOG",
                            "MainActivityViewModel, onResponse: accessToken = $accessToken"
                        )
                        fetchLikedSongs(accessToken)
                    }

                } else {
                    Log.e(TAG, "Token Exchange Failed (HTTP ${response.code}): ${response.body?.string()}")
                }
            }
        })
    }

    private fun fetchLikedSongs(token: String) {
        val url = "https://api.spotify.com/v1/me/tracks?limit=50"

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to get liked songs", e)
            }

            override fun onResponse(call: Call, response: Response) {
                val body = response.body?.string()
                Log.d(TAG, "Liked Songs JSON: $body")

                val likedSongsResponse = gson.fromJson(body, LikedTracksFullResponse::class.java)
                likedSongsResponse.items?.firstOrNull()?.track?.uri?.let { trackUri ->
                    playTrack(token, trackUri)
                }
            }
        })
    }

    private fun playTrack(token: String, trackUri: String) {
        val url = "https://api.spotify.com/v1/me/player/play"

        // JSON body to specify the track URI
        // NOTE: This assumes an active device is available. Spotify will try to play on the last active device.
        val jsonBody = """
            {
                "uris": ["$trackUri"]
            }
        """.trimIndent()

        val requestBody = "application/json; charset=utf-8".toMediaType()
            .let { okhttp3.RequestBody.create(it, jsonBody) }

        val request = Request.Builder()
            .url(url)
            .addHeader("Authorization", "Bearer $token")
            .put(requestBody) // PUT request to modify playback state
            .build()

        OkHttpClient().newCall(request).enqueue(object : Callback {
            override fun onFailure(call: Call, e: IOException) {
                Log.e(TAG, "Failed to start playback", e)
            }

            override fun onResponse(call: Call, response: Response) {
                if (response.isSuccessful) {
                    // HTTP 204 No Content is the expected success response for a play command
                    Log.d(TAG, "Playback command SUCCESS (HTTP ${response.code}). Track URI: $trackUri")
                } else {
                    val errorBody = response.body?.string()
                    Log.e(TAG, "Playback command FAILED (HTTP ${response.code}): $errorBody")
                }
            }
        })
    }
}