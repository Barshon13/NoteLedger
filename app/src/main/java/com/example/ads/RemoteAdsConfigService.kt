package com.example.ads

import android.content.Context
import android.util.Log
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * Service to fetch remote ads configuration from GitHub (or any raw JSON URL)
 * with local caching and offline fallback.
 */
object RemoteAdsConfigService {
    private const val TAG = "RemoteAdsConfigService"
    private const val PREFS_NAME = "remote_ads_config_prefs"
    private const val KEY_CONFIG_JSON = "cached_config_json"
    private const val KEY_CUSTOM_CONFIG_URL = "custom_config_url"

    // Default GitHub Raw config URL or fallback URL.
    // Points directly to the user's NoteLedger repository raw config on GitHub.
    const val DEFAULT_GITHUB_CONFIG_URL = "https://raw.githubusercontent.com/Barshon13/NoteLedger/main/ads_config.json"

    private val moshi = Moshi.Builder()
        .addLast(KotlinJsonAdapterFactory())
        .build()
    private val adapter = moshi.adapter(RemoteAdsConfig::class.java)

    private val httpClient = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val _config = MutableStateFlow(RemoteAdsConfig())
    val config: StateFlow<RemoteAdsConfig> = _config.asStateFlow()

    private val _isFetching = MutableStateFlow(false)
    val isFetching: StateFlow<Boolean> = _isFetching.asStateFlow()

    private val _lastFetchStatus = MutableStateFlow<String?>(null)
    val lastFetchStatus: StateFlow<String?> = _lastFetchStatus.asStateFlow()

    fun initialize(context: Context, customUrl: String? = null) {
        val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

        // 1. Load cached JSON if available
        val cachedJson = prefs.getString(KEY_CONFIG_JSON, null)
        if (!cachedJson.isNullOrBlank()) {
            try {
                adapter.fromJson(cachedJson)?.let {
                    _config.value = it
                    AdManager.updateConfig(it)
                    Log.d(TAG, "Loaded cached RemoteAdsConfig: $it")
                }
            } catch (e: Exception) {
                Log.w(TAG, "Failed to parse cached RemoteAdsConfig", e)
            }
        }

        // 2. Determine URL to fetch from
        val urlToFetch = customUrl
            ?: prefs.getString(KEY_CUSTOM_CONFIG_URL, null)?.takeIf { it.isNotBlank() }
            ?: DEFAULT_GITHUB_CONFIG_URL

        // 3. Fetch latest in background
        fetchRemoteConfig(context, urlToFetch)
    }

    fun fetchRemoteConfig(context: Context, url: String, onResult: ((Boolean, String) -> Unit)? = null) {
        CoroutineScope(Dispatchers.IO).launch {
            _isFetching.value = true
            try {
                Log.d(TAG, "Fetching remote ads configuration from: $url")
                val request = Request.Builder()
                    .url(url)
                    .header("Cache-Control", "no-cache")
                    .build()

                val response = httpClient.newCall(request).execute()
                if (response.isSuccessful) {
                    val bodyString = response.body?.string()
                    if (!bodyString.isNullOrBlank()) {
                        val parsedConfig = adapter.fromJson(bodyString)
                        if (parsedConfig != null) {
                            withContext(Dispatchers.Main) {
                                _config.value = parsedConfig
                                AdManager.updateConfig(parsedConfig)
                                _lastFetchStatus.value = "Synced successfully"
                            }

                            // Save to local cache
                            context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
                                .edit()
                                .putString(KEY_CONFIG_JSON, bodyString)
                                .putString(KEY_CUSTOM_CONFIG_URL, url)
                                .apply()

                            Log.d(TAG, "RemoteAdsConfig updated successfully: $parsedConfig")
                            withContext(Dispatchers.Main) {
                                onResult?.invoke(true, "Config updated successfully from GitHub")
                            }
                            return@launch
                        }
                    }
                }
                val msg = "Server returned code ${response.code}"
                Log.w(TAG, "Failed to fetch remote config: $msg")
                withContext(Dispatchers.Main) {
                    _lastFetchStatus.value = msg
                    onResult?.invoke(false, msg)
                }
            } catch (e: Exception) {
                Log.w(TAG, "Network exception while fetching remote ads config: ${e.message}")
                withContext(Dispatchers.Main) {
                    val errorMsg = e.localizedMessage ?: "Network error"
                    _lastFetchStatus.value = errorMsg
                    onResult?.invoke(false, errorMsg)
                }
            } finally {
                _isFetching.value = false
            }
        }
    }

    fun getSavedUrl(context: Context): String {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .getString(KEY_CUSTOM_CONFIG_URL, DEFAULT_GITHUB_CONFIG_URL) ?: DEFAULT_GITHUB_CONFIG_URL
    }

    fun setSavedUrl(context: Context, url: String) {
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_CUSTOM_CONFIG_URL, url)
            .apply()
    }
}
