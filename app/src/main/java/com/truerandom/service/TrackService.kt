package com.truerandom.service

import android.app.Service
import android.content.Intent
import android.os.IBinder
import android.util.Log
import androidx.lifecycle.MutableLiveData
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.truerandom.repository.LikedSongsDBRepository
import com.truerandom.repository.SecurePreferencesRepository
import com.truerandom.ui.TAG
import com.truerandom.util.EventsUtil
import com.truerandom.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackService: Service() {
    @Inject
    lateinit var likedSongsDBRepository: LikedSongsDBRepository

    // Dedicated scope for collecting event flows
    private val collectScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Dedicated Default scope for all other operations
    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onCreate() {
        super.onCreate()

        Log.d("JAY_LOG", "TrackService, onCreate: ")

        collectPlayPauseBtnEvent()
        collectPrevNextBtnEvent()
        collectCurrentTrackEndedEvent()

        // Show foreground notification
        NotificationUtil.createNotificationChannel(this)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Log.d("JAY_LOG", "TrackService, onStartCommand: ")

        // Start foreground service as normal first
        startForeground(
            NotificationUtil.NOTIFICATION_ID, NotificationUtil.createNotification(this)
        )

        return START_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Flow event collectors
     **/
    // Start or pause playback btn clicked (from mainActivity OR notification)
    private fun collectPlayPauseBtnEvent() {
        collectScope.launch {
            EventsUtil.playPauseButtonEventFlow.flow.collect {
                Log.d(TAG, "TrackService, collectPlayPauseBtnEvent: isPlaying = $isPlaying")

                if (isPlaying) {
                    mSpotifyAppRemote?.playerApi?.pause()
                } else {
                    currentTrackUri?.let {
                        Log.d(
                            TAG,
                            "TrackService, collectPlayPauseBtnEvent: currentTrack available, resuming..."
                        )
                        // Resume playback if already has uri
                        mSpotifyAppRemote?.playerApi?.resume()
                    }?: run {
                        Log.d(
                            TAG,
                            "TrackService, collectPlayPauseBtnEvent: no currentTrack, starting new playback..."
                        )
                        // No uri - start randomise flow (no need to increment play count)
                        playRandomLeastCountTrack(false)
                    }
                }
                isPlaying = !isPlaying
            }
        }
    }

    // Next or previous btn clicked (from mainActivity OR notification)
    private fun collectPrevNextBtnEvent() {
        collectScope.launch {
            EventsUtil.prevNextButtonEventFlow.flow.collect { isNext ->
                Log.d(TAG, "TrackService, collectPrevNextBtnEvent: isNext = $isNext")

                if (isNext) {
                    // Next - just play random track WITHOUT incrementing
                    playRandomLeastCountTrack(false)
                } else {
                    // Prev - just play previous track, no increments
                    currentTrackUri = previousTrackUri

                    // Start playback on appRemote
                    playCurrentTrackUri()

                    // Can only prev 1 track
                }
            }
        }
    }

    // When current track playback ended - just play next least random track after increment current
    private fun collectCurrentTrackEndedEvent() {
        collectScope.launch {
            EventsUtil.trackPlaybackEndEventFlow.flow.collect {
                Log.d(TAG, "TrackService, collectCurrentTrackEndedEvent: ")

                playRandomLeastCountTrack(true)
            }
        }
    }

    /**
     * Choose a new random uri to be played, based on lowest play count. This is called from:
     * - fresh new session play btn
     * - when a track has finished playing (play next track)
     * - next btn when track is playing
     **/
    private fun playRandomLeastCountTrack(shouldIncrementPlayCount: Boolean) {
        defaultScope.launch {
            Log.d(TAG, "TrackService, playRandomLowCountTrack: ")

            // Check if current uri available
            currentTrackUri?.let { currentUri ->
                Log.d(TAG, "TrackService, playRandomLowCountTrack: currentTrack available, incrementing playCount...")

                // Increment current track's playCount if needed (for normal playback finished)
                if (shouldIncrementPlayCount) {
                    val row = likedSongsDBRepository.incrementTrackPlayCount(currentUri)
                    Log.d(TAG, "TrackService, playRandomLeastCountTrack: incremented row = $row")
                }

                // Set current track to previous track
                previousTrackUri = currentTrackUri
                Log.d(
                    TAG,
                    "TrackService, playRandomLeastCountTrack: previousTrack = $previousTrackUri"
                )
            }

            // Get random uri and set to currentUri
            currentTrackUri = likedSongsDBRepository.getRandomLeastPlayedTrackUri()
            Log.d(
                TAG,
                "TrackService, playRandomLeastCountTrack: currentTrackUri = $currentTrackUri"
            )

            // Start playback on appRemote
            playCurrentTrackUri()
        }
    }

    private fun playCurrentTrackUri() {
        Log.d(TAG, "TrackService, playCurrentTrackUri: start playing $currentTrackUri")

        mSpotifyAppRemote?.playerApi
            ?.play(currentTrackUri)
            ?.setResultCallback {
                // SUCCESS: The command was successfully sent to the Spotify client,
                // and the client has acknowledged it and started playback.
                Log.d(TAG, "playCurrentTrackUri: Successfully started playing URI: $currentTrackUri")

                // Reset trackEnd detected boolean
                hasDetectedTrackEnd = false
            }
    }

    companion object {
        // Shared prefs
        private lateinit var securePreferencesRepository: SecurePreferencesRepository
        fun initSecurePrefs(repo: SecurePreferencesRepository) {
            securePreferencesRepository = repo
        }

        // App remote related
        var mSpotifyAppRemote: SpotifyAppRemote? = null

        val isPlayingLD = MutableLiveData(false)
        var isPlaying: Boolean
            get() = isPlayingLD.value?: false
            set(value) { isPlayingLD.postValue(value) }

        var previousTrackUri: String? = null
        var currentTrackUri: String? = null

        // Debounce for playState subscription
        var hasDetectedTrackEnd = false
    }
}