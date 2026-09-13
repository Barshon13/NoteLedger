package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.os.Handler
import android.os.Looper
import android.util.Log
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdManager provides a singleton bridge for Google Mobile Ads (AdMob).
 *
 * Ensures interstitial ads are preloaded immediately, auto-reloaded upon dismissal or failure,
 * and if an interstitial is requested while still loading, it waits briefly (up to timeout)
 * so users actually see the ad when adding/deleting items, while never permanently blocking actions.
 */
object AdManager {
    private const val TAG = "AdManager"

    private var appContext: Context? = null
    private var currentConfig: RemoteAdsConfig = RemoteAdsConfig()

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())

    /**
     * Initializes Google Mobile Ads SDK.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext

        if (_isInitialized.value) {
            loadInterstitialAd()
            return
        }

        Log.d(TAG, "Initializing Google Mobile Ads (AdMob)...")
        MobileAds.initialize(context.applicationContext) { status ->
            _isInitialized.value = true
            Log.d(TAG, "MobileAds initialized: $status")
            loadInterstitialAd()
        }
    }

    /**
     * Updates dynamic configuration from remote source or local override.
     */
    fun updateConfig(newConfig: RemoteAdsConfig) {
        currentConfig = newConfig
        Log.d(TAG, "Config updated: adsEnabled=${newConfig.adsEnabled}, interstitial=${newConfig.effectiveInterstitialAdUnitId}")
        if (newConfig.adsEnabled && newConfig.interstitialAdEnabled) {
            if (interstitialAd == null && !isInterstitialLoading) {
                loadInterstitialAd()
            }
        }
    }

    /**
     * Pre-loads an interstitial ad using the applicationContext to eliminate activity memory leaks.
     */
    fun loadInterstitialAd() {
        val ctx = appContext ?: return
        if (!currentConfig.adsEnabled || !currentConfig.interstitialAdEnabled) {
            Log.d(TAG, "Interstitial ads disabled in config, skipping load.")
            return
        }

        if (interstitialAd != null || isInterstitialLoading) {
            Log.d(TAG, "Interstitial ad already cached or currently loading.")
            return
        }

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = currentConfig.effectiveInterstitialAdUnitId
        Log.d(TAG, "Pre-loading AdMob Interstitial Ad with unitId: $adUnitId")

        mainHandler.post {
            InterstitialAd.load(
                ctx,
                adUnitId,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        Log.d(TAG, "AdMob Interstitial loaded successfully.")
                        interstitialAd = ad
                        isInterstitialLoading = false
                        _isInterstitialReady.value = true
                    }

                    override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                        Log.w(TAG, "AdMob Interstitial failed to load: ${loadAdError.message} (Code: ${loadAdError.code})")
                        interstitialAd = null
                        isInterstitialLoading = false
                        _isInterstitialReady.value = false
                        // Retry after short delay
                        mainHandler.postDelayed({ loadInterstitialAd() }, 4000)
                    }
                }
            )
        }
    }

    /**
     * Displays the interstitial ad on the specified Activity.
     * If the ad is already loaded, shows it immediately.
     * If it is currently loading, waits up to 1.8 seconds for it to finish loading
     * before falling back, ensuring ads show reliably on add/delete actions.
     * Guarantees [onAdDismissed] will be executed exactly once.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        val hasInvokedCallback = AtomicBoolean(false)
        fun safeDismiss() {
            if (hasInvokedCallback.compareAndSet(false, true)) {
                mainHandler.post { onAdDismissed() }
            }
        }

        if (!currentConfig.adsEnabled || !currentConfig.interstitialAdEnabled) {
            Log.d(TAG, "Ads or Interstitial disabled in config. Proceeding.")
            safeDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            showLoadedAd(activity, ad, ::safeDismiss)
            return
        }

        // If not loaded but currently loading, wait up to 1.8s for it to finish loading
        if (isInterstitialLoading) {
            Log.d(TAG, "Interstitial is currently loading, waiting briefly before dismissing...")
            var checksRemaining = 9 // 9 * 200ms = 1.8s
            fun checkAd() {
                val currentAd = interstitialAd
                if (currentAd != null && !activity.isFinishing && !activity.isDestroyed) {
                    showLoadedAd(activity, currentAd, ::safeDismiss)
                } else if (checksRemaining > 0 && isInterstitialLoading && !hasInvokedCallback.get()) {
                    checksRemaining--
                    mainHandler.postDelayed(::checkAd, 200)
                } else {
                    Log.d(TAG, "Wait timed out or load finished without ad. Proceeding.")
                    safeDismiss()
                }
            }
            mainHandler.postDelayed(::checkAd, 200)
            return
        }

        // Neither loaded nor loading, trigger load and proceed
        Log.d(TAG, "Interstitial ad not ready and not loading. Triggering load.")
        loadInterstitialAd()
        safeDismiss()
    }

    private fun showLoadedAd(activity: Activity, ad: InterstitialAd, onDismissed: () -> Unit) {
        interstitialAd = null
        _isInterstitialReady.value = false

        ad.fullScreenContentCallback = object : FullScreenContentCallback() {
            override fun onAdDismissedFullScreenContent() {
                Log.d(TAG, "AdMob Interstitial dismissed.")
                loadInterstitialAd()
                onDismissed()
            }

            override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                Log.w(TAG, "AdMob Interstitial failed to show: ${adError.message}")
                loadInterstitialAd()
                onDismissed()
            }

            override fun onAdShowedFullScreenContent() {
                Log.d(TAG, "AdMob Interstitial showed full screen.")
            }
        }

        mainHandler.post {
            try {
                if (!activity.isFinishing && !activity.isDestroyed) {
                    ad.show(activity)
                } else {
                    onDismissed()
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error displaying interstitial ad: ${e.message}", e)
                onDismissed()
            }
        }
    }

    /**
     * Helper to show interstitial ad by extracting Activity from Context.
     */
    fun showInterstitial(context: Context, onAdDismissed: () -> Unit) {
        val activity = findActivity(context)
        if (activity != null) {
            showInterstitialAd(activity, onAdDismissed)
        } else {
            Log.w(TAG, "Activity not found in context. Proceeding immediately.")
            onAdDismissed()
        }
    }

    /**
     * Backward-compatible alias for existing call sites.
     */
    fun tryShowInterstitial(context: Context, force: Boolean = false, onDismissed: () -> Unit = {}) {
        showInterstitial(context, onDismissed)
    }

    /**
     * Resolves the current Activity from any given Context.
     */
    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}
