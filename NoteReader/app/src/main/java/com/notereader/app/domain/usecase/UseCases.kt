package com.notereader.app.domain.usecase

import android.content.Context
import android.net.Uri
import com.notereader.app.data.db.AppDatabase
import com.notereader.app.data.db.entities.ReadingProgressEntity
import com.notereader.app.data.files.FileImportManager
import com.notereader.app.data.files.ThumbnailGenerator
import com.notereader.app.data.repository.PageRepository
import com.notereader.app.domain.model.DocumentFormat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class ImportFileAsPageUseCase(
    private val fileImportManager: FileImportManager,
    private val pageRepository: PageRepository,
    private val thumbnailGenerator: ThumbnailGenerator,
    private val applicationScope: CoroutineScope
) {
    suspend operator fun invoke(sectionId: Long, sourceUri: Uri): Result<Long> = withContext(Dispatchers.IO) {
        val copyResult = fileImportManager.copyIntoInternalStorage(sourceUri)
            .getOrElse { return@withContext Result.failure(it) }

        val verifiedFormat = fileImportManager.verifyFormat(copyResult.internalPath)
        if (verifiedFormat == null) {
            fileImportManager.deleteFile(copyResult.internalPath)
            return@withContext Result.failure(
                IllegalArgumentException("Invalid or unsupported file format (${copyResult.claimedExtension})")
            )
        }

        val metadata = fileImportManager.extractQuickMetadata(copyResult.internalPath, verifiedFormat)

        val pageId = pageRepository.insertPage(
            sectionId = sectionId,
            title = copyResult.suggestedTitle,
            originalFileName = copyResult.originalFileName,
            format = verifiedFormat,
            filePath = copyResult.internalPath,
            thumbnailPath = null,
            fileSizeBytes = copyResult.sizeBytes,
            pageOrCellCount = metadata.pageOrCellCount,
            charset = metadata.detectedCharset
        )

        // Launch thumbnail generation asynchronously in app scope
        applicationScope.launch(Dispatchers.Default) {
            val thumbPath = thumbnailGenerator.generate(pageId, copyResult.internalPath, verifiedFormat)
            pageRepository.updateThumbnailPath(pageId, thumbPath)
        }

        Result.success(pageId)
    }
}

data class ReaderRawState(
    val pdfPageIndex: Int = 0,
    val totalPdfPages: Int = 1,
    val zoom: Float = 1.0f,
    val scrollY: Float = 0f,
    val charOffset: Int = 0,
    val totalCharCount: Int = 1,
    val cellIndex: Int = 0,
    val totalCellCount: Int = 1,
    val scrollWithinCell: Float = 0f
)

class SaveReadingProgressUseCase(private val db: AppDatabase) {
    private val progressDao = db.readingProgressDao()

    suspend operator fun invoke(pageId: Long, format: DocumentFormat, rawState: ReaderRawState) = withContext(Dispatchers.IO) {
        val positionToken = when (format) {
            DocumentFormat.PDF -> "pdf:page=${rawState.pdfPageIndex};zoom=${rawState.zoom};scrollY=${rawState.scrollY}"
            DocumentFormat.TXT, DocumentFormat.MARKDOWN -> "text:charOffset=${rawState.charOffset}"
            DocumentFormat.IPYNB -> "ipynb:cellIndex=${rawState.cellIndex};scrollWithinCellPx=${rawState.scrollWithinCell}"
        }
        val fraction = when (format) {
            DocumentFormat.PDF -> rawState.pdfPageIndex.toFloat() / rawState.totalPdfPages.coerceAtLeast(1)
            DocumentFormat.TXT, DocumentFormat.MARKDOWN -> rawState.charOffset.toFloat() / rawState.totalCharCount.coerceAtLeast(1)
            DocumentFormat.IPYNB -> rawState.cellIndex.toFloat() / rawState.totalCellCount.coerceAtLeast(1)
        }.coerceIn(0f, 1f)

        progressDao.upsert(
            ReadingProgressEntity(
                pageId = pageId,
                progressFraction = fraction,
                positionToken = positionToken,
                updatedAt = System.currentTimeMillis()
            )
        )
    }
}

sealed class SearchResultItem {
    data class NotebookMatch(val id: Long, val title: String, val colorHex: String) : SearchResultItem()
    data class SectionMatch(val id: Long, val notebookId: Long, val title: String, val colorHex: String) : SearchResultItem()
    data class PageMatch(val id: Long, val sectionId: Long, val title: String, val format: DocumentFormat) : SearchResultItem()
}

class SearchLibraryUseCase(private val db: AppDatabase) {
    suspend operator fun invoke(query: String): List<SearchResultItem> = withContext(Dispatchers.IO) {
        if (query.isBlank()) return@withContext emptyList()
        val q = query.trim()
        val results = mutableListOf<SearchResultItem>()

        val pages = db.pageDao().searchPages(q)
        pages.forEach { p ->
            results.add(SearchResultItem.PageMatch(p.id, p.sectionId, p.title, DocumentFormat.valueOf(p.format)))
        }

        results
    }
}
