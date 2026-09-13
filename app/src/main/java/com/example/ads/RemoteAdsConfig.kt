package com.example.ads

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAdsConfig(
    @Json(name = "ads_enabled")
    val adsEnabled: Boolean = true,

    @Json(name = "ad_network")
    val adNetwork: String = "admob",

    @Json(name = "app_id")
    val appId: String = AdConstants.DEFAULT_SAMPLE_APP_ID,

    @Json(name = "banner_ad_unit_id")
    val bannerAdUnitId: String = AdConstants.DEFAULT_BANNER_AD_UNIT_ID,

    @Json(name = "interstitial_ad_unit_id")
    val interstitialAdUnitId: String = AdConstants.DEFAULT_INTERSTITIAL_AD_UNIT_ID,

    @Json(name = "banner_ad_enabled")
    val bannerAdEnabled: Boolean = true,

    @Json(name = "interstitial_ad_enabled")
    val interstitialAdEnabled: Boolean = true,

    @Json(name = "interstitial_interval_clicks")
    val interstitialIntervalClicks: Int = 1
) {
    val effectiveBannerAdUnitId: String
        get() = bannerAdUnitId.takeIf { it.isNotBlank() } ?: AdConstants.DEFAULT_BANNER_AD_UNIT_ID

    val effectiveInterstitialAdUnitId: String
        get() = interstitialAdUnitId.takeIf { it.isNotBlank() } ?: AdConstants.DEFAULT_INTERSTITIAL_AD_UNIT_ID
}
