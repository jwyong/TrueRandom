package com.truerandom.repository

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.core.content.edit
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys
import com.truerandom.ui.TAG
import javax.inject.Inject
import javax.inject.Singleton

private const val SECURE_PREFS_FILE_NAME = "TRUERANDOM.SP"

private const val PREFS_KEY_ACCESS_TOKEN = "PREFS_KEY_ACCESS_TOKEN"
private const val PREFS_KEY_ACCESS_TOKEN_EXPIRE_TIMESTAMP = "PREFS_KEY_ACCESS_TOKEN_EXPIRE_TIMESTAMP"

@Singleton
class SecurePreferencesRepository @Inject constructor(context: Context) {
    private val prefs: SharedPreferences by lazy {
        try {
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                SECURE_PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        } catch (e: Exception) {
            Log.e(
                TAG,
                "Failed to initialize secure preferences, deleting old corrupt files.",
                e
            )
            // Optionally: delete corrupted prefs file and retry
            context.deleteSharedPreferences(SECURE_PREFS_FILE_NAME)

            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                SECURE_PREFS_FILE_NAME,
                masterKeyAlias,
                context,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            )
        }
    }

    /**
     * Auth related
     **/
    // Access token
    fun saveAccessToken(accessToken: String) {
        prefs.edit() { putString(PREFS_KEY_ACCESS_TOKEN, accessToken) }
    }

    fun getAccessToken(): String? {
        return prefs.getString(PREFS_KEY_ACCESS_TOKEN, null)
    }

    // Timestamp in long for expiration of access token
    fun saveAccessTokenExpireTimestamp(timestamp: Long) {
        prefs.edit() { putLong(PREFS_KEY_ACCESS_TOKEN_EXPIRE_TIMESTAMP, timestamp) }
    }

    fun getAccessTokenExpireTimestamp(): Long {
        return prefs.getLong(PREFS_KEY_ACCESS_TOKEN_EXPIRE_TIMESTAMP, 0L)
    }
}
