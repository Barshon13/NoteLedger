package com.example.ads

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class RemoteAdsConfig(
    @Json(name = "ads_enabled")
    val adsEnabled: Boolean = true,

    @Json(name = "banner_ad_enabled")
    val bannerAdEnabled: Boolean = true,

    @Json(name = "interstitial_ad_enabled")
    val interstitialAdEnabled: Boolean = true,

    @Json(name = "rewarded_ad_enabled")
    val rewardedAdEnabled: Boolean = true,

    @Json(name = "banner_ad_unit_id")
    val bannerAdUnitId: String = AdConstants.TEST_BANNER_AD_UNIT_ID,

    @Json(name = "interstitial_ad_unit_id")
    val interstitialAdUnitId: String = AdConstants.TEST_INTERSTITIAL_AD_UNIT_ID,

    @Json(name = "rewarded_ad_unit_id")
    val rewardedAdUnitId: String = AdConstants.TEST_REWARDED_AD_UNIT_ID,

    @Json(name = "interstitial_interval_actions")
    val interstitialIntervalActions: Int = 2
)
