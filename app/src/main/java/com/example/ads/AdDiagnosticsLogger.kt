package com.example.ads

import com.example.BuildConfig
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

enum class AdType {
    INTERSTITIAL,
    REWARDED,
    BANNER
}

enum class AdStatus {
    IDLE,
    LOADING,
    LOADED,
    FAILED_TO_LOAD,
    SHOWING,
    SHOWN,
    FAILED_TO_SHOW,
    DISMISSED,
    REWARD_EARNED
}

data class AdLogEntry(
    val id: Long = System.currentTimeMillis(),
    val timestamp: Long = System.currentTimeMillis(),
    val timeFormatted: String = SimpleDateFormat("HH:mm:ss.SSS", Locale.getDefault()).format(Date()),
    val adType: AdType,
    val status: AdStatus,
    val message: String,
    val errorCode: Int? = null
)

data class AdStatusSummary(
    val adType: AdType,
    val status: AdStatus = AdStatus.IDLE,
    val lastUpdated: Long = System.currentTimeMillis(),
    val loadRequests: Int = 0,
    val successfulLoads: Int = 0,
    val failedLoads: Int = 0,
    val impressions: Int = 0,
    val rewardsEarned: Int = 0,
    val lastError: String? = null,
    val lastErrorCode: Int? = null
)

/**
 * Diagnostic log wrapper for AdManager.
 * Tracks state transitions, error codes, and historical events
 * for Interstitial, Rewarded, and Banner ads.
 */
object AdDiagnosticsLogger {

    private val _interstitialSummary = MutableStateFlow(AdStatusSummary(AdType.INTERSTITIAL))
    val interstitialSummary: StateFlow<AdStatusSummary> = _interstitialSummary.asStateFlow()

    private val _rewardedSummary = MutableStateFlow(AdStatusSummary(AdType.REWARDED))
    val rewardedSummary: StateFlow<AdStatusSummary> = _rewardedSummary.asStateFlow()

    private val _bannerSummary = MutableStateFlow(AdStatusSummary(AdType.BANNER))
    val bannerSummary: StateFlow<AdStatusSummary> = _bannerSummary.asStateFlow()

    private val _recentLogs = MutableStateFlow<List<AdLogEntry>>(emptyList())
    val recentLogs: StateFlow<List<AdLogEntry>> = _recentLogs.asStateFlow()

    // Overlay is enabled by default in DEBUG builds, or can be toggled by the user
    private val _isOverlayVisible = MutableStateFlow(BuildConfig.DEBUG)
    val isOverlayVisible: StateFlow<Boolean> = _isOverlayVisible.asStateFlow()

    private val _isExpanded = MutableStateFlow(false)
    val isExpanded: StateFlow<Boolean> = _isExpanded.asStateFlow()

    private const val MAX_LOGS = 100

    fun log(
        adType: AdType,
        status: AdStatus,
        message: String,
        errorCode: Int? = null
    ) {
        val entry = AdLogEntry(
            adType = adType,
            status = status,
            message = message,
            errorCode = errorCode
        )

        // Update logs list
        _recentLogs.update { current ->
            (listOf(entry) + current).take(MAX_LOGS)
        }

        // Update respective summary
        when (adType) {
            AdType.INTERSTITIAL -> updateSummary(_interstitialSummary, status, message, errorCode)
            AdType.REWARDED -> updateSummary(_rewardedSummary, status, message, errorCode)
            AdType.BANNER -> updateSummary(_bannerSummary, status, message, errorCode)
        }
    }

    private fun updateSummary(
        flow: MutableStateFlow<AdStatusSummary>,
        status: AdStatus,
        message: String,
        errorCode: Int?
    ) {
        flow.update { current ->
            current.copy(
                status = status,
                lastUpdated = System.currentTimeMillis(),
                loadRequests = if (status == AdStatus.LOADING) current.loadRequests + 1 else current.loadRequests,
                successfulLoads = if (status == AdStatus.LOADED) current.successfulLoads + 1 else current.successfulLoads,
                failedLoads = if (status == AdStatus.FAILED_TO_LOAD) current.failedLoads + 1 else current.failedLoads,
                impressions = if (status == AdStatus.SHOWN) current.impressions + 1 else current.impressions,
                rewardsEarned = if (status == AdStatus.REWARD_EARNED) current.rewardsEarned + 1 else current.rewardsEarned,
                lastError = if (status == AdStatus.FAILED_TO_LOAD || status == AdStatus.FAILED_TO_SHOW) message else current.lastError,
                lastErrorCode = if (errorCode != null) errorCode else current.lastErrorCode
            )
        }
    }

    fun setOverlayVisible(visible: Boolean) {
        _isOverlayVisible.value = visible
    }

    fun toggleOverlay() {
        _isOverlayVisible.value = !_isOverlayVisible.value
    }

    fun setExpanded(expanded: Boolean) {
        _isExpanded.value = expanded
    }

    fun toggleExpanded() {
        _isExpanded.value = !_isExpanded.value
    }

    fun clearLogs() {
        _recentLogs.value = emptyList()
    }
}
