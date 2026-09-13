package com.example.security

import android.content.Context
import android.util.Log
import com.example.BuildConfig
import com.example.crashlytics.CrashlyticsManager
import com.google.android.gms.tasks.Task
import com.google.firebase.FirebaseApp
import com.google.firebase.appcheck.AppCheckToken
import com.google.firebase.appcheck.FirebaseAppCheck
import com.google.firebase.appcheck.debug.DebugAppCheckProviderFactory
import com.google.firebase.appcheck.playintegrity.PlayIntegrityAppCheckProviderFactory
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.concurrent.atomic.AtomicBoolean
import kotlin.coroutines.resume

/**
 * Manages Firebase App Check attestation using Google Play Integrity (production)
 * and Debug Provider (development/testing) to safeguard backend endpoints and
 * Google Mobile Ads (AdMob) from unauthorized traffic, bots, and tampered APKs.
 */
object AppCheckManager {

    private const val TAG = "AppCheckManager"
    const val APP_CHECK_HEADER = "X-Firebase-AppCheck"

    data class AppCheckStatus(
        val isInitialized: Boolean = false,
        val providerName: String = "Uninitialized",
        val isTokenValid: Boolean = false,
        val currentTokenMask: String = "None",
        val tokenExpiryDate: String = "N/A",
        val lastAttestationStatus: String = "Pending initialization",
        val isAdMobProtected: Boolean = false,
        val isBackendProtected: Boolean = false
    )

    private val isInitialized = AtomicBoolean(false)

    private val _status = MutableStateFlow(AppCheckStatus())
    val status: StateFlow<AppCheckStatus> = _status.asStateFlow()

    @Volatile
    private var cachedTokenString: String? = null

    /**
     * Initializes Firebase App Check with the appropriate provider factory.
     * Must be called at app startup before AdMob or network operations execute.
     */
    fun initialize(context: Context) {
        if (isInitialized.getAndSet(true)) {
            Log.d(TAG, "AppCheckManager already initialized.")
            return
        }

        try {
            // Ensure Firebase Core App is initialized
            if (FirebaseApp.getApps(context).isEmpty()) {
                FirebaseApp.initializeApp(context)
            }

            val appCheck = FirebaseAppCheck.getInstance()

            val providerName: String
            if (BuildConfig.DEBUG) {
                // Development & Test environments: Use Debug provider
                providerName = "Play Integrity Debug Provider"
                appCheck.installAppCheckProviderFactory(
                    DebugAppCheckProviderFactory.getInstance()
                )
                Log.i(TAG, "Installed DebugAppCheckProviderFactory for development build.")
            } else {
                // Production: Use Play Integrity to verify device integrity, app authenticity, and licensing
                providerName = "Google Play Integrity"
                appCheck.installAppCheckProviderFactory(
                    PlayIntegrityAppCheckProviderFactory.getInstance()
                )
                Log.i(TAG, "Installed PlayIntegrityAppCheckProviderFactory for production build.")
            }

            // Enable automatic token refresh to ensure requests never stall
            appCheck.setTokenAutoRefreshEnabled(true)

            // Register listener for token updates
            appCheck.addAppCheckListener { token: AppCheckToken ->
                handleTokenReceived(token, providerName)
            }

            // Immediately request an initial token to warm up attestation cache
            appCheck.getAppCheckToken(false)
                .addOnSuccessListener { token ->
                    handleTokenReceived(token, providerName)
                    Log.i(TAG, "Initial App Check token acquired successfully.")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Initial App Check token fetch encountered warning: ${e.message}")
                    _status.value = _status.value.copy(
                        isInitialized = true,
                        providerName = providerName,
                        lastAttestationStatus = "Attestation pending: ${e.localizedMessage ?: "Unknown"}",
                        isAdMobProtected = true,
                        isBackendProtected = true
                    )
                    CrashlyticsManager.setCustomKey("app_check_status", "pending_attestation")
                }

            _status.value = _status.value.copy(
                isInitialized = true,
                providerName = providerName,
                lastAttestationStatus = "Attestation provider registered",
                isAdMobProtected = true,
                isBackendProtected = true
            )

            CrashlyticsManager.setCustomKey("app_check_provider", providerName)
            CrashlyticsManager.setCustomKey("app_check_active", true)
            CrashlyticsManager.log("Firebase App Check initialized with provider: $providerName")

        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Firebase App Check", e)
            CrashlyticsManager.recordException(e, "AppCheckInitialization")
            _status.value = _status.value.copy(
                isInitialized = false,
                lastAttestationStatus = "Error: ${e.localizedMessage}"
            )
        }
    }

