package com.example.data

import android.content.Context
import android.util.Log
import com.example.model.FirebaseProjectConfig
import com.google.firebase.FirebaseApp
import com.google.firebase.FirebaseOptions
import org.json.JSONObject

class FirebaseConfigManager(private val context: Context) {
  private val prefs = context.getSharedPreferences("ceo_pulse_firebase_config", Context.MODE_PRIVATE)

  fun getSavedConfig(): FirebaseProjectConfig {
    return FirebaseProjectConfig(
      apiKey = prefs.getString("apiKey", "") ?: "",
      projectId = prefs.getString("projectId", "") ?: "",
      applicationId = prefs.getString("applicationId", "") ?: "",
      storageBucket = prefs.getString("storageBucket", "") ?: "",
      authDomain = prefs.getString("authDomain", "") ?: ""
    )
  }

  fun saveConfig(config: FirebaseProjectConfig): Boolean {
    prefs.edit()
      .putString("apiKey", config.apiKey.trim())
      .putString("projectId", config.projectId.trim())
      .putString("applicationId", config.applicationId.trim())
      .putString("storageBucket", config.storageBucket.trim())
      .putString("authDomain", config.authDomain.trim())
      .apply()

    return initializeFirebase(config)
  }

  fun parseAndSavePastedConfig(rawText: String): Pair<Boolean, String> {
    try {
      val trimmed = rawText.trim()
      // Check if it's JSON or javascript object syntax
      val jsonString = if (trimmed.startsWith("{") && trimmed.endsWith("}")) {
        trimmed
      } else {
        // Try extracting JSON within { ... }
        val start = trimmed.indexOf('{')
        val end = trimmed.lastIndexOf('}')
        if (start != -1 && end != -1 && end > start) {
          trimmed.substring(start, end + 1)
        } else {
          ""
        }
      }

      val config: FirebaseProjectConfig = if (jsonString.isNotBlank()) {
        val jsonObj = JSONObject(jsonString)
        FirebaseProjectConfig(
          apiKey = jsonObj.optString("apiKey", jsonObj.optString("apiKey", "")),
          projectId = jsonObj.optString("projectId", ""),
          applicationId = jsonObj.optString("appId", jsonObj.optString("applicationId", "")),
          storageBucket = jsonObj.optString("storageBucket", ""),
          authDomain = jsonObj.optString("authDomain", "")
        )
      } else {
        // Fallback: parse key-value lines
        var apiKey = ""
        var projectId = ""
        var appId = ""
        var bucket = ""
        var authDomain = ""

        trimmed.lines().forEach { line ->
          val parts = line.split(":", "=", limit = 2)
          if (parts.size == 2) {
            val key = parts[0].trim().replace("\"", "").replace("'", "")
            val value = parts[1].trim().replace("\"", "").replace("'", "").replace(",", "")
            when (key) {
              "apiKey" -> apiKey = value
              "projectId" -> projectId = value
              "appId", "applicationId" -> appId = value
              "storageBucket" -> bucket = value
              "authDomain" -> authDomain = value
            }
          }
        }
        FirebaseProjectConfig(
          apiKey = apiKey,
          projectId = projectId,
          applicationId = appId,
          storageBucket = bucket,
          authDomain = authDomain
        )
      }

      if (!config.isConfigured) {
        return Pair(false, "Incomplete config: Ensure apiKey, projectId, and appId are present.")
      }

      val success = saveConfig(config)
      return if (success) {
        Pair(true, "Firebase configuration saved & initialized successfully.")
      } else {
        Pair(false, "Could not initialize Firebase with provided credentials.")
      }
    } catch (e: Exception) {
      Log.e("FirebaseConfigManager", "Error parsing pasted config", e)
      return Pair(false, "Invalid format: ${e.localizedMessage}")
    }
  }

  fun initializeFirebase(config: FirebaseProjectConfig): Boolean {
    return try {
      if (!config.isConfigured) return false

      val options = FirebaseOptions.Builder()
        .setApiKey(config.apiKey)
        .setProjectId(config.projectId)
        .setApplicationId(config.applicationId)
        .apply {
          if (config.storageBucket.isNotBlank()) {
            setStorageBucket(config.storageBucket)
          }
        }
        .build()

      // Check if default app exists or initialize
      val existingApps = FirebaseApp.getApps(context)
      if (existingApps.isEmpty()) {
        FirebaseApp.initializeApp(context, options)
      } else {
        val defaultApp = existingApps.find { it.name == FirebaseApp.DEFAULT_APP_NAME }
        if (defaultApp != null) {
          defaultApp.delete()
        }
        FirebaseApp.initializeApp(context, options)
      }
      true
    } catch (e: Exception) {
      Log.e("FirebaseConfigManager", "Failed to initialize FirebaseApp", e)
      false
    }
  }

  fun isConfigured(): Boolean {
    if (getSavedConfig().isConfigured) return true
    return try {
      FirebaseApp.getApps(context).isNotEmpty()
    } catch (e: Exception) {
      false
    }
  }
}
