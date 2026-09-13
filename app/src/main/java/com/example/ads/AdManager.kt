package com.example.ads

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
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

/**
 * AdManager provides a singleton bridge for Google Mobile Ads (AdMob).
 *
 * Pre-loads interstitial ads using applicationContext to prevent memory leaks,
 * and guarantees that onAdDismissed fallback is always called so user workflows
 * are never blocked even when ads are loading or unavailable.
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

    /**
     * Initializes Google Mobile Ads SDK.
     */
    fun initialize(context: Context) {
        if (_isInitialized.value) return
        appContext = context.applicationContext

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
        Log.d(TAG, "Remote config updated: adsEnabled=${newConfig.adsEnabled}, interstitial=${newConfig.effectiveInterstitialAdUnitId}")
        if (newConfig.adsEnabled && newConfig.interstitialAdEnabled && interstitialAd == null && !isInterstitialLoading) {
            loadInterstitialAd()
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
                }
            }
        )
    }

    /**
     * Displays the interstitial ad on the specified Activity.
     * Guarantees [onAdDismissed] will be executed either after ad completion
     * or immediately if the ad is not ready / fails to display.
     * Automatically requests and pre-loads the next interstitial after display.
     */
    fun showInterstitialAd(activity: Activity, onAdDismissed: () -> Unit) {
        if (!currentConfig.adsEnabled || !currentConfig.interstitialAdEnabled) {
            Log.d(TAG, "Ads or Interstitial disabled in config. Proceeding immediately.")
            onAdDismissed()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            Log.d(TAG, "Showing AdMob Interstitial ad...")
            ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "AdMob Interstitial dismissed.")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "AdMob Interstitial failed to show: ${adError.message}")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                    loadInterstitialAd()
                    onAdDismissed()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "AdMob Interstitial showed full screen.")
                    interstitialAd = null
                    _isInterstitialReady.value = false
                }
            }
            ad.show(activity)
        } else {
            Log.d(TAG, "Interstitial ad not ready. Invoking callback immediately and requesting pre-load.")
            loadInterstitialAd()
            onAdDismissed()
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
