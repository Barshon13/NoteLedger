package com.example.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

fun getPaletteColorScheme(palette: ThemePalette, isDark: Boolean): ColorScheme {
    return when (palette) {
        ThemePalette.SAGE_FOREST -> if (isDark) DarkSageScheme else LightSageScheme
        ThemePalette.OCEAN_SLATE -> if (isDark) DarkOceanScheme else LightOceanScheme
        ThemePalette.SUNSET_TERRACOTTA -> if (isDark) DarkSunsetScheme else LightSunsetScheme
        ThemePalette.ROYAL_VIOLET -> if (isDark) DarkVioletScheme else LightVioletScheme
        ThemePalette.EMERALD_MINT -> if (isDark) DarkEmeraldScheme else LightEmeraldScheme
        ThemePalette.CRIMSON_BERRY -> if (isDark) DarkCrimsonScheme else LightCrimsonScheme
    }
}

// 1. Sage Forest (Default Earthy & Balanced)
private val LightSageScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    inversePrimary = InversePrimaryLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    surfaceContainerLowest = SurfaceContainerLowestLight,
    surfaceContainerLow = SurfaceContainerLowLight,
    surfaceContainer = SurfaceContainerLight,
    surfaceContainerHigh = SurfaceContainerHighLight,
    surfaceContainerHighest = SurfaceContainerHighestLight,
    inverseSurface = InverseSurfaceLight,
    inverseOnSurface = InverseOnSurfaceLight,
    outline = OutlineLight,
    outlineVariant = OutlineVariantLight,
    scrim = ScrimLight
)

private val DarkSageScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    inversePrimary = InversePrimaryDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    surfaceContainerLowest = SurfaceContainerLowestDark,
    surfaceContainerLow = SurfaceContainerLowDark,
    surfaceContainer = SurfaceContainerDark,
    surfaceContainerHigh = SurfaceContainerHighDark,
    surfaceContainerHighest = SurfaceContainerHighestDark,
    inverseSurface = InverseSurfaceDark,
    inverseOnSurface = InverseOnSurfaceDark,
    outline = OutlineDark,
    outlineVariant = OutlineVariantDark,
    scrim = ScrimDark
)

// 2. Ocean Slate (Nordic Navy & Coastal Sky)
private val LightOceanScheme = lightColorScheme(
    primary = Color(0xFF2B5B84),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFD0E4FF),
    onPrimaryContainer = Color(0xFF001D36),
    inversePrimary = Color(0xFF9ECCF8),
    secondary = Color(0xFF4F6070),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFD3E4F7),
    onSecondaryContainer = Color(0xFF0C1D2B),
    tertiary = Color(0xFF3B6470),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFBEEAF7),
    onTertiaryContainer = Color(0xFF001F26),
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = Color(0xFFF7F9FC),
    onBackground = Color(0xFF191C20),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF191C20),
    surfaceVariant = Color(0xFFDEE3EB),
    onSurfaceVariant = Color(0xFF42474E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF1F4F9),
    surfaceContainer = Color(0xFFEBEFF4),
    surfaceContainerHigh = Color(0xFFE5E9EE),
    surfaceContainerHighest = Color(0xFFDFE3E8),
    inverseSurface = Color(0xFF2E3135),
    inverseOnSurface = Color(0xFFF0F0F4),
    outline = Color(0xFFD5DFEB),
    outlineVariant = Color(0xFFC2CBD5)
)

private val DarkOceanScheme = darkColorScheme(
    primary = Color(0xFF9ECCF8),
    onPrimary = Color(0xFF003258),
    primaryContainer = Color(0xFF0E436B),
    onPrimaryContainer = Color(0xFFD0E4FF),
    inversePrimary = Color(0xFF2B5B84),
    secondary = Color(0xFFB7C8DB),
    onSecondary = Color(0xFF213241),
    secondaryContainer = Color(0xFF374958),
    onSecondaryContainer = Color(0xFFD3E4F7),
    tertiary = Color(0xFFA2CEDD),
    onTertiary = Color(0xFF033541),
    tertiaryContainer = Color(0xFF224C58),
    onTertiaryContainer = Color(0xFFBEEAF7),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color(0xFF0E1318),
    onBackground = Color(0xFFDFE3E8),
    surface = Color(0xFF161C22),
    onSurface = Color(0xFFDFE3E8),
    surfaceVariant = Color(0xFF252D36),
    onSurfaceVariant = Color(0xFFC2CBD5),
    surfaceContainerLowest = Color(0xFF0B0F13),
    surfaceContainerLow = Color(0xFF131920),
    surfaceContainer = Color(0xFF181F26),
    surfaceContainerHigh = Color(0xFF222B34),
    surfaceContainerHighest = Color(0xFF2D3742),
    inverseSurface = Color(0xFFDFE3E8),
    inverseOnSurface = Color(0xFF161C22),
    outline = Color(0xFF45505C),
    outlineVariant = Color(0xFF35404B)
)

