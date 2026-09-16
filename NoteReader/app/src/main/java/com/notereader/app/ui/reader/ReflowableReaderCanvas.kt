package com.notereader.app.ui.reader

import android.text.Spanned
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.usecase.ReaderRawState
import com.notereader.app.rendering.text.PaginationKey
import com.notereader.app.rendering.text.ReflowableTextPaginator
import com.notereader.app.rendering.text.TextRange

@Composable
fun ReflowableReaderCanvas(
    fullText: CharSequence,
    isMarkdown: Boolean,
    initialOffset: Int,
    settings: ReadingSettings,
    onProgressChange: (ReaderRawState) -> Unit,
    onToggleChrome: () -> Unit
) {
    val paginator = remember { ReflowableTextPaginator() }
    val density = LocalDensity.current.density
    var canvasSize by remember { mutableStateOf(IntSize(1200, 1800)) }

    val paginationKey = remember(settings, canvasSize) {
        PaginationKey(
            documentId = 0,
            fontFamily = settings.fontFamily,
            fontSizeSp = settings.fontSizeSp,
            lineSpacingMultiplier = settings.lineSpacingMultiplier,
            marginDp = settings.marginDp,
            viewportWidthPx = canvasSize.width.coerceAtLeast(300),
            viewportHeightPx = canvasSize.height.coerceAtLeast(300)
        )
    }

    val pageRanges = remember(fullText, paginationKey, density) {
        paginator.paginate(fullText, paginationKey, density)
    }

    var currentPageIndex by remember(pageRanges) {
        val mapped = paginator.mapOffsetToPageIndex(initialOffset, pageRanges)
        mutableStateOf(mapped.coerceIn(0, (pageRanges.size - 1).coerceAtLeast(0)))
    }

    LaunchedEffect(currentPageIndex, pageRanges) {
        if (pageRanges.isNotEmpty() && currentPageIndex in pageRanges.indices) {
            val range = pageRanges[currentPageIndex]
            onProgressChange(
                ReaderRawState(
                    charOffset = range.start,
                    totalCharCount = fullText.length
                )
            )
        }
    }

    val bgColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.backgroundHex)) }
        .getOrDefault(Color.White)
    val textColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.textHex)) }
        .getOrDefault(Color.Black)

    val currentText = remember(currentPageIndex, pageRanges, fullText) {
        if (pageRanges.isNotEmpty() && currentPageIndex in pageRanges.indices) {
            val range = pageRanges[currentPageIndex]
            fullText.subSequence(range.start, range.end).toString()
        } else ""
    }

    val font = when (settings.fontFamily.lowercase()) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .onSizeChanged { canvasSize = it }
            .pointerInput(pageRanges.size) {
                detectTapGestures { offset ->
                    val width = size.width
                    val x = offset.x
                    when {
                        x < width * 0.30f -> {
                            if (currentPageIndex > 0) currentPageIndex--
                        }
                        x > width * 0.70f -> {
                            if (currentPageIndex < pageRanges.size - 1) currentPageIndex++
                        }
                        else -> {
                            onToggleChrome()
                        }
                    }
                }
            }
            .padding(horizontal = settings.marginDp.dp, vertical = (settings.marginDp + 16).dp)
    ) {
        Text(
            text = currentText,
            color = textColor,
            fontSize = settings.fontSizeSp.sp,
            lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp,
            fontFamily = font,
            modifier = Modifier.fillMaxSize()
        )
    }
}
