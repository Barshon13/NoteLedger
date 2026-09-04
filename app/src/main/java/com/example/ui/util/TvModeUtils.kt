package com.example.ui.util

import android.app.UiModeManager
import android.content.Context
import android.content.pm.PackageManager
import android.content.res.Configuration
import android.os.Build
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.border
import androidx.compose.foundation.focusable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsFocusedAsState
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.input.key.Key
import androidx.compose.ui.input.key.KeyEventType
import androidx.compose.ui.input.key.key
import androidx.compose.ui.input.key.onKeyEvent
import androidx.compose.ui.input.key.type
import androidx.compose.ui.unit.dp

/**
 * CompositionLocal to identify if running on Fire OS / Fire TV / Android TV.
 */
val LocalIsTvDevice = compositionLocalOf { false }

object TvModeUtils {

    /**
     * Determines whether the app is executing on Amazon Fire TV, Fire OS, or Android TV.
     */
    fun isRunningOnTv(context: Context): Boolean {
        val pm = context.packageManager

        // 1. Amazon Fire TV specific hardware feature
        val isFireTvFeature = pm.hasSystemFeature("amazon.hardware.fire_tv")

        // 2. Android TV / Leanback feature
        val isLeanback = pm.hasSystemFeature(PackageManager.FEATURE_LEANBACK)

        // 3. UI Mode Television
        val uiModeManager = context.getSystemService(Context.UI_MODE_SERVICE) as? UiModeManager
        val isUiModeTv = uiModeManager?.currentModeType == Configuration.UI_MODE_TYPE_TELEVISION

        // 4. Amazon Manufacturer / Model inspection (e.g. AFTMM, AFTSS, AFTSO, Fire TV Stick, Fire TV Cube)
        val isAmazon = Build.MANUFACTURER.equals("Amazon", ignoreCase = true)
        val model = Build.MODEL.uppercase()
        val isAmazonTvModel = model.startsWith("AFT") || model.contains("FIRE TV") || model.contains("FIRETV")

        return isFireTvFeature || isLeanback || isUiModeTv || (isAmazon && isAmazonTvModel)
    }

    /**
     * Safe overscan padding for TV displays to prevent UI clipping on older HDMI televisions.
     */
    fun getTvSafePadding(isTv: Boolean): PaddingValues {
        return if (isTv) {
            PaddingValues(horizontal = 32.dp, vertical = 24.dp)
        } else {
            PaddingValues(0.dp)
        }
    }
}

/**
 * Adds visible focus outline and D-Pad center key activation when navigating with Fire TV remote.
 */
fun Modifier.tvFocusHighlight(
    shape: Shape = RoundedCornerShape(12.dp),
    focusedBorderColor: Color? = null,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    val interactionSource = remember { MutableInteractionSource() }
    val isFocused by interactionSource.collectIsFocusedAsState()
    val highlightColor = focusedBorderColor ?: MaterialTheme.colorScheme.primary

    this
        .onKeyEvent { event ->
            if (event.type == KeyEventType.KeyUp &&
                (event.key == Key.DirectionCenter || event.key == Key.Enter || event.key == Key.NumPadEnter)
            ) {
                if (onClick != null) {
                    onClick()
                    true
                } else {
                    false
                }
            } else {
                false
            }
        }
        .focusable(interactionSource = interactionSource)
        .then(
            if (isFocused) {
                Modifier.border(
                    BorderStroke(2.5.dp, highlightColor),
                    shape = shape
                )
            } else {
                Modifier
            }
        )
}