// 3. Sunset Terracotta (Warm Clay & Amber)
private val LightSunsetScheme = lightColorScheme(
    primary = Color(0xFF9C412B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFDAD2),
    onPrimaryContainer = Color(0xFF3D0700),
    inversePrimary = Color(0xFFFFB4A2),
    secondary = Color(0xFF77574E),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFDAD2),
    onSecondaryContainer = Color(0xFF2C150F),
    tertiary = Color(0xFF7D5737),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCBE),
    onTertiaryContainer = Color(0xFF2E1500),
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = Color(0xFFFDF8F6),
    onBackground = Color(0xFF221A18),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF221A18),
    surfaceVariant = Color(0xFFF5DED8),
    onSurfaceVariant = Color(0xFF534340),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF2EE),
    surfaceContainer = Color(0xFFF5EBE6),
    surfaceContainerHigh = Color(0xFFEFE5E0),
    surfaceContainerHighest = Color(0xFFEAE0DB),
    inverseSurface = Color(0xFF382E2C),
    inverseOnSurface = Color(0xFFFEECE7),
    outline = Color(0xFFEBDDD8),
    outlineVariant = Color(0xFFD8C2BC)
)

private val DarkSunsetScheme = darkColorScheme(
    primary = Color(0xFFFFB4A2),
    onPrimary = Color(0xFF5F1504),
    primaryContainer = Color(0xFF7D2B16),
    onPrimaryContainer = Color(0xFFFFDAD2),
    inversePrimary = Color(0xFF9C412B),
    secondary = Color(0xFFE7BDB2),
    onSecondary = Color(0xFF442A22),
    secondaryContainer = Color(0xFF5D3F37),
    onSecondaryContainer = Color(0xFFFFDAD2),
    tertiary = Color(0xFFF1BE94),
    onTertiary = Color(0xFF472A0D),
    tertiaryContainer = Color(0xFF624021),
    onTertiaryContainer = Color(0xFFFFDCBE),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color(0xFF181211),
    onBackground = Color(0xFFEDE0DD),
    surface = Color(0xFF221A18),
    onSurface = Color(0xFFEDE0DD),
    surfaceVariant = Color(0xFF362B28),
    onSurfaceVariant = Color(0xFFD8C2BC),
    surfaceContainerLowest = Color(0xFF120D0C),
    surfaceContainerLow = Color(0xFF1C1513),
    surfaceContainer = Color(0xFF241D1A),
    surfaceContainerHigh = Color(0xFF2F2623),
    surfaceContainerHighest = Color(0xFF3B302D),
    inverseSurface = Color(0xFFEDE0DD),
    inverseOnSurface = Color(0xFF221A18),
    outline = Color(0xFF5E4C48),
    outlineVariant = Color(0xFF4A3C38)
)

// 4. Royal Violet (Plum & Lavender)
private val LightVioletScheme = lightColorScheme(
    primary = Color(0xFF6750A4),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFEADDFF),
    onPrimaryContainer = Color(0xFF21005D),
    inversePrimary = Color(0xFFD0BCFF),
    secondary = Color(0xFF625B71),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFE8DEF8),
    onSecondaryContainer = Color(0xFF1D192B),
    tertiary = Color(0xFF7D5260),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFD8E4),
    onTertiaryContainer = Color(0xFF31111D),
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = Color(0xFFFAF7FD),
    onBackground = Color(0xFF1D1A22),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF1D1A22),
    surfaceVariant = Color(0xFFE7E0EB),
    onSurfaceVariant = Color(0xFF49454E),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF5F1FA),
    surfaceContainer = Color(0xFFEFEBF4),
    surfaceContainerHigh = Color(0xFFE9E5EE),
    surfaceContainerHighest = Color(0xFFE3DFE8),
    inverseSurface = Color(0xFF322F37),
    inverseOnSurface = Color(0xFFF5EFF7),
    outline = Color(0xFFE6E0EB),
    outlineVariant = Color(0xFFCAC4D0)
)

