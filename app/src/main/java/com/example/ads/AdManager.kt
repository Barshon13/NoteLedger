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
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * Standard Google AdMob test unit IDs as per official AdMob documentation:
 * https://developers.google.com/admob/android/test-ads
 */
object AdConstants {
    const val TEST_BANNER_AD_UNIT_ID = "ca-app-pub-3940256099942544/6300978111"
    const val TEST_INTERSTITIAL_AD_UNIT_ID = "ca-app-pub-3940256099942544/1033173712"
    const val TEST_REWARDED_AD_UNIT_ID = "ca-app-pub-3940256099942544/5224354917"
}

object AdManager {
    private const val TAG = "AdManager"

    private val isMobileAdsInitialized = AtomicBoolean(false)
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading = false
    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading = false

    private var currentConfig = RemoteAdsConfig()
    private var actionCount = 0

    private val _isInitialized = MutableStateFlow(false)
    val isInitialized: StateFlow<Boolean> = _isInitialized.asStateFlow()

    private val _activeConfig = MutableStateFlow(RemoteAdsConfig())
    val activeConfig: StateFlow<RemoteAdsConfig> = _activeConfig.asStateFlow()

    fun initialize(context: Context) {
        if (isMobileAdsInitialized.getAndSet(true)) return

        try {
            MobileAds.initialize(context) { status ->
                Log.d(TAG, "AdMob MobileAds initialized: $status")
                _isInitialized.value = true
                if (currentConfig.adsEnabled && currentConfig.interstitialAdEnabled) {
                    loadInterstitialAd(context)
                }
                if (currentConfig.adsEnabled && currentConfig.rewardedAdEnabled) {
                    loadRewardedAd(context)
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize AdMob MobileAds", e)
        }
    }

    fun updateConfig(config: RemoteAdsConfig) {
        val oldConfig = currentConfig
        currentConfig = config
        _activeConfig.value = config

        // If ad unit IDs changed, discard old loaded ad to ensure new ad unit is fetched
        if (oldConfig.interstitialAdUnitId != config.interstitialAdUnitId) {
            interstitialAd = null
        }
        if (oldConfig.rewardedAdUnitId != config.rewardedAdUnitId) {
            rewardedAd = null
        }
    }

    fun loadInterstitialAd(context: Context) {
        if (!currentConfig.adsEnabled || !currentConfig.interstitialAdEnabled) {
            Log.d(TAG, "Interstitial ads are currently disabled in remote config.")
            return
        }

        if (interstitialAd != null || isInterstitialLoading) {
            return
        }

        isInterstitialLoading = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = currentConfig.interstitialAdUnitId.ifBlank { AdConstants.TEST_INTERSTITIAL_AD_UNIT_ID }
        Log.d(TAG, "Requesting AdMob Interstitial ad with unit ID: $adUnitId")

        InterstitialAd.load(
            context,
            adUnitId,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    isInterstitialLoading = false
                    Log.d(TAG, "Interstitial ad loaded successfully and is ready.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Interstitial ad failed to load (code ${loadAdError.code}): ${loadAdError.message}")
                    interstitialAd = null
                    isInterstitialLoading = false
                }
            }
        )
    }

    fun showInterstitialAd(activity: Activity, onAdClosed: (() -> Unit)? = null): Boolean {
        if (!currentConfig.adsEnabled || !currentConfig.interstitialAdEnabled) {
            onAdClosed?.invoke()
            return false
        }

        val currentAd = interstitialAd
        if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad dismissed by user.")
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdClosed?.invoke()
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    Log.w(TAG, "Interstitial ad failed to show: ${adError.message}")
                    interstitialAd = null
                    loadInterstitialAd(activity)
                    onAdClosed?.invoke()
                }

                override fun onAdShowedFullScreenContent() {
                    Log.d(TAG, "Interstitial ad is now displaying on screen.")
                }
            }
            currentAd.show(activity)
            return true
        } else {
            Log.d(TAG, "Interstitial ad not ready yet. Preloading for next trigger...")
            loadInterstitialAd(activity)
            onAdClosed?.invoke()
            return false
        }
    }

    /**
     * Attempts to show an interstitial ad from any Composable Context.
     * @param force If true, attempts to show immediately if loaded. If false, shows after a threshold of user actions.
     */
    fun tryShowInterstitial(context: Context, force: Boolean = true, onComplete: (() -> Unit)? = null) {
        if (!currentConfig.adsEnabled || !currentConfig.interstitialAdEnabled) {
            onComplete?.invoke()
            return
        }

        val activity = context.findActivity()
        if (activity == null) {
            onComplete?.invoke()
            return
        }

        val interval = currentConfig.interstitialIntervalActions.coerceAtLeast(1)
        if (!force) {
            actionCount++
            if (actionCount < interval) {
                onComplete?.invoke()
                return
            }
            actionCount = 0
        }

        showInterstitialAd(activity, onComplete)
    }

    fun loadRewardedAd(context: Context) {
        if (!currentConfig.adsEnabled || !currentConfig.rewardedAdEnabled) {
            return
        }

        if (rewardedAd != null || isRewardedLoading) {
            return
        }

        isRewardedLoading = true
        val adRequest = AdRequest.Builder().build()
        val adUnitId = currentConfig.rewardedAdUnitId.ifBlank { AdConstants.TEST_REWARDED_AD_UNIT_ID }
        RewardedAd.load(
            context,
            adUnitId,
            adRequest,
            object : RewardedAdLoadCallback() {
                override fun onAdLoaded(ad: RewardedAd) {
                    rewardedAd = ad
                    isRewardedLoading = false
                    Log.d(TAG, "Rewarded ad loaded successfully.")
                }

                override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                    Log.w(TAG, "Rewarded ad failed to load: ${loadAdError.message}")
                    rewardedAd = null
                    isRewardedLoading = false
                }
            }
        )
    }

    fun showRewardedAd(activity: Activity, onUserEarnedReward: (Int, String) -> Unit): Boolean {
        if (!currentConfig.adsEnabled || !currentConfig.rewardedAdEnabled) {
            return false
        }

        val currentAd = rewardedAd
        return if (currentAd != null) {
            currentAd.fullScreenContentCallback = object : FullScreenContentCallback() {
                override fun onAdDismissedFullScreenContent() {
                    rewardedAd = null
                    loadRewardedAd(activity)
                }

                override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                    rewardedAd = null
                    loadRewardedAd(activity)
                }
            }
            currentAd.show(activity) { rewardItem ->
                onUserEarnedReward(rewardItem.amount, rewardItem.type)
            }
            true
        } else {
            loadRewardedAd(activity)
            false
        }
    }

    private fun Context.findActivity(): Activity? {
        var currentContext = this
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) {
                return currentContext
            }
            currentContext = currentContext.baseContext
        }
        return null
    }
}
