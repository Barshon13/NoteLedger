package com.example.ui.theme

import android.content.Context
import android.content.SharedPreferences
import androidx.compose.ui.graphics.Color

enum class ThemeMode(val displayName: String) {
    SYSTEM("System default"),
    LIGHT("Light"),
    DARK("Dark")
}

enum class ThemePalette(
    val displayName: String,
    val description: String,
    val primaryColor: Color,
    val secondaryColor: Color,
    val tertiaryColor: Color
) {
    SAGE_FOREST(
        displayName = "Sage Forest",
        description = "Organic eucalyptus & earth stone",
        primaryColor = Color(0xFF4F6354),
        secondaryColor = Color(0xFF705D49),
        tertiaryColor = Color(0xFF57624A)
    ),
    OCEAN_SLATE(
        displayName = "Ocean Slate",
        description = "Deep Nordic navy & coastal sky",
        primaryColor = Color(0xFF2B5B84),
        secondaryColor = Color(0xFF4F6070),
        tertiaryColor = Color(0xFF3B6470)
    ),
    SUNSET_TERRACOTTA(
        displayName = "Sunset Clay",
        description = "Warm terracotta & golden amber",
        primaryColor = Color(0xFF9C412B),
        secondaryColor = Color(0xFF77574E),
        tertiaryColor = Color(0xFF7D5737)
    ),
    ROYAL_VIOLET(
        displayName = "Royal Amethyst",
        description = "Velvet plum & lavender mist",
        primaryColor = Color(0xFF6750A4),
        secondaryColor = Color(0xFF625B71),
        tertiaryColor = Color(0xFF7D5260)
    ),
    EMERALD_MINT(
        displayName = "Emerald Mint",
        description = "Fresh botanical emerald & jade",
        primaryColor = Color(0xFF1B6B50),
        secondaryColor = Color(0xFF4D6357),
        tertiaryColor = Color(0xFF3D6466)
    ),
    CRIMSON_BERRY(
        displayName = "Artisan Ruby",
        description = "Burgundy rose & copper accents",
        primaryColor = Color(0xFF8F394B),
        secondaryColor = Color(0xFF74565F),
        tertiaryColor = Color(0xFF785848)
    )
}

class ThemePreferences(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    var themeMode: ThemeMode
        get() {
            val name = prefs.getString(KEY_THEME_MODE, ThemeMode.SYSTEM.name)
            return try {
                ThemeMode.valueOf(name ?: ThemeMode.SYSTEM.name)
            } catch (e: Exception) {
                ThemeMode.SYSTEM
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_MODE, value.name).apply()
        }

    var themePalette: ThemePalette
        get() {
            val name = prefs.getString(KEY_THEME_PALETTE, ThemePalette.SAGE_FOREST.name)
            return try {
                ThemePalette.valueOf(name ?: ThemePalette.SAGE_FOREST.name)
            } catch (e: Exception) {
                ThemePalette.SAGE_FOREST
            }
        }
        set(value) {
            prefs.edit().putString(KEY_THEME_PALETTE, value.name).apply()
        }

    var dynamicColor: Boolean
        get() = prefs.getBoolean(KEY_DYNAMIC_COLOR, false)
        set(value) {
            prefs.edit().putBoolean(KEY_DYNAMIC_COLOR, value).apply()
        }

    companion object {
        private const val PREFS_NAME = "noteledger_theme_prefs"
        private const val KEY_THEME_MODE = "key_theme_mode"
        private const val KEY_THEME_PALETTE = "key_theme_palette"
        private const val KEY_DYNAMIC_COLOR = "key_dynamic_color"
    }
}
