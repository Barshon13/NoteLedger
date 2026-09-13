package com.example.crashlytics

import android.content.Context
import android.os.Build
import android.util.Log
import com.example.BuildConfig
import com.google.firebase.crashlytics.FirebaseCrashlytics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import java.util.Locale

/**
 * Manages Firebase Crashlytics stability monitoring, real-world crash reporting,
 * and environment diagnostic tagging optimized for the OPPO App Market ecosystem.
 */
object CrashlyticsManager {

    private const val TAG = "CrashlyticsManager"

    data class StabilityDiagnostics(
        val isCrashlyticsActive: Boolean,
        val targetMarket: String,
        val installerPackage: String,
        val isOppoEcosystem: Boolean,
        val colorOsVersion: String?,
        val deviceModel: String,
        val androidVersion: String,
        val appVersion: String,
        val totalRecordedExceptions: Int
    )

    private val _diagnostics = MutableStateFlow(
        StabilityDiagnostics(
            isCrashlyticsActive = false,
            targetMarket = "OPPO App Market (Detecting...)",
            installerPackage = "Pending",
            isOppoEcosystem = false,
            colorOsVersion = null,
            deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
            androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
            appVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
            totalRecordedExceptions = 0
        )
    )
    val diagnostics: StateFlow<StabilityDiagnostics> = _diagnostics.asStateFlow()

    private var exceptionCount = 0

    /**
     * Initializes Crashlytics and instruments device & environment tags.
     */
    fun initialize(context: Context) {
        try {
            val crashlytics = FirebaseCrashlytics.getInstance()
            crashlytics.isCrashlyticsCollectionEnabled = true

            val installer = getInstallerPackage(context)
            val isOppoInstaller = isOppoInstallerPackage(installer)
            val isOppoDevice = isOppoDeviceFamily()
            val colorOs = detectColorOsVersion()

            val marketName = when {
                isOppoInstaller -> "OPPO App Market / HeyTap"
                isOppoDevice -> "OPPO Device Ecosystem"
                installer.contains("vending") || installer.contains("google") -> "Google Play"
                installer.isNotBlank() -> "Custom Store ($installer)"
                else -> "Direct / Sideload (Target: OPPO App Market)"
            }

            // Instrument Crashlytics Custom Keys for filtering in Firebase Console
            crashlytics.setCustomKey("target_market", "oppo_app_market")
            crashlytics.setCustomKey("detected_market", marketName)
            crashlytics.setCustomKey("installer_package", installer.ifBlank { "direct_apk" })
            crashlytics.setCustomKey("is_oppo_ecosystem", isOppoDevice || isOppoInstaller)
            crashlytics.setCustomKey("device_manufacturer", Build.MANUFACTURER)
            crashlytics.setCustomKey("device_brand", Build.BRAND)
            crashlytics.setCustomKey("device_model", Build.MODEL)
            crashlytics.setCustomKey("device_hardware", Build.HARDWARE)
            crashlytics.setCustomKey("device_fingerprint", Build.FINGERPRINT)
            crashlytics.setCustomKey("android_release", Build.VERSION.RELEASE)
            crashlytics.setCustomKey("android_sdk_int", Build.VERSION.SDK_INT)
            crashlytics.setCustomKey("app_version_name", BuildConfig.VERSION_NAME)
            crashlytics.setCustomKey("app_version_code", BuildConfig.VERSION_CODE)
            if (!colorOs.isNullOrBlank()) {
                crashlytics.setCustomKey("coloros_version", colorOs)
            }

            crashlytics.log("NoteLedger initialized in $marketName environment on ${Build.MANUFACTURER} ${Build.MODEL} ($colorOs)")

            _diagnostics.value = StabilityDiagnostics(
                isCrashlyticsActive = true,
                targetMarket = marketName,
                installerPackage = installer.ifBlank { "Direct APK / Development" },
                isOppoEcosystem = isOppoDevice || isOppoInstaller,
                colorOsVersion = colorOs,
                deviceModel = "${Build.MANUFACTURER} ${Build.MODEL}",
                androidVersion = "Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})",
                appVersion = "v${BuildConfig.VERSION_NAME} (${BuildConfig.VERSION_CODE})",
                totalRecordedExceptions = exceptionCount
            )

            Log.i(TAG, "Firebase Crashlytics initialized successfully. Market: $marketName, OPPO Ecosystem: ${isOppoDevice || isOppoInstaller}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to initialize Crashlytics", e)
        }
    }