private val DarkVioletScheme = darkColorScheme(
    primary = Color(0xFFD0BCFF),
    onPrimary = Color(0xFF371E73),
    primaryContainer = Color(0xFF4F378B),
    onPrimaryContainer = Color(0xFFEADDFF),
    inversePrimary = Color(0xFF6750A4),
    secondary = Color(0xFFCCC2DC),
    onSecondary = Color(0xFF332D41),
    secondaryContainer = Color(0xFF4A4458),
    onSecondaryContainer = Color(0xFFE8DEF8),
    tertiary = Color(0xFFEFB8C8),
    onTertiary = Color(0xFF492532),
    tertiaryContainer = Color(0xFF633B48),
    onTertiaryContainer = Color(0xFFFFD8E4),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color(0xFF15121B),
    onBackground = Color(0xFFE7E0EB),
    surface = Color(0xFF1E1A24),
    onSurface = Color(0xFFE7E0EB),
    surfaceVariant = Color(0xFF312B3B),
    onSurfaceVariant = Color(0xFFCAC4D0),
    surfaceContainerLowest = Color(0xFF0F0C14),
    surfaceContainerLow = Color(0xFF191520),
    surfaceContainer = Color(0xFF211D28),
    surfaceContainerHigh = Color(0xFF2C2733),
    surfaceContainerHighest = Color(0xFF383240),
    inverseSurface = Color(0xFFE7E0EB),
    inverseOnSurface = Color(0xFF1E1A24),
    outline = Color(0xFF554D5D),
    outlineVariant = Color(0xFF423B49)
)

// 5. Emerald Mint (Botanical Emerald & Jade)
private val LightEmeraldScheme = lightColorScheme(
    primary = Color(0xFF1B6B50),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFAAF2CE),
    onPrimaryContainer = Color(0xFF002116),
    inversePrimary = Color(0xFF86D6B4),
    secondary = Color(0xFF4D6357),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFCFE9D8),
    onSecondaryContainer = Color(0xFF0B1F16),
    tertiary = Color(0xFF3D6466),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFC0E9EB),
    onTertiaryContainer = Color(0xFF002022),
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = Color(0xFFF5FAF7),
    onBackground = Color(0xFF171D1A),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF171D1A),
    surfaceVariant = Color(0xFFDCE5DF),
    onSurfaceVariant = Color(0xFF404944),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFEFF5F1),
    surfaceContainer = Color(0xFFE9F0EC),
    surfaceContainerHigh = Color(0xFFE3EAE6),
    surfaceContainerHighest = Color(0xFFDDE4E0),
    inverseSurface = Color(0xFF2C322E),
    inverseOnSurface = Color(0xFFEEF2EE),
    outline = Color(0xFFD3E3DA),
    outlineVariant = Color(0xFFC0C9C3)
)

private val DarkEmeraldScheme = darkColorScheme(
    primary = Color(0xFF86D6B4),
    onPrimary = Color(0xFF003828),
    primaryContainer = Color(0xFF00513B),
    onPrimaryContainer = Color(0xFFAAF2CE),
    inversePrimary = Color(0xFF1B6B50),
    secondary = Color(0xFFB4CCBE),
    onSecondary = Color(0xFF1F352A),
    secondaryContainer = Color(0xFF364B40),
    onSecondaryContainer = Color(0xFFCFE9D8),
    tertiary = Color(0xFFA4CDCE),
    onTertiary = Color(0xFF053537),
    tertiaryContainer = Color(0xFF244C4E),
    onTertiaryContainer = Color(0xFFC0E9EB),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color(0xFF0E1511),
    onBackground = Color(0xFFDDE4DF),
    surface = Color(0xFF141D18),
    onSurface = Color(0xFFDDE4DF),
    surfaceVariant = Color(0xFF233029),
    onSurfaceVariant = Color(0xFFC0C9C3),
    surfaceContainerLowest = Color(0xFF0A100D),
    surfaceContainerLow = Color(0xFF121915),
    surfaceContainer = Color(0xFF17201B),
    surfaceContainerHigh = Color(0xFF212B26),
    surfaceContainerHighest = Color(0xFF2C3731),
    inverseSurface = Color(0xFFDDE4DF),
    inverseOnSurface = Color(0xFF141D18),
    outline = Color(0xFF43534B),
    outlineVariant = Color(0xFF33423A)
)

