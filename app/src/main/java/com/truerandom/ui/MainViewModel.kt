package com.truerandom.ui

import android.app.Activity
import android.content.Context
import android.util.Log
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.paging.Pager
import androidx.paging.PagingConfig
import androidx.paging.PagingData
import androidx.paging.cachedIn
import com.google.gson.Gson
import com.spotify.android.appremote.api.ConnectionParams
import com.spotify.android.appremote.api.Connector
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.spotify.sdk.android.auth.AuthorizationClient
import com.spotify.sdk.android.auth.AuthorizationRequest
import com.spotify.sdk.android.auth.AuthorizationResponse
import com.truerandom.db.entity.LikedTrackEntity
import com.truerandom.model.LikedTrackWithCount
import com.truerandom.repository.LikedSongsApiRepository
import com.truerandom.repository.LikedSongsDBRepository
import com.truerandom.repository.SecurePreferencesRepository
import com.truerandom.service.TrackService
import com.truerandom.util.EventsUtil
import com.truerandom.util.LogUtil
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.Flow
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

    // Paged list for liked tracks
    val likedTracksPagedFlow: Flow<PagingData<LikedTrackWithCount>> = Pager(
        config = PagingConfig(
            pageSize = 60,            // Loads 60 items at a time
            enablePlaceholders = true, // Shows scrollbar accurately for 20k rows
            prefetchDistance = 20      // Loads next page when 20 items from bottom
        ),
        pagingSourceFactory = {
            likedSongsDBRepository.getPagedLikedTracksWithCount()
        }
    ).flow.cachedIn(viewModelScope)

    /**
     * Auth related
     **/
    // Entry point to fetch liked songs using access token
    fun checkAndFetchLikedTracks(activity: Activity) {
        LogUtil.d(TAG, "MainViewModel, checkAndFetchLikedTracks: ")

        // Check if accessToken is still valid
        if (isAccessTokenValid()) {
            // Still valid - get accessToken from sharedPrefs and do stuff
            LogUtil.d(
                TAG,
                "MainViewModel, checkAndFetchLikedTracks: accessToken valid, checking liked songs..."
            )
            accessToken = preferencesRepository.getAccessToken()
            checkFirstTimeSync()
        } else {
            // Invalid - get new token (user needs to auth again)
            LogUtil.d(
                TAG,
                "MainViewModel, checkAndFetchLikedTracks: accessToken invalid, starting auth flow..."
            )
            startAuthFlow(activity)
        }
    }

    // Check if accessToken is valid (available and NOT expired)
    private fun isAccessTokenValid(): Boolean {
        val tokenExpiry = preferencesRepository.getAccessTokenExpireTimestamp()
        val currentTime = System.currentTimeMillis()
        return tokenExpiry > currentTime
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
        when (response.type) {
            AuthorizationResponse.Type.TOKEN -> {
                accessToken = response.accessToken

                // Safe accessToken + expiry timestamp to SP
                accessToken?.let { preferencesRepository.saveAccessToken(it) }
                val accessTokenExpireTimestamp =
                    System.currentTimeMillis() + (response.expiresIn * 1000)
                preferencesRepository.saveAccessTokenExpireTimestamp(accessTokenExpireTimestamp)

                // Start syncing like songs to Room db
                checkFirstTimeSync()
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

    private fun checkFirstTimeSync() {
        viewModelScope.launch {
            // Fetch full list if DB not synced yet
            if (!likedSongsDBRepository.isLikedSongsSynced()) {
                syncLikedTracksInRoom()
            }
        }
    }

    // Start syncing liked tracks from API to Room DB
    suspend fun syncLikedTracksInRoom() {
        isLoading = true

        LogUtil.d(
            TAG,
            "MainViewModel, syncLikedTracksInRoom: starting paged fetching..."
        )

        // Clear all tracks in liked tracks table first
        likedSongsDBRepository.deleteAllLikedTracks()

        var url: String? = "https://api.spotify.com/v1/me/tracks?limit=50"

        var totalTracks: Int? = null

        // Keep looking until returned url is null (reached end of list)
        while (url != null) {
            val response = likedSongsApiRepository.fetchLikedSongsPage(
                accessToken ?: return, url
            )
            LogUtil.d(
                TAG,
                "MainViewModel, syncing ${response.items?.count()} of ${response.total}"
            )

            // Show toast for first iteration
            if (totalTracks != response.total) {
                totalTracks = response.total
                toastMsg = "Syncing $totalTracks tracks..."
            }

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
                            addedAt = item.addedAt,

                            // Use first url in album images list as cover
                            albumCoverUrl = track.album?.images?.firstOrNull()?.url,
                            durationMs = track.durationMs
                        )
                    }
                }
            }?.let { likedTrackEntityList ->
                // Batch insert to DB
                Log.d(
                    TAG,
                    "MainViewModel, likedTrackEntityList count = ${likedTrackEntityList.count()}"
                )
                likedSongsDBRepository.saveLikedTracks(likedTrackEntityList)
            }

            // Update url for next paged request
            url = response.next
        }

        isLoading = false
    }

    /**
     * App remote related
     **/
    // Connect appRemote - this must be done before auth
    fun attemptAppRemoteConnect(context: Context) {
        LogUtil.d(TAG, "attemptAppRemoteConnect")
        isLoading = true

        // Use showAuthView(true) to implicitly re-authorize the user without a full login screen
        val connectionParams = ConnectionParams.Builder(CLIENT_ID)
            .setRedirectUri(REDIRECT_URI)
            .showAuthView(true)
            .build()

        // Connect
        SpotifyAppRemote.connect(context, connectionParams, object : Connector.ConnectionListener {
            override fun onConnected(spotifyAppRemote: SpotifyAppRemote) {
                LogUtil.d(TAG, "attemptAppRemoteConnect: SpotifyAppRemote onConnected")
                isLoading = false

                // Set instance to sticky service
                TrackService.mSpotifyAppRemote = spotifyAppRemote

                // Register system media callback
                EventsUtil.sendRegisterMediaCallbackEvent(true)

                // TODO: JAY_LOG - remove if unused
//                subscribeToPlayerState()

                toastMsg = "Spotify Remote Connected!"
            }

            override fun onFailure(throwable: Throwable) {
                LogUtil.d(TAG, "attemptAppRemoteConnect: SpotifyAppRemote onFailure: ${throwable.message}")
                isLoading = false

                toastMsg = "Spotify Remote Failed: ${throwable.message}"
            }
        })
    }

    // TODO: JAY_LOG - remove if unused
    // Subscribe to player state to detect end and play next random track
    private fun subscribeToPlayerState() {
        TrackService.mSpotifyAppRemote?.playerApi
            ?.subscribeToPlayerState()
            ?.setEventCallback { playerState ->
                val track = playerState.track
                if (track != null) {
                    // Only care for the current trackId
                    if (playerState.track.uri != TrackService.currentTrackUri) {
                        LogUtil.d(TAG, "MainVM playerState trackUri different: playerUri = ${playerState.track.uri}, " +
                                "currentUri = ${TrackService.currentTrackUri} - do nothing...")
                        return@setEventCallback
                    }

                    // Log and update your UI/Service state with the new track and position
                    LogUtil.d(
                        TAG,
                        "MainVM playerState: trackUri = ${track.uri}, isPaused = ${playerState.isPaused}, " +
                                "position = ${playerState.playbackPosition}"
                    )

                    // Paused - check playback position
                    if (playerState.isPaused) {

                        // Position == 0, this track is done - play next random track
                        if (playerState.playbackPosition == 0L) {
                            if (TrackService.hasDetectedTrackEnd) return@setEventCallback

                            LogUtil.d(TAG, "Track ${track.name} is ending. Trigger next track logic.")
                            TrackService.hasDetectedTrackEnd = true

//                            EventsUtil.sendTrackPlaybackEndEvent()
                        } else {
                            // Position NOT 0 - just update UI to PAUSED
                            TrackService.isPlaying = false
                        }

                    } else {
                        // Player changed to PLAY - don't do anything for now
                    }
                }
            }?.setErrorCallback { error ->
                Log.e(TAG, "subscribeToPlayerState: error = ", error)
                toastMsg = "Error while subscribing to playerState: ${error.message}"
            }
    }
}