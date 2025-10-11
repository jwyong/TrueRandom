package com.truerandom

import StandardPermissionsUtil
import android.app.ComponentCaller
import android.content.Intent
import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.core.content.ContextCompat
import com.spotify.sdk.android.auth.AuthorizationClient
import com.truerandom.databinding.ActivityMainBinding
import com.truerandom.service.TrackService

const val TAG = "JAY_LOG"
class MainActivity : ComponentActivity(), StandardPermissionsUtil.StandardPermissionCallback {
    private lateinit var binding: ActivityMainBinding
    private val viewModel: MainActivityViewModel by viewModels()

    private lateinit var spotifyAuthLauncher: ActivityResultLauncher<Intent>

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        // Register the Spotify login launcher
        spotifyAuthLauncher = registerForActivityResult(
            ActivityResultContracts.StartActivityForResult()
        ) { result ->
//            Log.d("JAY_LOG", "MainActivity, onCreate: result = $result")
//
//            viewModel.onRequestAuthResult(result)
        }

        bindUi()

        StandardPermissionsUtil.requestStandardPermissions(this)
    }

    private fun bindUi() {
        with(binding) {
            bthAuth.setOnClickListener {
                viewModel.createSpotifyAuthorizationIntent(this@MainActivity)
//                val intent = viewModel.createSpotifyAuthorizationIntent(this@MainActivity)
//                spotifyAuthLauncher.launch(intent)
            }

            btnPlayPause.setOnClickListener {
                viewModel.attemptAppRemoteConnect(this@MainActivity)

            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)

        setIntent(intent) // Always call setIntent to update the Activity's Intent

        viewModel.onRequestAuthResult(intent)
    }

    override fun onActivityResult(
        requestCode: Int,
        resultCode: Int,
        data: Intent?,
        caller: ComponentCaller
    ) {
        super.onActivityResult(requestCode, resultCode, data, caller)

        Log.d("JAY_LOG", "MainActivity, onActivityResult: requestCode = $requestCode, " +
                "resultCode = $resultCode, data = $data, caller = $caller")

        val response = AuthorizationClient.getResponse(resultCode, data)

        Log.d("JAY_LOG", "MainActivity, onActivityResult: response = $response")

        viewModel.onAuthResult(response)
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