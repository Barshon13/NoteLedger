package com.example.ads

import android.app.Activity
import android.content.Context
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.google.android.gms.ads.AdError
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.FullScreenContentCallback
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.RequestConfiguration
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback
import com.google.android.gms.ads.rewarded.RewardItem
import com.google.android.gms.ads.rewarded.RewardedAd
import com.google.android.gms.ads.rewarded.RewardedAdLoadCallback
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object AdManager {
    private const val TAG = "AdManager"

    // Official Google AdMob Test Ad Unit IDs
    const val BANNER_AD_ID = "ca-app-pub-3940256099942544/6300978111"
    const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    const val REWARDED_AD_ID = "ca-app-pub-3940256099942544/5224354917"

    private var appContext: Context? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    // Interstitial Ad State
    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading: Boolean = false
    private val _isInterstitialReady = MutableStateFlow(false)
    val isInterstitialReady: StateFlow<Boolean> = _isInterstitialReady.asStateFlow()

    // Rewarded Ad State
    private var rewardedAd: RewardedAd? = null
    private var isRewardedLoading: Boolean = false
    private val _isRewardedReady = MutableStateFlow(false)
    val isRewardedReady: StateFlow<Boolean> = _isRewardedReady.asStateFlow()

    fun initialize(context: Context) {
        val applicationContext = context.applicationContext
        appContext = applicationContext

        // Configure test device to ensure emulator / test environment always gets test ads
        val requestConfiguration = RequestConfiguration.Builder()
            .setTestDeviceIds(listOf(AdRequest.DEVICE_ID_EMULATOR))
            .build()
        MobileAds.setRequestConfiguration(requestConfiguration)

        AdDiagnosticsLogger.log(
            AdType.INTERSTITIAL,
            AdStatus.IDLE,
            "Initializing MobileAds SDK..."
        )

        MobileAds.initialize(applicationContext) { status: InitializationStatus ->
            Log.d(TAG, "AdMob SDK Initialized successfully: $status")
            AdDiagnosticsLogger.log(
                AdType.INTERSTITIAL,
                AdStatus.IDLE,
                "MobileAds SDK Initialized ($status)"
            )
            // Ensure pre-loading is scheduled on the main looper
            mainHandler.post {
                loadInterstitial()
                loadRewarded()
            }
        }
    }

    /**
     * Pre-loads an Interstitial Ad on the Main Thread.
     */
    fun loadInterstitial() {
        mainHandler.post {
            val context = appContext ?: return@post
            if (isInterstitialLoading || interstitialAd != null) {
                Log.d(TAG, "Interstitial ad already ready or currently loading")
                return@post
            }

            isInterstitialLoading = true
            AdDiagnosticsLogger.log(
                AdType.INTERSTITIAL,
                AdStatus.LOADING,
                "Requesting Interstitial ad load ($INTERSTITIAL_AD_ID)"
            )

            val adRequest = AdRequest.Builder().build()
            Log.d(TAG, "Starting Interstitial Ad load request...")

            InterstitialAd.load(
                context,
                INTERSTITIAL_AD_ID,
                adRequest,
                object : InterstitialAdLoadCallback() {
                    override fun onAdLoaded(ad: InterstitialAd) {
                        interstitialAd = ad
                        isInterstitialLoading = false
                        _isInterstitialReady.value = true
                        Log.d(TAG, "Interstitial Ad loaded and ready to display")
                        AdDiagnosticsLogger.log(
                            AdType.INTERSTITIAL,
                            AdStatus.LOADED,
                            "Interstitial Ad loaded successfully"
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        interstitialAd = null
                        isInterstitialLoading = false
                        _isInterstitialReady.value = false
                        Log.e(TAG, "Interstitial Ad failed to load: ${error.message} (code ${error.code})")
                        AdDiagnosticsLogger.log(
                            AdType.INTERSTITIAL,
                            AdStatus.FAILED_TO_LOAD,
                            error.message,
                            error.code
                        )
                        // Auto-retry after 15 seconds
                        mainHandler.postDelayed({ loadInterstitial() }, 15000)
                    }
                }
            )
        }
    }

    /**
     * Displays the pre-loaded Interstitial Ad.
     * Sets fullScreenContentCallback to null out reference immediately, run onDismiss,
     * and pre-load the next ad.
     */
    fun showInterstitial(activity: Activity, onDismiss: () -> Unit = {}) {
        mainHandler.post {
            val ad = interstitialAd
            if (ad != null) {
                AdDiagnosticsLogger.log(
                    AdType.INTERSTITIAL,
                    AdStatus.SHOWING,
                    "Calling show() on Interstitial Ad"
                )
                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Interstitial Ad dismissed by user")
                        interstitialAd = null
                        _isInterstitialReady.value = false
                        AdDiagnosticsLogger.log(
                            AdType.INTERSTITIAL,
                            AdStatus.DISMISSED,
                            "Interstitial Ad dismissed"
                        )
                        onDismiss()
                        loadInterstitial()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Interstitial Ad failed to show: ${adError.message}")
                        interstitialAd = null
                        _isInterstitialReady.value = false
                        AdDiagnosticsLogger.log(
                            AdType.INTERSTITIAL,
                            AdStatus.FAILED_TO_SHOW,
                            adError.message,
                            adError.code
                        )
                        onDismiss()
                        loadInterstitial()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Interstitial Ad presented on screen")
                        interstitialAd = null
                        _isInterstitialReady.value = false
                        AdDiagnosticsLogger.log(
                            AdType.INTERSTITIAL,
                            AdStatus.SHOWN,
                            "Interstitial Ad presented on screen"
                        )
                    }
                }
                ad.show(activity)
            } else {
                Log.w(TAG, "Interstitial Ad requested but not ready yet")
                AdDiagnosticsLogger.log(
                    AdType.INTERSTITIAL,
                    AdStatus.FAILED_TO_SHOW,
                    "Ad requested but not ready in memory (preloading triggered)"
                )
                loadInterstitial()
                onDismiss()
            }
        }
    }

    /**
     * Pre-loads a Rewarded Video Ad on the Main Thread.
     */
    fun loadRewarded() {
        mainHandler.post {
            val context = appContext ?: return@post
            if (isRewardedLoading || rewardedAd != null) {
                Log.d(TAG, "Rewarded ad already ready or currently loading")
                return@post
            }

            isRewardedLoading = true
            AdDiagnosticsLogger.log(
                AdType.REWARDED,
                AdStatus.LOADING,
                "Requesting Rewarded ad load ($REWARDED_AD_ID)"
            )

            val adRequest = AdRequest.Builder().build()
            Log.d(TAG, "Starting Rewarded Ad load request...")

            RewardedAd.load(
                context,
                REWARDED_AD_ID,
                adRequest,
                object : RewardedAdLoadCallback() {
                    override fun onAdLoaded(ad: RewardedAd) {
                        rewardedAd = ad
                        isRewardedLoading = false
                        _isRewardedReady.value = true
                        Log.d(TAG, "Rewarded Ad loaded and ready to display")
                        AdDiagnosticsLogger.log(
                            AdType.REWARDED,
                            AdStatus.LOADED,
                            "Rewarded Ad loaded successfully"
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        rewardedAd = null
                        isRewardedLoading = false
                        _isRewardedReady.value = false
                        Log.e(TAG, "Rewarded Ad failed to load: ${error.message} (code ${error.code})")
                        AdDiagnosticsLogger.log(
                            AdType.REWARDED,
                            AdStatus.FAILED_TO_LOAD,
                            error.message,
                            error.code
                        )
                        // Auto-retry after 15 seconds
                        mainHandler.postDelayed({ loadRewarded() }, 15000)
                    }
                }
            )
        }
    }

    /**
     * Displays the pre-loaded Rewarded Video Ad.
     * Invokes onUserEarnedReward only when the user legitimately earns the reward.
     */
    fun showRewarded(
        activity: Activity,
        onUserEarnedReward: (RewardItem) -> Unit,
        onDismiss: () -> Unit = {}
    ) {
        mainHandler.post {
            val ad = rewardedAd
            if (ad != null) {
                var rewardEarned = false
                var earnedItem: RewardItem? = null

                AdDiagnosticsLogger.log(
                    AdType.REWARDED,
                    AdStatus.SHOWING,
                    "Calling show() on Rewarded Ad"
                )

                ad.fullScreenContentCallback = object : FullScreenContentCallback() {
                    override fun onAdDismissedFullScreenContent() {
                        Log.d(TAG, "Rewarded Ad dismissed")
                        rewardedAd = null
                        _isRewardedReady.value = false
                        AdDiagnosticsLogger.log(
                            AdType.REWARDED,
                            AdStatus.DISMISSED,
                            "Rewarded Ad dismissed"
                        )
                        if (rewardEarned && earnedItem != null) {
                            onUserEarnedReward(earnedItem!!)
                        }
                        onDismiss()
                        loadRewarded()
                    }

                    override fun onAdFailedToShowFullScreenContent(adError: AdError) {
                        Log.e(TAG, "Rewarded Ad failed to show: ${adError.message}")
                        rewardedAd = null
                        _isRewardedReady.value = false
                        AdDiagnosticsLogger.log(
                            AdType.REWARDED,
                            AdStatus.FAILED_TO_SHOW,
                            adError.message,
                            adError.code
                        )
                        Toast.makeText(activity, "Failed to display reward video: ${adError.message}", Toast.LENGTH_SHORT).show()
                        onDismiss()
                        loadRewarded()
                    }

                    override fun onAdShowedFullScreenContent() {
                        Log.d(TAG, "Rewarded Ad presented on screen")
                        rewardedAd = null
                        _isRewardedReady.value = false
                        AdDiagnosticsLogger.log(
                            AdType.REWARDED,
                            AdStatus.SHOWN,
                            "Rewarded Ad presented on screen"
                        )
                    }
                }

                ad.show(activity) { rewardItem ->
                    Log.d(TAG, "User earned reward: ${rewardItem.amount} ${rewardItem.type}")
                    rewardEarned = true
                    earnedItem = rewardItem
                    AdDiagnosticsLogger.log(
                        AdType.REWARDED,
                        AdStatus.REWARD_EARNED,
                        "Reward earned: ${rewardItem.amount} ${rewardItem.type}"
                    )
                }
            } else {
                Log.w(TAG, "Rewarded Ad requested but not ready yet")
                AdDiagnosticsLogger.log(
                    AdType.REWARDED,
                    AdStatus.FAILED_TO_SHOW,
                    "Rewarded Ad requested but not ready in memory (preloading triggered)"
                )
                Toast.makeText(activity, "Reward video is loading. Please try again in a moment.", Toast.LENGTH_SHORT).show()
                loadRewarded()
                onDismiss()
            }
        }
    }
}
