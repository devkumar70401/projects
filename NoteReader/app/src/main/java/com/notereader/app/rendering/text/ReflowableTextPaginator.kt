package com.notereader.app.rendering.text

import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import androidx.collection.LruCache
import com.notereader.app.domain.model.ReadingSettings
import java.io.File
import java.nio.charset.Charset

data class TextRange(val start: Int, val end: Int)

data class PaginationKey(
    val documentId: Long,
    val fontFamily: String,
    val fontSizeSp: Float,
    val lineSpacingMultiplier: Float,
    val marginDp: Float,
    val viewportWidthPx: Int,
    val viewportHeightPx: Int
)

class ReflowableTextPaginator {

    private val cache = LruCache<PaginationKey, List<TextRange>>(maxSize = 6)

    fun paginate(text: CharSequence, key: PaginationKey, density: Float): List<TextRange> {
        cache.get(key)?.let { return it }
        val result = computePagination(text, key, density)
        cache.put(key, result)
        return result
    }

    private fun computePagination(text: CharSequence, key: PaginationKey, density: Float): List<TextRange> {
        if (text.isEmpty()) return listOf(TextRange(0, 0))

        val paint = TextPaint().apply {
            isAntiAlias = true
            textSize = key.fontSizeSp * density
            typeface = when (key.fontFamily.lowercase()) {
                "serif" -> Typeface.SERIF
                "monospace" -> Typeface.MONOSPACE
                else -> Typeface.SANS_SERIF
            }
        }

        val marginPx = (key.marginDp * density).toInt()
        val contentWidthPx = (key.viewportWidthPx - (marginPx * 2)).coerceAtLeast(200)
        val contentHeightPx = (key.viewportHeightPx - (marginPx * 2)).coerceAtLeast(200)

        val ranges = mutableListOf<TextRange>()
        var cursor = 0
        val length = text.length

        while (cursor < length) {
            val end = findPageBreak(text, cursor, paint, contentWidthPx, contentHeightPx, key.lineSpacingMultiplier)
            ranges.add(TextRange(cursor, end))
            if (end <= cursor) {
                // safeguard
                cursor = (cursor + 1).coerceAtMost(length)
            } else {
                cursor = end
            }
        }
        return ranges
    }

    private fun findPageBreak(
        text: CharSequence,
        start: Int,
        paint: TextPaint,
        widthPx: Int,
        heightPx: Int,
        lineSpacing: Float
    ): Int {
        var lo = start + 1
        var hi = text.length
        var best = lo

        while (lo <= hi) {
            val mid = (lo + hi) / 2
            val candidate = text.subSequence(start, mid)
            val layout = StaticLayout.Builder.obtain(candidate, 0, candidate.length, paint, widthPx)
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(0f, lineSpacing)
                .setIncludePad(false)
                .build()

            if (layout.height <= heightPx) {
                best = mid
                lo = mid + 1
            } else {
                hi = mid - 1
            }
        }

        return snapToSafeBreak(text, start, best)
    }

    private fun snapToSafeBreak(text: CharSequence, start: Int, candidateEnd: Int): Int {
        if (candidateEnd >= text.length) return text.length

        // Look back for paragraph break or whitespace up to 100 characters
        val lookbackLimit = (candidateEnd - 120).coerceAtLeast(start + 1)
        for (i in candidateEnd downTo lookbackLimit) {
            val c = text[i]
            if (c == '\n' || c == '\r') return i + 1
            if (Character.isWhitespace(c)) return i + 1
        }
        return candidateEnd
    }

    fun mapOffsetToPageIndex(offset: Int, ranges: List<TextRange>): Int {
        if (ranges.isEmpty()) return 0
        var low = 0
        var high = ranges.size - 1
        while (low <= high) {
            val mid = (low + high) / 2
            val range = ranges[mid]
            if (offset >= range.start && offset < range.end) {
                return mid
            } else if (offset < range.start) {
                high = mid - 1
            } else {
                low = mid + 1
            }
        }
        return (ranges.size - 1).coerceAtLeast(0)
    }

    fun mapPageIndexToOffset(pageIndex: Int, ranges: List<TextRange>): Int {
        if (ranges.isEmpty() || pageIndex !in ranges.indices) return 0
        return ranges[pageIndex].start
    }
}

object CharsetDetectingReader {
    fun readString(filePath: String, charsetName: String = "UTF-8"): String {
        return try {
            val file = File(filePath)
            if (!file.exists()) return ""
            val charset = runCatching { Charset.forName(charsetName) }.getOrDefault(Charsets.UTF_8)
            file.readText(charset)
        } catch (e: Exception) {
            ""
        }
    }
}
