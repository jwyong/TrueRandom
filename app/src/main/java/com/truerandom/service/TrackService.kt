package com.truerandom.service

import android.app.Service
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.session.MediaController
import android.media.session.MediaSessionManager
import android.media.session.PlaybackState
import android.os.IBinder
import android.provider.Settings
import com.spotify.android.appremote.api.SpotifyAppRemote
import com.truerandom.R
import com.truerandom.repository.LikedSongsDBRepository
import com.truerandom.repository.SecurePreferencesRepository
import com.truerandom.ui.TAG
import com.truerandom.util.EventsUtil
import com.truerandom.util.LogUtil
import com.truerandom.util.NotificationUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class TrackService : Service() {
    @Inject
    lateinit var likedSongsDBRepository: LikedSongsDBRepository

    // Dedicated scope for collecting event flows
    private val collectScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    // Dedicated Default scope for all other operations
    private val defaultScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    /**
     * System media event callback related
     **/
    // TODO: JAY_LOG - tidy up code + improve pause / play logics, permissions, etc
    private var spotifyController: MediaController? = null

    // 1. Define the Callback
    private val mediaCallback = object : MediaController.Callback() {
        override fun onPlaybackStateChanged(state: PlaybackState?) {
            state ?: return

            val isPaused = state.state == PlaybackState.STATE_PAUSED
            val playbackPosition = state.position

            if (isPaused) {
                // Position == 0, this track is done - play next random track
                if (playbackPosition == 0L) {
                    if (hasDetectedTrackEnd) return

                    LogUtil.d(TAG, "TrackService: onPlaybackStateChanged - $currentTrackLabel is ending. Trigger next track logic.")

                    hasDetectedTrackEnd = true

                    playRandomLeastCountTrack(true)

                } else {
                    LogUtil.d(TAG, "TrackService: onPlaybackStateChanged - $currentTrackLabel paused.")

                    // Position NOT 0 - just update UI to PAUSED
                    isPlaying = false
                }

            } else {
                // Player changed to PLAY - don't do anything for now
            }
        }
    }

    // 2. Function to find and bind to Spotify
    private fun connectToSpotifyController() {
        if (!isNotificationServiceEnabled()) {
            val intent = Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS")
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            startActivity(intent)
        }

        if (!isNotificationServiceEnabled()) {
            LogUtil.d(TAG, "Cannot bind to Spotify: Notification Access not granted.")
            // Optionally: Trigger the settings Intent here (see step 3)
            return
        }

        try {
            val mm = getSystemService(Context.MEDIA_SESSION_SERVICE) as MediaSessionManager
            val componentName = ComponentName(this, MediaNotificationListener::class.java)

            // This is the line that throws the SecurityException if not permitted
            val sessions = mm.getActiveSessions(componentName)
            val spotify = sessions.find { it.packageName == "com.spotify.music" }

            if (spotify != null) {
                spotifyController = spotify
                spotifyController?.registerCallback(mediaCallback)
                LogUtil.d(TAG, "Successfully bound to Spotify System Events")
            }
        } catch (e: SecurityException) {
            LogUtil.d(TAG, "SecurityException: Still missing permission! ${e.message}")
        }
    }

    private fun isNotificationServiceEnabled(): Boolean {
        val pkgName = packageName
        val flat = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
        return flat != null && flat.contains(pkgName)
    }

    override fun onCreate() {
        super.onCreate()

        collectPlayTrackEvent()
        collectPlayPauseBtnEvent()
        collectPrevNextBtnEvent()
        collectCurrentTrackEndedEvent()

        collectNotificationUiFlowEvent()

        // Show foreground notification
        NotificationUtil.createNotificationChannel(this)

        connectToSpotifyController()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // Start foreground service as normal first
        startForeground(
            NotificationUtil.NOTIFICATION_ID, NotificationUtil.createNotification(this)
        )

        return START_NOT_STICKY
    }

    override fun onBind(p0: Intent?): IBinder? {
        return null
    }

    /**
     * Flow event collectors
     **/
    // Specific track tapped - play that track immediately
    private fun collectPlayTrackEvent() {
        collectScope.launch {
            EventsUtil.playTrackEventFlow.flow.collect { trackUri ->
                LogUtil.d(TAG, "TrackService, playTrackEventFlow trackUri = $trackUri")

                // Set the trackUri to currentTrackUri, then play it
                currentTrackUri = trackUri
                playCurrentTrackUri()

                // Update UI
                isPlaying = true
            }
        }
    }

    // Start or pause playback btn clicked (from mainActivity OR notification)
    private fun collectPlayPauseBtnEvent() {
        collectScope.launch {
            EventsUtil.playPauseButtonEventFlow.flow.collect {
                LogUtil.d(TAG, "TrackService, playPauseButtonEventFlow: isPlaying = $isPlaying")

                if (isPlaying) {
                    mSpotifyAppRemote?.playerApi?.pause()
                } else {
                    currentTrackUri?.let {
                        LogUtil.d(
                            TAG,
                            "TrackService, collectPlayPauseBtnEvent: currentTrack available, resuming..."
                        )
                        // Resume playback if already has uri
                        mSpotifyAppRemote?.playerApi?.resume()
                    } ?: run {
                        LogUtil.d(
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
                LogUtil.d(TAG, "TrackService, collectPrevNextBtnEvent: isNext = $isNext")

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
                LogUtil.d(TAG, "TrackService, trackPlaybackEndEventFlow: ")

                playRandomLeastCountTrack(true)
            }
        }
    }

    // Notification UI - just create a new notification and set to foreground again
    private fun collectNotificationUiFlowEvent() {
        collectScope.launch {
            combine(isPlayingSF, currentTrackLabelSF) { _, _ ->
                true
            }.collect {
                updateNotificationUI()
            }
        }
    }

    private fun updateNotificationUI() {
        // 1. Re-run the function to get the new Notification object
        val updatedNotification = NotificationUtil.createNotification(this)

        // 2. Re-issue the notification using the same ID
        // This updates the existing foreground notification without affecting the service status
        startForeground(NotificationUtil.NOTIFICATION_ID, updatedNotification)
    }


    /**
     * Choose a new random uri to be played, based on lowest play count. This is called from:
     * - fresh new session play btn
     * - when a track has finished playing (play next track)
     * - next btn when track is playing
     **/
    private fun playRandomLeastCountTrack(shouldIncrementPlayCount: Boolean) {
        defaultScope.launch {
            LogUtil.d(TAG, "TrackService, playRandomLowCountTrack: ")

            // Check if current uri available
            currentTrackUri?.let { currentUri ->
                LogUtil.d(TAG, "TrackService, playRandomLowCountTrack: currentTrack available")

                // Increment current track's playCount if needed (for normal playback finished)
                if (shouldIncrementPlayCount) {
                    val isIncremented = likedSongsDBRepository.incrementTrackPlayCount(currentUri)
                    LogUtil.d(
                        TAG,
                        "TrackService, playRandomLeastCountTrack: isIncremented = $isIncremented"
                    )
                }

                // Set current track to previous track
                previousTrackUri = currentTrackUri
                LogUtil.d(
                    TAG,
                    "TrackService, playRandomLeastCountTrack: previousTrack = $previousTrackUri"
                )
            }

            // Get random uri and set to currentUri
            currentTrackUri = likedSongsDBRepository.getRandomLeastPlayedTrackUri()
            LogUtil.d(
                TAG,
                "TrackService, playRandomLeastCountTrack: currentTrackUri = $currentTrackUri"
            )

            // Start playback on appRemote
            playCurrentTrackUri()
        }
    }

    private fun playCurrentTrackUri() {
        LogUtil.d(TAG, "TrackService, playCurrentTrackUri: start playing $currentTrackUri")

        mSpotifyAppRemote?.playerApi
            ?.play(currentTrackUri)
            ?.setResultCallback {
                // SUCCESS: The command was successfully sent to the Spotify client,
                // and the client has acknowledged it and started playback.
                LogUtil.d(TAG, "TrackService, playCurrentTrackUri: Successfully started playing URI: $currentTrackUri")

                // Reset trackEnd detected boolean
                hasDetectedTrackEnd = false

                // Update UI (title and artist)
                defaultScope.launch {
                    currentTrackUri?.let { trackUri ->
                        val trackUIDetail = likedSongsDBRepository.getTrackDetailsByTrackUri(trackUri)
                        val trackName = if (trackUIDetail?.trackName?.isNotBlank() == true) {
                            trackUIDetail.trackName
                        } else {
                            getString(R.string.unknown)
                        }
                        val trackArtists = if (trackUIDetail?.artistName?.isNotBlank() == true) {
                            trackUIDetail.artistName
                        } else {
                            getString(R.string.unknown)
                        }

                        currentTrackLabel = "$trackName - $trackArtists"
                    }
                }
            }?.setErrorCallback { throwable ->
                LogUtil.d(TAG, "playCurrentTrackUri error: ${throwable.message}")
            }
    }

    override fun onDestroy() {
        // Stop playing track onDestroy
        LogUtil.d(TAG, "TrackService, onDestroy: pausing playback...")
        mSpotifyAppRemote?.playerApi?.pause()

        super.onDestroy()
    }

    companion object {
        // Shared prefs
        private lateinit var securePreferencesRepository: SecurePreferencesRepository
        fun initSecurePrefs(repo: SecurePreferencesRepository) {
            securePreferencesRepository = repo
        }

        // App remote related
        var mSpotifyAppRemote: SpotifyAppRemote? = null

        // UI
        private val _isPlayingSF = MutableStateFlow(false)
        val isPlayingSF: StateFlow<Boolean> = _isPlayingSF.asStateFlow()
        var isPlaying: Boolean
            get() = _isPlayingSF.value
            set(value) {
                _isPlayingSF.value = value
            }

        // Label: <trackName> - <trackArtists>
        private val _currentTrackLabelSF = MutableStateFlow("")
        val currentTrackLabelSF: StateFlow<String> = _currentTrackLabelSF.asStateFlow()
        var currentTrackLabel: String
            get() = _currentTrackLabelSF.value
            set(value) {
                _currentTrackLabelSF.value = value
            }

        var previousTrackUri: String? = null

        private val _currentTrackUriSF = MutableStateFlow<String?>(null)
        val currentTrackUriSF: StateFlow<String?> = _currentTrackUriSF.asStateFlow()
        var currentTrackUri: String?
            get() = _currentTrackUriSF.value
            set(value) {
                _currentTrackUriSF.value = value
            }

        // Debounce for playState subscription
        var hasDetectedTrackEnd = false
    }
}