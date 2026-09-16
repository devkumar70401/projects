package com.notereader.app.data.repository

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.notereader.app.domain.model.PageTurnStyle
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.model.ReadingTheme
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "reading_settings")

class SettingsRepository(private val context: Context) {

    private object Keys {
        val THEME_ID = stringPreferencesKey("theme_id")
        val FONT_FAMILY = stringPreferencesKey("font_family")
        val FONT_SIZE = floatPreferencesKey("font_size")
        val LINE_SPACING = floatPreferencesKey("line_spacing")
        val MARGIN_DP = floatPreferencesKey("margin_dp")
        val PAGE_TURN_STYLE = stringPreferencesKey("page_turn_style")
        val KEEP_SCREEN_ON = booleanPreferencesKey("keep_screen_on")
        val INK_COLOR = stringPreferencesKey("ink_color")
        val INK_WIDTH = floatPreferencesKey("ink_width")
    }

    val readingSettingsFlow: Flow<ReadingSettings> = context.dataStore.data.map { prefs ->
        val themeId = prefs[Keys.THEME_ID] ?: ReadingTheme.LIGHT.id
        val theme = ReadingTheme.values().find { it.id == themeId } ?: ReadingTheme.LIGHT
        val turnStyleName = prefs[Keys.PAGE_TURN_STYLE] ?: PageTurnStyle.SLIDE.name
        val turnStyle = runCatching { PageTurnStyle.valueOf(turnStyleName) }.getOrDefault(PageTurnStyle.SLIDE)

        ReadingSettings(
            theme = theme,
            fontFamily = prefs[Keys.FONT_FAMILY] ?: "SansSerif",
            fontSizeSp = prefs[Keys.FONT_SIZE] ?: 18f,
            lineSpacingMultiplier = prefs[Keys.LINE_SPACING] ?: 1.35f,
            marginDp = prefs[Keys.MARGIN_DP] ?: 24f,
            pageTurnStyle = turnStyle,
            keepScreenOn = prefs[Keys.KEEP_SCREEN_ON] ?: true,
            sPenInkColor = prefs[Keys.INK_COLOR] ?: "#EF4444",
            sPenStrokeWidth = prefs[Keys.INK_WIDTH] ?: 3f
        )
    }

    suspend fun updateSettings(settings: ReadingSettings) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_ID] = settings.theme.id
            prefs[Keys.FONT_FAMILY] = settings.fontFamily
            prefs[Keys.FONT_SIZE] = settings.fontSizeSp
            prefs[Keys.LINE_SPACING] = settings.lineSpacingMultiplier
            prefs[Keys.MARGIN_DP] = settings.marginDp
            prefs[Keys.PAGE_TURN_STYLE] = settings.pageTurnStyle.name
            prefs[Keys.KEEP_SCREEN_ON] = settings.keepScreenOn
            prefs[Keys.INK_COLOR] = settings.sPenInkColor
            prefs[Keys.INK_WIDTH] = settings.sPenStrokeWidth
        }
    }
}