    private fun handleTokenReceived(token: AppCheckToken, providerName: String) {
        val rawToken = token.token
        cachedTokenString = rawToken

        val mask = if (rawToken.length > 12) {
            "${rawToken.take(6)}...${rawToken.takeLast(6)}"
        } else {
            "Valid Token"
        }

        val expiryString = try {
            val date = Date(token.expireTimeMillis)
            val sdf = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault())
            sdf.format(date)
        } catch (e: Exception) {
            "Active"
        }

        _status.value = AppCheckStatus(
            isInitialized = true,
            providerName = providerName,
            isTokenValid = rawToken.isNotBlank(),
            currentTokenMask = mask,
            tokenExpiryDate = expiryString,
            lastAttestationStatus = "Verified & Active",
            isAdMobProtected = true,
            isBackendProtected = true
        )

        CrashlyticsManager.setCustomKey("app_check_token_valid", true)
    }

    /**
     * Retrieves the current App Check token asynchronously, optionally forcing a refresh.
     * Returns null if attestation fails or token is unavailable.
     */
    suspend fun getAppCheckToken(forceRefresh: Boolean = false): String? = withContext(Dispatchers.IO) {
        if (!forceRefresh && !cachedTokenString.isNullOrBlank()) {
            return@withContext cachedTokenString
        }

        try {
            val appCheck = FirebaseAppCheck.getInstance()
            val tokenResult: AppCheckToken = suspendCancellableCoroutine { continuation ->
                val task: Task<AppCheckToken> = appCheck.getAppCheckToken(forceRefresh)
                task.addOnSuccessListener { appCheckToken ->
                    continuation.resume(appCheckToken)
                }.addOnFailureListener { exc ->
                    Log.w(TAG, "Failed to fetch App Check token: ${exc.message}")
                    CrashlyticsManager.recordException(exc, "AppCheckTokenFetch")
                    continuation.resume(object : AppCheckToken() {
                        override fun getToken(): String = ""
                        override fun getExpireTimeMillis(): Long = 0L
                    })
                }
            }

            if (tokenResult.token.isNotBlank()) {
                cachedTokenString = tokenResult.token
                tokenResult.token
            } else {
                cachedTokenString
            }
        } catch (e: Exception) {
            Log.w(TAG, "Error fetching App Check token", e)
            cachedTokenString
        }
    }

    /**
     * Triggers a manual re-attestation of the App Check token (e.g. from Diagnostics UI).
     */
    fun refreshAppCheckToken(onResult: ((Boolean, String) -> Unit)? = null) {
        try {
            FirebaseAppCheck.getInstance().getAppCheckToken(true)
                .addOnSuccessListener { token ->
                    handleTokenReceived(token, _status.value.providerName)
                    onResult?.invoke(true, "App Check token refreshed and verified successfully")
                }
                .addOnFailureListener { e ->
                    Log.w(TAG, "Manual token refresh failed: ${e.message}")
                    onResult?.invoke(false, e.localizedMessage ?: "Attestation refresh failed")
                }
        } catch (e: Exception) {
            onResult?.invoke(false, e.localizedMessage ?: "App Check not initialized")
        }
    }
}