// 6. Crimson Ruby (Artisan Wine & Berry)
private val LightCrimsonScheme = lightColorScheme(
    primary = Color(0xFF8F394B),
    onPrimary = Color(0xFFFFFFFF),
    primaryContainer = Color(0xFFFFD9DF),
    onPrimaryContainer = Color(0xFF3B0014),
    inversePrimary = Color(0xFFFFB2BE),
    secondary = Color(0xFF74565F),
    onSecondary = Color(0xFFFFFFFF),
    secondaryContainer = Color(0xFFFFD9E2),
    onSecondaryContainer = Color(0xFF2B151C),
    tertiary = Color(0xFF785848),
    onTertiary = Color(0xFFFFFFFF),
    tertiaryContainer = Color(0xFFFFDCCF),
    onTertiaryContainer = Color(0xFF2C160B),
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = Color(0xFFFDF7F8),
    onBackground = Color(0xFF201A1B),
    surface = Color(0xFFFFFFFF),
    onSurface = Color(0xFF201A1B),
    surfaceVariant = Color(0xFFF2DDE1),
    onSurfaceVariant = Color(0xFF514346),
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFFBF1F3),
    surfaceContainer = Color(0xFFF5EBED),
    surfaceContainerHigh = Color(0xFFEFE5E7),
    surfaceContainerHighest = Color(0xFFE9DFE1),
    inverseSurface = Color(0xFF362E30),
    inverseOnSurface = Color(0xFFFAEEEF),
    outline = Color(0xFFEBD8DC),
    outlineVariant = Color(0xFFD5C2C5)
)

private val DarkCrimsonScheme = darkColorScheme(
    primary = Color(0xFFFFB2BE),
    onPrimary = Color(0xFF560720),
    primaryContainer = Color(0xFF722235),
    onPrimaryContainer = Color(0xFFFFD9DF),
    inversePrimary = Color(0xFF8F394B),
    secondary = Color(0xFFE3BDC7),
    onSecondary = Color(0xFF422931),
    secondaryContainer = Color(0xFF5B3F47),
    onSecondaryContainer = Color(0xFFFFD9E2),
    tertiary = Color(0xFFE7BDAB),
    onTertiary = Color(0xFF442B1D),
    tertiaryContainer = Color(0xFF5D4132),
    onTertiaryContainer = Color(0xFFFFDCCF),
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = Color(0xFF181113),
    onBackground = Color(0xFFEAE0E1),
    surface = Color(0xFF21181A),
    onSurface = Color(0xFFEAE0E1),
    surfaceVariant = Color(0xFF35292C),
    onSurfaceVariant = Color(0xFFD5C2C5),
    surfaceContainerLowest = Color(0xFF120C0D),
    surfaceContainerLow = Color(0xFF1C1416),
    surfaceContainer = Color(0xFF231B1E),
    surfaceContainerHigh = Color(0xFF2E2528),
    surfaceContainerHighest = Color(0xFF393033),
    inverseSurface = Color(0xFFEAE0E1),
    inverseOnSurface = Color(0xFF21181A),
    outline = Color(0xFF5A4B4E),
    outlineVariant = Color(0xFF463A3D)
)

@Composable
fun NoteLedgerTheme(
    themeMode: ThemeMode = ThemeMode.SYSTEM,
    themePalette: ThemePalette = ThemePalette.SAGE_FOREST,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val darkTheme = when (themeMode) {
        ThemeMode.SYSTEM -> isSystemInDarkTheme()
        ThemeMode.LIGHT -> false
        ThemeMode.DARK -> true
    }

    NoteLedgerTheme(
        darkTheme = darkTheme,
        themePalette = themePalette,
        dynamicColor = dynamicColor,
        content = content
    )
}

@Composable
fun NoteLedgerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    themePalette: ThemePalette = ThemePalette.SAGE_FOREST,
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        else -> getPaletteColorScheme(themePalette, darkTheme)
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val insetsController = WindowCompat.getInsetsController(window, view)
                insetsController.isAppearanceLightStatusBars = !darkTheme
                insetsController.isAppearanceLightNavigationBars = !darkTheme
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        shapes = Shapes,
        content = content
    )
}

// Backward-compatible alias
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    NoteLedgerTheme(
        darkTheme = darkTheme,
        themePalette = ThemePalette.SAGE_FOREST,
        dynamicColor = dynamicColor,
        content = content
    )
}


