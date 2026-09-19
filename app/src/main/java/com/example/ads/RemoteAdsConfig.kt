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
    // If the unit ID was cached from the older Unity LevelPlay format (not starting with "ca-app-pub-"),
    // sanitize it by falling back to the official AdMob Test Ad Unit ID.
    val effectiveBannerAdUnitId: String
        get() {
            val id = bannerAdUnitId.trim()
            return if (id.startsWith("ca-app-pub-") || id.startsWith("/6499/")) {
                id
            } else {
                AdConstants.DEFAULT_BANNER_AD_UNIT_ID
            }
        }

    val effectiveInterstitialAdUnitId: String
        get() {
            val id = interstitialAdUnitId.trim()
            return if (id.startsWith("ca-app-pub-") || id.startsWith("/6499/")) {
                id
            } else {
                AdConstants.DEFAULT_INTERSTITIAL_AD_UNIT_ID
            }
        }
}
