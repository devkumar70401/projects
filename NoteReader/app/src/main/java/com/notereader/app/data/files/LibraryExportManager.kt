package com.notereader.app.data.files

import android.content.Context
import android.net.Uri
import com.notereader.app.data.db.AppDatabase
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.*
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class LibraryExportManager(
    private val context: Context,
    private val db: AppDatabase
) {
    suspend fun exportFullLibrary(): Result<File> = withContext(Dispatchers.IO) {
        runCatching {
            val exportDir = File(context.cacheDir, "exports").apply { if (!exists()) mkdirs() }
            val zipFile = File(exportDir, "NoteReader_Backup_${System.currentTimeMillis()}.zip")

            ZipOutputStream(BufferedOutputStream(FileOutputStream(zipFile))).use { zos ->
                // 1. Export Pages dir
                val pagesDir = File(context.filesDir, "pages")
                if (pagesDir.exists()) {
                    pagesDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            addFileToZip(zos, file, "pages/${file.name}")
                        }
                    }
                }

                // 2. Export Thumbnails dir
                val thumbsDir = File(context.filesDir, "thumbnails")
                if (thumbsDir.exists()) {
                    thumbsDir.listFiles()?.forEach { file ->
                        if (file.isFile) {
                            addFileToZip(zos, file, "thumbnails/${file.name}")
                        }
                    }
                }
            }

            zipFile
        }
    }

    private fun addFileToZip(zos: ZipOutputStream, file: File, entryName: String) {
        FileInputStream(file).use { fis ->
            val entry = ZipEntry(entryName)
            zos.putNextEntry(entry)
            fis.copyTo(zos)
            zos.closeEntry()
        }
    }
}
