package com.truerandom

import android.app.Application
import com.truerandom.repository.SecurePreferencesRepository
import com.truerandom.service.TrackService
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TrueRandom : Application() {
    @Inject
    lateinit var securePreferencesRepository: SecurePreferencesRepository

    override fun onCreate() {
        super.onCreate()

        TrackService.initSecurePrefs(securePreferencesRepository)
    }
}