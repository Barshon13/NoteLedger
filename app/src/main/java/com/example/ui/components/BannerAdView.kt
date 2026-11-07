package com.example.ui.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.ads.AdDiagnosticsLogger
import com.example.ads.AdStatus
import com.example.ads.AdType
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

@Composable
fun BannerAdView(modifier: Modifier = Modifier) {
    // Test Banner Ad ID
    val adUnitId = "ca-app-pub-3940256099942544/6300978111"
    var isAdLoaded by remember { mutableStateOf(false) }

    AndroidView(
        modifier = if (isAdLoaded) {
            modifier
                .fillMaxWidth()
                .height(50.dp)
        } else {
            Modifier.size(0.dp)
        },
        factory = { context ->
            AdView(context).apply {
                setAdSize(AdSize.BANNER)
                setAdUnitId(adUnitId)
                adListener = object : AdListener() {
                    override fun onAdLoaded() {
                        isAdLoaded = true
                        AdDiagnosticsLogger.log(
                            AdType.BANNER,
                            AdStatus.LOADED,
                            "Banner ad loaded and displayed"
                        )
                    }

                    override fun onAdFailedToLoad(error: LoadAdError) {
                        isAdLoaded = false
                        AdDiagnosticsLogger.log(
                            AdType.BANNER,
                            AdStatus.FAILED_TO_LOAD,
                            "Banner ad failed: ${error.message}",
                            error.code
                        )
                    }

                    override fun onAdOpened() {
                        AdDiagnosticsLogger.log(
                            AdType.BANNER,
                            AdStatus.SHOWN,
                            "Banner ad opened/clicked"
                        )
                    }
                }
                AdDiagnosticsLogger.log(
                    AdType.BANNER,
                    AdStatus.LOADING,
                    "Loading Banner ad ($adUnitId)"
                )
                loadAd(AdRequest.Builder().build())
            }
        }
    )
}
