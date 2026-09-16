package com.notereader.app.rendering.pdf

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import androidx.collection.LruCache
import kotlinx.coroutines.*
import java.io.Closeable
import java.io.File

class PdfDocumentSession(private val file: File) : Closeable {

    private var renderer: PdfRenderer? = null
    private var fileDescriptor: ParcelFileDescriptor? = null

    // 5-page LRU cache with automatic bitmap recycling to bound memory usage
    private val bitmapCache = object : LruCache<Int, Bitmap>(5) {
        override fun entryRemoved(evicted: Boolean, key: Int, oldValue: Bitmap, newValue: Bitmap?) {
            if (oldValue != newValue && !oldValue.isRecycled) {
                oldValue.recycle()
            }
        }
    }

    suspend fun open() = withContext(Dispatchers.IO) {
        fileDescriptor = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
        renderer = PdfRenderer(fileDescriptor!!)
    }

    fun pageCount(): Int = renderer?.pageCount ?: 0

    suspend fun renderPage(index: Int, targetWidthPx: Int, targetHeightPx: Int): Bitmap? =
        withContext(Dispatchers.Default) {
            if (index < 0 || index >= pageCount()) return@withContext null
            bitmapCache.get(index)?.let { return@withContext it }

            synchronized(this@PdfDocumentSession) {
                val r = renderer ?: return@withContext null
                var page: PdfRenderer.Page? = null
                try {
                    page = r.openPage(index)
                    val pageWidth = page.width
                    val pageHeight = page.height

                    // Calculate scale to fill the physical viewport at 100% native resolution
                    val scale = minOf(
                        targetWidthPx.toFloat() / pageWidth,
                        targetHeightPx.toFloat() / pageHeight
                    ).coerceIn(0.2f, 6.0f)

                    val bmpW = (pageWidth * scale).toInt().coerceAtLeast(300)
                    val bmpH = (pageHeight * scale).toInt().coerceAtLeast(300)

                    // ARGB_8888 ensures anti-aliased font rendering and smooth gradients
                    val bitmap = Bitmap.createBitmap(bmpW, bmpH, Bitmap.Config.ARGB_8888)
                    bitmap.eraseColor(Color.WHITE)
                    page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)

                    bitmapCache.put(index, bitmap)
                    bitmap
                } catch (e: Exception) {
                    null
                } finally {
                    try { page?.close() } catch (_: Exception) {}
                }
            }
        }

    fun prefetchAround(currentIndex: Int, targetWidthPx: Int, targetHeightPx: Int, scope: CoroutineScope) {
        scope.launch(Dispatchers.Default) {
            if (currentIndex > 0) renderPage(currentIndex - 1, targetWidthPx, targetHeightPx)
            if (currentIndex < pageCount() - 1) renderPage(currentIndex + 1, targetWidthPx, targetHeightPx)
        }
    }

    override fun close() {
        bitmapCache.evictAll()
        try { renderer?.close() } catch (_: Exception) {}
        try { fileDescriptor?.close() } catch (_: Exception) {}
    }
}
