package com.truerandom.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.gson.Gson
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.repository.LikedSongsApiRepository
import com.truerandom.repository.LikedSongsDBRepository
import com.truerandom.repository.SecurePreferencesRepository
import com.truerandom.service.TrackService
import com.truerandom.util.EventsUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import javax.inject.Inject

const val AUTH_REQUEST_CODE = 394056

// Auth params
private const val CLIENT_ID = "e61d6a48cd14457c97e43850f03eb35c"
private const val REDIRECT_URI = "truerandom://auth"

@HiltViewModel
class MainViewModel @Inject constructor(
    private val gson: Gson,
    private val preferencesRepository: SecurePreferencesRepository,
    private val likedSongsDBRepository: LikedSongsDBRepository,
    private val likedSongsApiRepository: LikedSongsApiRepository
) : ViewModel() {
    private val authScopes = arrayOf(
        "user-read-email",
        "user-read-private",
        "streaming",
        "user-library-read",
        "user-modify-playback-state"
    )
    private var accessToken: String? = null

    val isLoadingLD = MutableLiveData(false)
    private var isLoading: Boolean
        get() = isLoadingLD.value ?: false
        set(value) {
            isLoadingLD.value = value
        }

    val toastMsgLD = MutableLiveData("")
    private var toastMsg: String
        get() = toastMsgLD.value ?: ""
        set(value) {
            toastMsgLD.value = value
        }

    /**
     * Auth related
     **/
    // Entry point to fetch liked songs using access token
    fun checkAndFetchLikedTracks(activity: Activity) {
        Log.d(TAG, "MainViewModel, checkAndFetchLikedTracks: ")

        // Check if accessToken is still valid
        if (isAccessTokenValid()) {
            // Still valid - just do next step (sync liked songs using accessToken in SP)
            Log.d(
                TAG,
                "MainViewModel, checkAndFetchLikedTracks: accessToken valid, checking liked songs..."
            )
            syncLikedTracksInRoom()
        } else {
            // Invalid - get new token (user needs to auth again)
            Log.d(
                TAG,
                "MainViewModel, checkAndFetchLikedTracks: accessToken invalid, starting auth flow..."
            )
            startAuthFlow(activity)
        }
    }

    // Check if accessToken is valid (available and NOT expired)
    private fun isAccessTokenValid(): Boolean {
        return preferencesRepository.getAccessTokenExpireTimestamp() > System.currentTimeMillis()
    }

    // Start get access token (user needs to auth)
    private fun startAuthFlow(activity: Activity) {
        isLoading = true

        val builder = AuthorizationRequest.Builder(
            CLIENT_ID, AuthorizationResponse.Type.TOKEN, REDIRECT_URI
        )

        builder.setScopes(authScopes)
        val request = builder.build()

        // use phone browser directly to open the URI (instead of spotify browser)
        AuthorizationClient.openLoginActivity(activity, AUTH_REQUEST_CODE, request)
    }

    // After user auth (activity result)
    fun onAuthActivityResult(response: AuthorizationResponse) {
        Log.d(TAG, "Authorization Response Parsed. Type: ${response.type.name}")

        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                accessToken = response.accessToken

                // Safe accessToken + expiry timestamp to SP
                accessToken?.let { preferencesRepository.saveAccessToken(it) }
                val accessTokenExpireTimestamp =
                    System.currentTimeMillis() + (response.expiresIn * 1000)
                preferencesRepository.saveAccessTokenExpireTimestamp(accessTokenExpireTimestamp)

                // Start syncing like songs to Room db
                syncLikedTracksInRoom()
            }

            AuthorizationResponse.Type.ERROR -> {
                val error = "Authorization failed: ${response.error}"
                Log.e(TAG, error)
                toastMsg = error
            }

            else -> {
                toastMsg = "Authorization returned unhandled type: ${response.type}"
                Log.e(
                    TAG,
                    "onAuthActivityResult: unhandled type, response = ${gson.toJson(response)}"
                )
            }
        }
        isLoading = false
    }

    // Start syncing liked tracks from API to Room DB
    private fun syncLikedTracksInRoom() {
        // Fetch full list if DB not synced yet
        viewModelScope.launch {
            if (!likedSongsDBRepository.isLikedSongsSynced()) {
                isLoading = true

                Log.d(
                    TAG,
                    "MainViewModel, syncLikedTracksInRoom: liked songs not synced, starting paged fetching..."
                )

                var url: String? = "https://api.spotify.com/v1/me/tracks?limit=50"

                // Keep looking until returned url is null (reached end of list)
                while (url != null) {
                    Log.d(TAG, "MainViewModel, syncLikedTracksInRoom: url = $url")

                    val response = likedSongsApiRepository.fetchLikedSongsPage(
                        accessToken ?: return@launch, url
                    )
                    Log.d(
                        TAG,
                        "MainViewModel, syncLikedTracksInRoom: resp items count = ${response.items?.count()}"
                    )

                    // Map items to entity
                    response.items?.mapNotNull { item ->
                        item.track?.let { track ->
                            track.uri?.let { uri ->
                                LikedTrackEntity(
                                    trackUri = uri,
                                    trackName = track.name,
                                    artistName = track.formatArtistNames(),
                                    isLocal = track.isLocal,
                                    isPlayable = track.isPlayable,
                                    addedAt = item.addedAt
                                )
                            }
                        }
                    }?.let { likedTrackEntityList ->
                        // Batch insert to DB
                        Log.d(
                            TAG,
                            "MainViewModel, syncLikedTracksInRoom: likedTrackEntityList count = ${likedTrackEntityList.count()}"
                        )
                        likedSongsDBRepository.saveLikedTracks(likedTrackEntityList)
                    }

                    // Update url for next paged request
                    url = response.next
                    Log.d(TAG, "MainViewModel, syncLikedTracksInRoom: url updated to $url")
                }

                Log.d(TAG, "MainViewModel, syncLikedTracksInRoom: done fetching liked songs")

                isLoading = false
            }
        }
    }

    /**
     * App remote related
     **/
    // Connect appRemote - this must be done before auth
    fun attemptAppRemoteConnect(context: Context) {
        Log.d(TAG, "MainActivityViewModel, attemptAppRemoteConnect:")

        // Use showAuthView(true) to implicitly re-authorize the user without a full login screen
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        // Connect
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                Log.d(TAG, "MainViewModel, attemptAppRemoteConnect onConnected: ")

                // Set instance to sticky service
                TrackService.mSpotifyAppRemote = spotifyAppRemote

                subscribeToPlayerState()

                toastMsg = "Spotify Remote Connected!"
            }

            override fun onFailure(throwable: Throwable) {
                Log.e(TAG, "SpotifyAppRemote connection failed: ${throwable.message}", throwable)

                toastMsg = "Spotify Remote Failed: ${throwable.message}"
            }
        })
    }

    // Subscribe to player state to detect end and play next random track
    private fun subscribeToPlayerState() {
        TrackService.mSpotifyAppRemote?.playerApi
            ?.subscribeToPlayerState()
            ?.setEventCallback { playerState ->
                val track = playerState.track
                if (track != null) {
                    // TODO: JAY_LOG - remove this log when done
                    // Log and update your UI/Service state with the new track and position
                    Log.d(
                        TAG,
                        "Player State: Track ID: ${track.uri} - isPaused: ${playerState.isPaused}, " +
                                "position = ${playerState.playbackPosition}"
                    )

                    // Playback ended when isPause = true AND position = 0
                    if (playerState.isPaused && playerState.playbackPosition == 0L) {
                        if (TrackService.hasDetectedTrackEnd) return@setEventCallback

                        Log.d(TAG, "Track ${track.name} is ending. Trigger next track logic.")
                        TrackService.hasDetectedTrackEnd = true

                        EventsUtil.sendTrackPlaybackEndEvent()
                    }
                }
            }?.setErrorCallback { error ->
                Log.e(TAG, "subscribeToPlayerState: error = ", error)
            }
    }
}