package com.notereader.app.data.files

import android.content.Context
import android.graphics.pdf.PdfRenderer
import android.net.Uri
import android.os.ParcelFileDescriptor
import com.notereader.app.domain.model.DocumentFormat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.mozilla.universalchardet.UniversalDetector
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID

data class CopyResult(
    val internalPath: String,
    val originalFileName: String,
    val suggestedTitle: String,
    val claimedExtension: String,
    val sizeBytes: Long
)

data class QuickMetadata(
    val pageOrCellCount: Int?,
    val detectedCharset: String = "UTF-8"
)

class FileImportManager(private val context: Context) {

    private val pagesDir: File
        get() = File(context.filesDir, "pages").apply { if (!exists()) mkdirs() }

    private val thumbnailsDir: File
        get() = File(context.filesDir, "thumbnails").apply { if (!exists()) mkdirs() }

    suspend fun copyIntoInternalStorage(sourceUri: Uri): Result<CopyResult> = withContext(Dispatchers.IO) {
        runCatching {
            var fileName = "document"
            var extension = "txt"

            context.contentResolver.query(sourceUri, null, null, null, null)?.use { cursor ->
                val nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
                if (nameIndex != -1 && cursor.moveToFirst()) {
                    val full = cursor.getString(nameIndex)
                    if (!full.isNullOrBlank()) {
                        fileName = full
                        extension = full.substringAfterLast(".", "txt")
                    }
                }
            }

            val uuid = UUID.randomUUID().toString()
            val destFile = File(pagesDir, "$uuid.$extension")

            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                FileOutputStream(destFile).use { output ->
                    input.copyTo(output)
                }
            } ?: throw IllegalStateException("Cannot open input stream for URI: $sourceUri")

            val title = fileName.substringBeforeLast(".")
            CopyResult(
                internalPath = destFile.absolutePath,
                originalFileName = fileName,
                suggestedTitle = title.ifBlank { "Untitled" },
                claimedExtension = extension,
                sizeBytes = destFile.length()
            )
        }
    }

    suspend fun verifyFormat(filePath: String): DocumentFormat? = withContext(Dispatchers.IO) {
        val file = File(filePath)
        if (!file.exists() || file.length() == 0L) return@withContext null

        val ext = file.extension.lowercase()
        when (ext) {
            "pdf" -> {
                try {
                    val bytes = ByteArray(5)
                    FileInputStream(file).use { it.read(bytes) }
                    val header = String(bytes)
                    if (header.startsWith("%PDF")) DocumentFormat.PDF else null
                } catch (e: Exception) {
                    null
                }
            }
            "ipynb" -> {
                try {
                    val head = file.readText().take(500)
                    if (head.contains("cells") || head.contains("nbformat")) DocumentFormat.IPYNB else null
                } catch (e: Exception) {
                    null
                }
            }
            "md", "markdown" -> DocumentFormat.MARKDOWN
            "txt" -> DocumentFormat.TXT
            else -> DocumentFormat.fromExtension(ext)
        }
    }

    suspend fun detectCharset(file: File): String = withContext(Dispatchers.IO) {
        try {
            val buf = ByteArray(4096)
            FileInputStream(file).use { fis ->
                val detector = UniversalDetector(null)
                var nread: Int
                while (fis.read(buf).also { nread = it } > 0 && !detector.isDone) {
                    detector.handleData(buf, 0, nread)
                }
                detector.dataEnd()
                detector.detectedCharset ?: "UTF-8"
            }
        } catch (e: Exception) {
            "UTF-8"
        }
    }

    suspend fun extractQuickMetadata(filePath: String, format: DocumentFormat): QuickMetadata = withContext(Dispatchers.IO) {
        val file = File(filePath)
        when (format) {
            DocumentFormat.PDF -> {
                var pfd: ParcelFileDescriptor? = null
                var renderer: PdfRenderer? = null
                try {
                    pfd = ParcelFileDescriptor.open(file, ParcelFileDescriptor.MODE_READ_ONLY)
                    renderer = PdfRenderer(pfd)
                    QuickMetadata(pageOrCellCount = renderer.pageCount)
                } catch (e: Exception) {
                    QuickMetadata(pageOrCellCount = null)
                } finally {
                    try { renderer?.close() } catch (_: Exception) {}
                    try { pfd?.close() } catch (_: Exception) {}
                }
            }
            DocumentFormat.IPYNB -> {
                try {
                    val text = file.readText()
                    // rough quick count of cells
                    val count = text.split("\"cell_type\"").size - 1
                    QuickMetadata(pageOrCellCount = count.coerceAtLeast(1))
                } catch (e: Exception) {
                    QuickMetadata(pageOrCellCount = null)
                }
            }
            DocumentFormat.TXT -> {
                val charset = detectCharset(file)
                QuickMetadata(pageOrCellCount = null, detectedCharset = charset)
            }
            DocumentFormat.MARKDOWN -> {
                QuickMetadata(pageOrCellCount = null, detectedCharset = "UTF-8")
            }
        }
    }

    fun deleteFile(path: String) {
        try {
            val file = File(path)
            if (file.exists()) file.delete()
        } catch (_: Exception) {}
    }
}
