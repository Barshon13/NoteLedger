package com.example.ads

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

/**
 * BannerAdView hosts the Google Mobile Ads (AdMob) banner ad inside Jetpack Compose.
 *
 * Collapses to 0.dp when disabled or if an ad fails to load, preventing
 * awkward whitespace or fake placeholder badges.
 */
@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    adUnitId: String = AdConstants.DEFAULT_BANNER_AD_UNIT_ID,
    testTag: String = "admob_banner_ad_view"
) {
    val lifecycleOwner = LocalLifecycleOwner.current
    var isAdLoaded by remember { mutableStateOf(false) }
    var adViewRef by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(lifecycleOwner, adViewRef) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_RESUME -> adViewRef?.resume()
                Lifecycle.Event.ON_PAUSE -> adViewRef?.pause()
                Lifecycle.Event.ON_DESTROY -> adViewRef?.destroy()
                else -> {}
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose {
            lifecycleOwner.lifecycle.removeObserver(observer)
            adViewRef?.destroy()
            adViewRef = null
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(if (isAdLoaded) 50.dp else 0.dp)
            .testTag(testTag),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier.fillMaxWidth(),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    setAdUnitId(adUnitId)
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                        }

                        override fun onAdFailedToLoad(loadAdError: LoadAdError) {
                            isAdLoaded = false
                        }
                    }
                    adViewRef = this
                    loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
