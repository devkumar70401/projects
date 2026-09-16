package com.notereader.app.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.gestures.detectTransformGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.*
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.model.ReadingTheme
import com.notereader.app.domain.usecase.ReaderRawState
import com.notereader.app.rendering.pdf.PdfDocumentSession
import kotlinx.coroutines.launch

@Composable
fun PdfReaderCanvas(
    session: PdfDocumentSession,
    totalPages: Int,
    initialPage: Int,
    settings: ReadingSettings,
    onProgressChange: (ReaderRawState) -> Unit,
    onToggleChrome: () -> Unit
) {
    var currentPageIndex by remember { mutableStateOf(initialPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    val coroutineScope = rememberCoroutineScope()
    val density = LocalDensity.current

    // GPU-accelerated pinch zoom & pan state
    var zoomScale by remember { mutableFloatStateOf(1f) }
    var panOffsetX by remember { mutableFloatStateOf(0f) }
    var panOffsetY by remember { mutableFloatStateOf(0f) }

    // Color matrix filter for themes
    val colorFilter = remember(settings.theme) {
        when (settings.theme) {
            ReadingTheme.DARK, ReadingTheme.AMOLED, ReadingTheme.NORD, ReadingTheme.FOREST -> {
                val matrix = ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingTheme.SEPIA -> {
                val matrix = ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, 20f,
                        0f, 0.85f, 0f, 0f, 10f,
                        0f, 0f, 0.7f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            else -> null
        }
    }

    val bgColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.backgroundHex)) }
        .getOrDefault(Color.White)

    BoxWithConstraints(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor),
        contentAlignment = Alignment.Center
    ) {
        val targetWidthPx = with(density) { maxWidth.roundToPx() }
        val targetHeightPx = with(density) { maxHeight.roundToPx() }

        LaunchedEffect(currentPageIndex, targetWidthPx, targetHeightPx) {
            if (targetWidthPx > 0 && targetHeightPx > 0) {
                val bmp = session.renderPage(currentPageIndex, targetWidthPx, targetHeightPx)
                currentBitmap = bmp
                session.prefetchAround(currentPageIndex, targetWidthPx, targetHeightPx, coroutineScope)
                onProgressChange(
                    ReaderRawState(
                        pdfPageIndex = currentPageIndex,
                        totalPdfPages = totalPages
                    )
                )
            }
        }

        Box(
            modifier = Modifier
                .fillMaxSize()
                .graphicsLayer {
                    scaleX = zoomScale
                    scaleY = zoomScale
                    translationX = panOffsetX
                    translationY = panOffsetY
                }
                .pointerInput(Unit) {
                    detectTransformGestures { _, pan, zoom, _ ->
                        zoomScale = (zoomScale * zoom).coerceIn(1f, 4f)
                        if (zoomScale > 1f) {
                            val maxPanX = (targetWidthPx * (zoomScale - 1f)) / 2f
                            val maxPanY = (targetHeightPx * (zoomScale - 1f)) / 2f
                            panOffsetX = (panOffsetX + pan.x).coerceIn(-maxPanX, maxPanX)
                            panOffsetY = (panOffsetY + pan.y).coerceIn(-maxPanY, maxPanY)
                        } else {
                            panOffsetX = 0f
                            panOffsetY = 0f
                        }
                    }
                }
                .pointerInput(totalPages) {
                    detectTapGestures(
                        onDoubleTap = {
                            if (zoomScale > 1.2f) {
                                zoomScale = 1f
                                panOffsetX = 0f
                                panOffsetY = 0f
                            } else {
                                zoomScale = 2.2f
                            }
                        },
                        onTap = { offset ->
                            val width = size.width
                            val x = offset.x
                            when {
                                x < width * 0.30f -> {
                                    if (currentPageIndex > 0) {
                                        currentPageIndex--
                                        zoomScale = 1f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                }
                                x > width * 0.70f -> {
                                    if (currentPageIndex < totalPages - 1) {
                                        currentPageIndex++
                                        zoomScale = 1f
                                        panOffsetX = 0f
                                        panOffsetY = 0f
                                    }
                                }
                                else -> {
                                    onToggleChrome()
                                }
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            val bmp = currentBitmap
            if (bmp != null) {
                Image(
                    bitmap = bmp.asImageBitmap(),
                    contentDescription = "PDF Page ${currentPageIndex + 1}",
                    colorFilter = colorFilter,
                    contentScale = ContentScale.Fit,
                    filterQuality = FilterQuality.High,
                    modifier = Modifier.fillMaxSize()
                )
            } else {
                CircularProgressIndicator(
                    color = MaterialTheme.colorScheme.primary,
                    strokeWidth = 3.dp
                )
            }
        }
    }
}
