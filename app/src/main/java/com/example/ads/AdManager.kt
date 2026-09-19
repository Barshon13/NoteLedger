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
import com.google.android.ump.ConsentInformation
import com.google.android.ump.ConsentRequestParameters
import com.google.android.ump.UserMessagingPlatform
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AdManager provides a singleton bridge for Google Mobile Ads (AdMob) with Google User Messaging
 * Platform (UMP) Consent Management for EEA/UK (GDPR) and US State privacy compliance.
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

    private val _isPrivacyOptionsRequired = MutableStateFlow(false)
    val isPrivacyOptionsRequired: StateFlow<Boolean> = _isPrivacyOptionsRequired.asStateFlow()

    private var interstitialAd: InterstitialAd? = null
    private var isInterstitialLoading: Boolean = false

    private val mainHandler = Handler(Looper.getMainLooper())
    private val isMobileAdsInitializeCalled = AtomicBoolean(false)

    /**
     * Gathers user consent via Google User Messaging Platform (UMP) SDK and initializes
     * MobileAds once consent is obtained (or immediately if outside consent jurisdictions).
     */
    fun gatherConsentAndInitialize(activity: Activity) {
        appContext = activity.applicationContext

        val params = ConsentRequestParameters.Builder()
            .setTagForUnderAgeOfConsent(false)
            .build()

        val consentInformation = UserMessagingPlatform.getConsentInformation(activity)
        consentInformation.requestConsentInfoUpdate(
            activity,
            params,
            {
                UserMessagingPlatform.loadAndShowConsentFormIfRequired(activity) { formError ->
                    if (formError != null) {
                        Log.w(TAG, "UMP Consent form error: ${formError.errorCode}: ${formError.message}")
                    }

                    _isPrivacyOptionsRequired.value =
                        consentInformation.privacyOptionsRequirementStatus ==
                                ConsentInformation.PrivacyOptionsRequirementStatus.REQUIRED

                    if (consentInformation.canRequestAds()) {
                        initializeMobileAds(activity.applicationContext)
                    }
                }
            },
            { requestConsentError ->
                Log.w(TAG, "UMP Consent info update failed: ${requestConsentError.errorCode}: ${requestConsentError.message}")
                if (consentInformation.canRequestAds()) {
                    initializeMobileAds(activity.applicationContext)
                }
            }
        )

        // Check if cached consent allows ad requests immediately
        if (consentInformation.canRequestAds()) {
            initializeMobileAds(activity.applicationContext)
        }
    }

    /**
     * Initializes Google Mobile Ads SDK safely and idempotently.
     */
    fun initialize(context: Context) {
        appContext = context.applicationContext
        val activity = findActivity(context)
        if (activity != null) {
            gatherConsentAndInitialize(activity)
        } else {
            initializeMobileAds(context.applicationContext)
        }
    }

    private fun initializeMobileAds(context: Context) {
        if (isMobileAdsInitializeCalled.compareAndSet(false, true)) {
            Log.d(TAG, "Initializing Google Mobile Ads (AdMob)...")
            MobileAds.initialize(context) { status ->
                _isInitialized.value = true
                Log.d(TAG, "MobileAds initialized: $status")
                loadInterstitialAd()
            }
        } else if (_isInitialized.value) {
            loadInterstitialAd()
        }
    }

    /**
     * Shows privacy options form (GDPR consent revocation/update) for Google compliance.
     */
    fun showPrivacyOptionsForm(activity: Activity, onDismissed: () -> Unit = {}) {
        UserMessagingPlatform.showPrivacyOptionsForm(activity) { formError ->
            if (formError != null) {
                Log.w(TAG, "Error showing privacy options form: ${formError.message}")
            }
            onDismissed()
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
        if (!currentConfig.adsEnabled || !currentConfig.interstitialAdEnabled) return
        if (interstitialAd != null || isInterstitialLoading) return

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
            safeDismiss()
            return
        }

        val ad = interstitialAd
        if (ad != null) {
            showLoadedAd(activity, ad, ::safeDismiss)
            return
        }

        if (isInterstitialLoading) {
            var checksRemaining = 9
            fun checkAd() {
                val currentAd = interstitialAd
                if (currentAd != null && !activity.isFinishing && !activity.isDestroyed) {
                    showLoadedAd(activity, currentAd, ::safeDismiss)
                } else if (checksRemaining > 0 && isInterstitialLoading && !hasInvokedCallback.get()) {
                    checksRemaining--
                    mainHandler.postDelayed(::checkAd, 200)
                } else {
                    safeDismiss()
                }
            }
            mainHandler.postDelayed(::checkAd, 200)
            return
        }

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

    fun showInterstitial(context: Context, onAdDismissed: () -> Unit) {
        val activity = findActivity(context)
        if (activity != null) {
            showInterstitialAd(activity, onAdDismissed)
        } else {
            onAdDismissed()
        }
    }

    fun tryShowInterstitial(context: Context, force: Boolean = false, onDismissed: () -> Unit = {}) {
        showInterstitial(context, onDismissed)
    }

    fun findActivity(context: Context): Activity? {
        var currentContext = context
        while (currentContext is ContextWrapper) {
            if (currentContext is Activity) return currentContext
            currentContext = currentContext.baseContext
        }
        return null
    }
}
