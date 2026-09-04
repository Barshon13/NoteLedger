package com.example.ads

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.ads.AdRequest
import com.google.android.gms.ads.AdSize
import com.google.android.gms.ads.AdView

@Composable
fun BannerAdView(
    modifier: Modifier = Modifier,
    customAdUnitId: String? = null
) {
    val config by AdManager.activeConfig.collectAsStateWithLifecycle()

    // If ads are disabled remotely or banner is disabled, do not render or reserve space
    if (!config.adsEnabled || !config.bannerAdEnabled) {
        return
    }

    val adUnitId = customAdUnitId ?: config.bannerAdUnitId.ifBlank { AdConstants.TEST_BANNER_AD_UNIT_ID }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(MaterialTheme.colorScheme.surface)
            .testTag("admob_banner_view"),
        contentAlignment = Alignment.Center
    ) {
        AndroidView(
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            factory = { context ->
                AdView(context).apply {
                    setAdSize(AdSize.BANNER)
                    this.adUnitId = adUnitId
                    loadAd(AdRequest.Builder().build())
                }
            },
            update = { view ->
                // If ad unit changes dynamically, reload ad
                if (view.adUnitId != adUnitId) {
                    view.adUnitId = adUnitId
                    view.loadAd(AdRequest.Builder().build())
                }
            }
        )
    }
}