    /**
     * Records a non-fatal handled exception with contextual metadata to Crashlytics.
     */
    fun recordException(throwable: Throwable, contextTag: String? = null) {
        try {
            exceptionCount++
            val crashlytics = FirebaseCrashlytics.getInstance()
            if (!contextTag.isNullOrBlank()) {
                crashlytics.log("Handled Exception in [$contextTag]: ${throwable.message}")
                crashlytics.setCustomKey("last_handled_tag", contextTag)
            }
            crashlytics.recordException(throwable)

            val current = _diagnostics.value
            _diagnostics.value = current.copy(totalRecordedExceptions = exceptionCount)

            Log.w(TAG, "Recorded non-fatal exception to Crashlytics [tag=$contextTag]: ${throwable.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Unable to report exception to Crashlytics", e)
        }
    }

    /**
     * Logs a session breadcrumb for debugging crash sequences.
     */
    fun log(message: String) {
        try {
            FirebaseCrashlytics.getInstance().log(message)
            Log.d(TAG, "Breadcrumb: $message")
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Sets a custom string key in Crashlytics.
     */
    fun setCustomKey(key: String, value: String) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Sets a custom boolean key in Crashlytics.
     */
    fun setCustomKey(key: String, value: Boolean) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Sets a custom integer key in Crashlytics.
     */
    fun setCustomKey(key: String, value: Int) {
        try {
            FirebaseCrashlytics.getInstance().setCustomKey(key, value)
        } catch (e: Exception) {
            // Safe fallback
        }
    }

    /**
     * Simulates a non-fatal test exception to verify Firebase Crashlytics connectivity.
     */
    fun triggerTestNonFatal() {
        val testException = Exception("OPPO App Market Stability Test - Non-fatal event logged from NoteLedger v${BuildConfig.VERSION_NAME}")
        recordException(testException, "OppoStabilityDiagnostics")
    }

    /**
     * Simulates a test crash to verify full unhandled crash dump in Crashlytics console.
     */
    fun triggerTestCrash(): Nothing {
        log("Triggering deliberate test crash to verify OPPO App Market crash reporting")
        throw RuntimeException("NoteLedger Test Crash: Verifying Firebase Crashlytics integration for OPPO App Market")
    }

    private fun getInstallerPackage(context: Context): String {
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                context.packageManager.getInstallSourceInfo(context.packageName).installingPackageName ?: ""
            } else {
                @Suppress("DEPRECATION")
                context.packageManager.getInstallerPackageName(context.packageName) ?: ""
            }
        } catch (e: Exception) {
            ""
        }
    }

    private fun isOppoInstallerPackage(installer: String): Boolean {
        val lower = installer.lowercase(Locale.US)
        return lower.contains("oppo") ||
                lower.contains("heytap") ||
                lower.contains("coloros") ||
                lower.contains("nearme")
    }

    private fun isOppoDeviceFamily(): Boolean {
        val manufacturer = Build.MANUFACTURER.lowercase(Locale.US)
        val brand = Build.BRAND.lowercase(Locale.US)
        return manufacturer.contains("oppo") ||
                brand.contains("oppo") ||
                manufacturer.contains("realme") ||
                brand.contains("realme") ||
                manufacturer.contains("oneplus") ||
                brand.contains("oneplus")
    }

    private fun detectColorOsVersion(): String? {
        return try {
            val systemPropertiesClass = Class.forName("android.os.SystemProperties")
            val getMethod = systemPropertiesClass.getMethod("get", String::class.java)

            val oppoRom = getMethod.invoke(null, "ro.build.version.opporom") as? String
            if (!oppoRom.isNullOrBlank()) return "ColorOS $oppoRom"

            val oplusRom = getMethod.invoke(null, "ro.oplus.version.my_manifest") as? String
            if (!oplusRom.isNullOrBlank()) return "ColorOS $oplusRom"

            val realmeRom = getMethod.invoke(null, "ro.build.version.realmeui") as? String
            if (!realmeRom.isNullOrBlank()) return "Realme UI $realmeRom"

            if (isOppoDeviceFamily()) "ColorOS / OPlus" else null
        } catch (e: Exception) {
            if (isOppoDeviceFamily()) "ColorOS" else null
        }
    }
}
