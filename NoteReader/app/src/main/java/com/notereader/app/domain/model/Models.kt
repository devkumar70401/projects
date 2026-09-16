package com.notereader.app.domain.model

import androidx.compose.runtime.Immutable
import androidx.compose.runtime.Stable

@Immutable
data class Notebook(
    val id: Long = 0,
    val title: String,
    val colorHex: String,
    val sortOrder: Int,
    val createdAt: Long,
    val updatedAt: Long,
    val pageCount: Int = 0
)

@Immutable
data class Section(
    val id: Long = 0,
    val notebookId: Long,
    val title: String,
    val colorHex: String,
    val sortOrder: Int,
    val createdAt: Long,
    val pageCount: Int = 0
)

@Immutable
data class Page(
    val id: Long = 0,
    val sectionId: Long,
    val title: String,
    val format: DocumentFormat,
    val internalFilePath: String,
    val thumbnailPath: String?,
    val originalFileName: String,
    val fileSizeBytes: Long,
    val pageOrCellCount: Int?,
    val sortOrder: Int,
    val importedAt: Long,
    val lastOpenedAt: Long?,
    val charset: String = "UTF-8"
)

@Immutable
data class PageWithProgress(
    val page: Page,
    val progressFraction: Float = 0f,
    val positionToken: String? = null
)

@Immutable
data class Bookmark(
    val id: Long = 0,
    val pageId: Long,
    val positionToken: String,
    val label: String?,
    val createdAt: Long
)

@Immutable
data class Annotation(
    val id: Long = 0,
    val pageId: Long,
    val type: String, // "HIGHLIGHT" | "INK"
    val positionToken: String,
    val colorHex: String,
    val textSnippet: String? = null,
    val inkPathData: String? = null,
    val createdAt: Long
)

@Immutable
enum class ReadingTheme(
    val id: String,
    val displayName: String,
    val backgroundHex: String,
    val surfaceHex: String,
    val textHex: String,
    val accentHex: String,
    val codeBackgroundHex: String,
    val isDark: Boolean
) {
    LIGHT("light", "Light", "#FFFFFF", "#F8FAFC", "#0F172A", "#2563EB", "#F1F5F9", false),
    SEPIA("sepia", "Sepia", "#FBF0D9", "#F3E5C8", "#433422", "#9A6B38", "#EDE0C4", false),
    SOLARIZED("solarized", "Solarized", "#FDF6E3", "#EEE8D5", "#657B83", "#268BD2", "#E8DFCA", false),
    DARK("dark", "Dark", "#1E293B", "#0F172A", "#E2E8F0", "#38BDF8", "#334155", true),
    AMOLED("amoled", "AMOLED Black", "#000000", "#121212", "#E5E5E5", "#60A5FA", "#1E1E1E", true),
    NORD("nord", "Nord", "#2E3440", "#3B4252", "#ECEFF4", "#88C0D0", "#434C5E", true),
    FOREST("forest", "Forest", "#1A2E26", "#243E34", "#D8EADF", "#52B788", "#2D4C40", true)
}

enum class PageTurnStyle(val displayName: String) {
    SLIDE("Slide"),
    FADE("Fade"),
    NONE("Continuous Scroll")
}

@Immutable
data class ReadingSettings(
    val theme: ReadingTheme = ReadingTheme.LIGHT,
    val fontFamily: String = "SansSerif", // "SansSerif", "Serif", "Monospace"
    val fontSizeSp: Float = 18f,
    val lineSpacingMultiplier: Float = 1.35f,
    val marginDp: Float = 24f,
    val pageTurnStyle: PageTurnStyle = PageTurnStyle.SLIDE,
    val keepScreenOn: Boolean = true,
    val sPenInkColor: String = "#EF4444",
    val sPenStrokeWidth: Float = 3f
)
