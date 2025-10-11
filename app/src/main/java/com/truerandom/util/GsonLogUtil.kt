package com.truerandom.util// GsonLogUtil.kt

import android.content.Intent
import android.os.Bundle
import android.util.Log
import com.google.gson.Gson
import com.google.gson.GsonBuilder

/**
 * Utility for logging complex Android objects (Intent, Bundle) as formatted JSON.
 */
object GsonLogUtil {
    private val gson: Gson = GsonBuilder().setPrettyPrinting().create()

    /**
     * Converts a Bundle to a JSON string.
     */
    private fun bundleToJson(bundle: Bundle?): String {
        if (bundle == null) {
            return "{}"
        }

        // Convert the Bundle to a Map<String, Any> for proper JSON serialization
        val map = mutableMapOf<String, Any?>()
        val keySet = try { bundle.keySet() } catch (e: Exception) { return "{}" }

        for (key in keySet) {
            val value = bundle.get(key)
            // Store a readable string representation for complex objects
            map[key] = value?.toString() ?: "null"
        }
        return gson.toJson(map)
    }

    /**
     * Logs the contents of an Intent in a detailed, JSON-formatted string.
     */
    fun logIntentDetailsJson(tag: String, message: String, intent: Intent?) {
        if (intent == null) {
            Log.d(tag, "$message Intent is null")
            return
        }

        // Build a Map containing the Intent's key properties
        val intentMap = mutableMapOf<String, Any?>()
        intentMap["Action"] = intent.action
        intentMap["Data_URI"] = intent.dataString
        intentMap["Flags_Hex"] = "0x${Integer.toHexString(intent.flags)}"
        intentMap["Component"] = intent.component?.toShortString()
        
        // Add the Bundle extras as a sub-object (requires separate serialization)
        intentMap["Extras"] = bundleToJson(intent.extras)

        val logMessage = StringBuilder()
        logMessage.append("\n============================================\n")
        logMessage.append("Intent Details: $message\n")
        logMessage.append("--------------------------------------------\n")
        logMessage.append(gson.toJson(intentMap))
        logMessage.append("\n============================================\n")

        Log.d(tag, logMessage.toString())
    }
}