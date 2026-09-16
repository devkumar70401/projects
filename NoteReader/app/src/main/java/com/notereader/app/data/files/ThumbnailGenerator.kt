package com.notereader.app.data.files

import android.content.Context
import android.graphics.*
import android.graphics.pdf.PdfRenderer
import android.os.ParcelFileDescriptor
import android.text.Layout
import android.text.StaticLayout
import android.text.TextPaint
import com.notereader.app.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream

class ThumbnailGenerator(private val context: Context) {

    private val thumbnailsDir: File
        get() = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }

    private val density: Float
        get() = context.resources.displayMetrics.density

    // Master thumbnail physical dimensions (sized for large tablet cards at native density)
    private val masterWidthPx: Int
        get() = (280f * density).toInt().coerceAtLeast(560)

    private val masterHeightPx: Int
        get() = (360f * density).toInt().coerceAtLeast(720)

    suspend fun generate(pageId: Long, filePath: String, format: DocumentFormat): String? = withContext(Dispatchers.IO) {
        try {
            val file = File(filePath)
            if (!file.exists()) return@withContext null

            val destFile = File(thumbnailsDir, "$pageId.jpg")
            val bitmap = when (format) {
                DocumentFormat.PDF -> generatePdfThumbnail(file)
                DocumentFormat.TXT, DocumentFormat.MARKDOWN -> generateTextThumbnail(file)
                DocumentFormat.IPYNB -> generateIpynbThumbnail(file)
            } ?: return@withContext null

            FileOutputStream(destFile).use { out ->
                // 95% quality preserves high-frequency text anti-aliasing without blur
                bitmap.compress(Bitmap.CompressFormat.JPEG, 95, out)
            }
            bitmap.recycle()
            destFile.absolutePath
        } catch (e: Exception) {
            null
        }
    }

    private fun generatePdfThumbnail(file: File): Bitmap? {
        var pfd: ParcelFileDescriptor? = null
        var renderer: PdfRenderer? = null
        var page: PdfRenderer.Page? = null
        return try {
            pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
            renderer = PdfRenderer(pfd)
            if (renderer.pageCount == 0) return null
            page = renderer.openPage(0)

            val targetW = masterWidthPx
            val targetH = (targetW.toFloat() * (page.height.toFloat() / page.width.toFloat()))
                .toInt()
                .coerceIn(masterWidthPx / 2, masterHeightPx * 2)

            val bitmap = Bitmap.createBitmap(targetW, targetH, Bitmap.Config.ARGB_8888)
            bitmap.eraseColor(Color.WHITE)
            page.render(bitmap, null, null, PdfRenderer.Page.RENDER_MODE_FOR_DISPLAY)
            bitmap
        } catch (e: Exception) {
            null
        } finally {
            try { page?.close() } catch (_: Exception) {}
            try { renderer?.close() } catch (_: Exception) {}
            try { pfd?.close() } catch (_: Exception) {}
        }
    }

    private fun generateTextThumbnail(file: File): Bitmap? {
        return try {
            val snippet = file.readText().take(500)
            val width = masterWidthPx
            val height = masterHeightPx
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.parseColor("#F9F9F7"))

            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
                color = Color.parseColor("#1A1C1B")
                textSize = 14f * density
                typeface = Typeface.create("sans-serif", Typeface.NORMAL)
            }

            val padding = (16f * density).toInt()
            val layout = StaticLayout.Builder.obtain(snippet, 0, snippet.length, paint, width - (padding * 2))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f * density, 1.15f)
                .build()

            canvas.save()
            canvas.translate(padding.toFloat(), padding.toFloat())
            layout.draw(canvas)
            canvas.restore()
            bitmap
        } catch (e: Exception) {
            null
        }
    }

    private fun generateIpynbThumbnail(file: File): Bitmap? {
        return try {
            val raw = file.readText().take(800)
            val snippet = if (raw.contains("\"source\"")) {
                "Jupyter Notebook\n\n" + raw.substringAfter("\"source\":").take(300)
                    .replace("[\"", "")
                    .replace("\"]", "")
                    .replace("\\n", "\n")
            } else "Jupyter Notebook"

            val width = masterWidthPx
            val height = masterHeightPx
            val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
            val canvas = Canvas(bitmap)
            canvas.drawColor(Color.parseColor("#0F172A"))

            val paint = TextPaint(Paint.ANTI_ALIAS_FLAG or Paint.SUBPIXEL_TEXT_FLAG or Paint.LINEAR_TEXT_FLAG).apply {
                color = Color.parseColor("#38BDF8")
                textSize = 13.5f * density
                typeface = Typeface.create("monospace", Typeface.NORMAL)
            }

            val padding = (16f * density).toInt()
            val layout = StaticLayout.Builder.obtain(snippet, 0, snippet.length, paint, width - (padding * 2))
                .setAlignment(Layout.Alignment.ALIGN_NORMAL)
                .setLineSpacing(4f * density, 1.2f)
                .build()

            canvas.save()
            canvas.translate(padding.toFloat(), padding.toFloat())
            layout.draw(canvas)
            canvas.restore()
            bitmap
        } catch (e: Exception) {
            null
        }
    }
}
