package com.truerandom

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.util.Log
import android.widget.Toast
import androidx.lifecycle.ViewModel
import com.google.gson.Gson
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
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
const val REDIRECT_URI = "truerandom://auth"

class MainActivityViewModel : ViewModel() {
    private val SCOPES = arrayOf(
        "user-read-email", "user-read-private", "streaming", "user-library-read", "user-modify-playback-state"
    )
    private val gson = Gson()

    private var accessToken: String? = null
    private var trackUri: String? = null

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

    // ========= OLD CODES ABOVE!!!!
    // TODO: JAY_LOG - testing appRemote
    private var mSpotifyAppRemote: SpotifyAppRemote? = null

    // Change the function signature to return the Intent
    fun createSpotifyAuthorizationIntent(activity: Activity) {
        Log.d(TAG, "Creating Spotify Authorization Intent...")

        val builder = AuthorizationRequest.Builder(
            CLIENT_ID,
            AuthorizationResponse.Type.TOKEN,
            REDIRECT_URI
        )

        builder.setScopes(SCOPES)
        val request = builder.build()

        // use phone browser directly to open the URI (instead of spotify browser)
        AuthorizationClient.openLoginActivity(activity, AUTH_REQUEST_CODE, request)
    }

    fun onAuthResult(response: AuthorizationResponse) {
        Log.d(TAG, "Authorization Response Parsed. Type: ${response.type.name}")

        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                accessToken = response.accessToken

                // Detailed logging for the parsed response object
                Log.i(TAG, "Auth Token Success! Details:")
                Log.i(TAG, "   > Access Token: ${accessToken?.take(10)}...")
                Log.i(TAG, "   > Expires In: ${response.expiresIn} seconds")

                fetchLikedSongs(response.accessToken)
            }
            AuthorizationResponse.Type.ERROR -> {
                Log.e(TAG, "Authorization failed: ${response.error}")
            }
            else -> {
                Log.w(TAG, "Authorization result was cancelled or of an unexpected type.")
            }
        }
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
                likedSongsResponse.items?.lastOrNull()?.track?.uri?.let { trackUri ->
                    playTrack(trackUri)
                }
            }
        })
    }

    fun attemptAppRemoteConnect(context: Context) {
        // Log the type of Context being used to help diagnose UI-related connection failures
        Log.d(TAG, "MainActivityViewModel, attemptAppRemoteConnect: Executing delayed connect. Context Type: ${context::class.simpleName}")

        // NOTE: For 'showAuthView(true)' to work, 'context' should ideally be an Activity Context.

        // Use showAuthView(true) to implicitly re-authorize the user without a full login screen
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        // Connect
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                mSpotifyAppRemote = spotifyAppRemote
                Log.i(TAG, "SpotifyAppRemote connected successfully! Ready for playback.")
                Toast.makeText(context, "Spotify Remote Connected!", Toast.LENGTH_SHORT).show()

                // Next step in development would be to call a playback function here.
            }

            override fun onFailure(throwable: Throwable) {
                Log.e(TAG, "SpotifyAppRemote connection failed: ${throwable.message}", throwable)
                Toast.makeText(context, "Spotify Remote Failed: ${throwable.message}", Toast.LENGTH_LONG).show()
            }
        })
    }

    private fun playTrack(trackUri: String) {
        mSpotifyAppRemote?.let {
            // Play a playlist
            it.playerApi.play(trackUri)
            // Subscribe to PlayerState
            it.playerApi.subscribeToPlayerState().setEventCallback {
                Log.d("JAY_LOG", "MainActivityViewModel, connected: event callback = $it")
            }
        }

    }

}