package com.rubberdingyrapids.baking.data

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Where recipes get published when sharing a link. Defaults to JsonBin; can point at your own server. */
data class CloudSettings(val baseUrl: String, val apiKey: String) {
    val isConfigured: Boolean get() = baseUrl.isNotBlank()
}

class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    private val _cloud = MutableStateFlow(
        CloudSettings(
            baseUrl = prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL,
            apiKey = prefs.getString(KEY_API_KEY, "") ?: "",
        ),
    )
    val cloud: StateFlow<CloudSettings> = _cloud.asStateFlow()

    fun updateCloud(baseUrl: String, apiKey: String) {
        val next = CloudSettings(baseUrl.trim().trimEnd('/'), apiKey.trim())
        _cloud.value = next
        prefs.edit().putString(KEY_BASE_URL, next.baseUrl).putString(KEY_API_KEY, next.apiKey).apply()
    }

    companion object {
        const val DEFAULT_BASE_URL = "https://api.jsonbin.io/v3/b"
        private const val KEY_BASE_URL = "cloud_base_url"
        private const val KEY_API_KEY = "cloud_api_key"
    }
}
