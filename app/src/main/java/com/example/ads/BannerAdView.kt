package com.example.ads

import android.util.Log
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Campaign
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdListener
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView
import com.google.android.gms.ads.LoadAdError

private const val TAG = "BannerAdView"

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    customAdUnitId: String? = null
) {
    val config by AdManager.activeConfig.collectAsStateWithLifecycle()
    val context = LocalContext.current

    // If ads are disabled remotely or banner is disabled, do not render or reserve space
    if (!config.adsEnabled || !config.bannerAdEnabled) {
        return
    }

    val adUnitId = customAdUnitId ?: config.bannerAdUnitId.ifBlank { AdConstants.TEST_BANNER_AD_UNIT_ID }

    var isAdLoaded by remember { mutableStateOf(false) }
    var adLoadError by remember { mutableStateOf<String?>(null) }
    var currentAdView by remember { mutableStateOf<AdView?>(null) }

    DisposableEffect(Unit) {
        onDispose {
            try {
                currentAdView?.destroy()
            } catch (e: Exception) {
                Log.w(TAG, "Error destroying AdView", e)
            }
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("admob_banner_container"),
        contentAlignment = Alignment.Center
    ) {
        // Standard AdMob AdView
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp)
                .testTag("admob_banner_view"),
            factory = { ctx ->
                AdView(ctx).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    adListener = object : AdListener() {
                        override fun onAdLoaded() {
                            isAdLoaded = true
                            adLoadError = null
                            Log.d(TAG, "AdMob Banner loaded successfully.")
                        }

                        override fun onAdFailedToLoad(error: LoadAdError) {
                            isAdLoaded = false
                            val errorDesc = "AdMob (Code ${error.code}): ${error.message}"
                            adLoadError = errorDesc
                            Log.w(TAG, "AdMob Banner failed to load: $errorDesc")
                        }

                        override fun onAdOpened() {
                            Log.d(TAG, "AdMob Banner opened.")
                        }

                        override fun onAdClicked() {
                            Log.d(TAG, "AdMob Banner clicked.")
                        }

                        override fun onAdClosed() {
                            Log.d(TAG, "AdMob Banner closed.")
                        }

                        override fun onAdImpression() {
                            Log.d(TAG, "AdMob Banner impression recorded.")
                        }
                    }
                    currentAdView = this
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { view ->
                if (view.adUnitId != adUnitId) {
                    view.adUnitId = adUnitId
                    view.loadAd(AdRequest.Builder().build())
                }
            }
        )

        // Fallback / Loading visual indicator if Google AdMob is loading or no-fill in development emulator
        if (!isAdLoaded && adLoadError != null) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.65f))
                    .border(0.5.dp, MaterialTheme.colorScheme.outlineVariant)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Box(
                        modifier = Modifier
                            .background(MaterialTheme.colorScheme.primaryContainer, RoundedCornerShape(4.dp))
                            .padding(horizontal = 6.dp, vertical = 2.dp)
                    ) {
                        Text(
                            text = "AD",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    Column {
                        Text(
                            text = "AdMob Banner Active • Ready for Live Ads",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = "Unit: ${adUnitId.take(18)}... | ${if (adLoadError?.contains("3") == true) "Awaiting Google fill" else "Connected"}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}
