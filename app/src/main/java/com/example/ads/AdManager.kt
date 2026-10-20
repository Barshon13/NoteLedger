package com.example.ads

import android.content.Context
import android.util.Log
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.LoadAdError
import com.google.android.gms.ads.MobileAds
import com.google.android.gms.ads.initialization.InitializationStatus
import com.google.android.gms.ads.interstitial.InterstitialAd
import com.google.android.gms.ads.interstitial.InterstitialAdLoadCallback

object AdManager {
    private const val TAG = "AdManager"
    
    // Test Interstitial Ad ID
    private const val INTERSTITIAL_AD_ID = "ca-app-pub-3940256099942544/1033173712"
    
    private var interstitialAd: InterstitialAd? = null

    fun initialize(context: Context) {
        MobileAds.initialize(context) { status: InitializationStatus ->
            Log.d(TAG, "AdMob Initialized: $status")
            loadInterstitial(context)
        }
    }

    private fun loadInterstitial(context: Context) {
        val adRequest = AdRequest.Builder().build()
        InterstitialAd.load(
            context,
            INTERSTITIAL_AD_ID,
            adRequest,
            object : InterstitialAdLoadCallback() {
                override fun onAdLoaded(ad: InterstitialAd) {
                    interstitialAd = ad
                    Log.d(TAG, "Interstitial Ad Loaded")
                }

                override fun onAdFailedToLoad(error: LoadAdError) {
                    interstitialAd = null
                    Log.d(TAG, "Interstitial Ad Failed to Load: ${error.message}")
                }
            }
        )
    }

    fun showInterstitial(activity: android.app.Activity) {
        if (interstitialAd != null) {
            interstitialAd?.show(activity)
            loadInterstitial(activity) // Load next one
        } else {
            Log.d(TAG, "Interstitial Ad not ready yet")
            loadInterstitial(activity)
        }
    }
}
