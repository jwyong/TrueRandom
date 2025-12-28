package com.truerandom.ui

import StandardPermissionsUtil
import android.app.Activity
import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import android.view.View
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import com.spotify.sdk.android.auth.AuthorizationClient
import com.truerandom.R
import com.truerandom.databinding.ActivityMainBinding
import com.truerandom.service.TrackService
import com.truerandom.util.EventsUtil
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

const val TAG = "JAY_LOG"
@AndroidEntryPoint
class MainActivity : AppCompatActivity(), StandardPermissionsUtil.StandardPermissionCallback {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainViewModel by viewModels()

    private lateinit var adapter: LikedTracksAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        bindUi()
        collectFlowEvents()

        // Get required perms
        StandardPermissionsUtil.requestStandardPermissions(this)

        // Start check auth + appRemote
        viewModel.checkAndFetchLikedTracks(this)
        viewModel.attemptAppRemoteConnect(this)
    }

    private fun bindUi() {
        with(binding) {
            viewModel.isLoadingLD.observe(this@MainActivity) { isLoading ->
                progressHorizontal.visibility =  if (isLoading) View.VISIBLE else View.GONE

                // Disable btns when still syncing
                btnPrev.isEnabled = !isLoading
                btnPlayPause.isEnabled = !isLoading
                btnNext.isEnabled = !isLoading
            }

            adapter = LikedTracksAdapter()
            rvLikedTracks.adapter = adapter

            btnPlayPause.setOnClickListener {
                EventsUtil.sendPlayPauseButtonEvent()
            }

            btnPrev.setOnClickListener {
                EventsUtil.sendPrevNextButtonEvent(false)
            }

            btnNext.setOnClickListener {
                EventsUtil.sendPrevNextButtonEvent(true)
            }
        }
    }

    private fun collectFlowEvents() {
        lifecycleScope.launch {
            Log.d(TAG, "MainActivity, collectFlowEvents: 1")
            combine(
                TrackService.isPlayingSF, TrackService.currentTrackLabelSF
            ) { isPlaying, currentTrackLabel ->
                binding.btnPlayPause.text = if (isPlaying) {
                    getString(R.string.notification_pause)
                } else {
                    getString(R.string.notification_play)
                }

                binding.tvTrack.text = currentTrackLabel
                Unit
            }.collect { }
        }

        lifecycleScope.launch {
            Log.d(TAG, "MainActivity, collectFlowEvents: before likedTracks collect")
            viewModel.likedTracksPagedFlow.collect { pagingData ->
                Log.d(TAG, "MainActivity, collectFlowEvents: pagingData = $pagingData")
                adapter.submitData(pagingData)
            }
        }
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        Log.d(
            TAG,
            "MainActivity, onActivityResult: requestCode = $requestCode, resultCode = $resultCode, data = $data"
        )

        if (requestCode == AUTH_REQUEST_CODE && resultCode == Activity.RESULT_OK) {
            val response = AuthorizationClient.getResponse(resultCode, data)

            Log.d(TAG, "MainActivity, onActivityResult: response = $response")
            viewModel.onAuthActivityResult(response)
        }
    }

    override fun onStandardPermissionsGranted(requestCode: Int) {
        Log.d("JAY_LOG", "MainActivity, onStandardPermissionsGranted: requestCode = $requestCode")

        startTrackService()
    }

    override fun onStandardPermissionsDenied(requestCode: Int) {
        Log.d("JAY_LOG", "MainActivity, onStandardPermissionsDenied: requestCode = $requestCode")
    }

    private fun startTrackService() {
        Log.d("JAY_LOG", "MainActivity, startTrackService: ")

        val serviceIntent = Intent(this, TrackService::class.java)
        ContextCompat.startForegroundService(this, serviceIntent)
    }
}