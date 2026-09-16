package com.notereader.app.data.repository

import android.content.Context
import com.notereader.app.data.db.AppDatabase
import com.notereader.app.data.db.entities.BookmarkEntity
import com.notereader.app.data.db.entities.NotebookEntity
import com.notereader.app.data.db.entities.PageEntity
import com.notereader.app.data.db.entities.ReadingProgressEntity
import com.notereader.app.data.db.entities.SectionEntity
import com.notereader.app.domain.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import java.io.File

class NotebookRepository(private val db: AppDatabase) {
    private val notebookDao = db.notebookDao()
    private val pageDao = db.pageDao()

    fun observeAllNotebooks(): Flow<List<Notebook>> {
        return notebookDao.observeAll().map { notebooks ->
            notebooks.map { nb ->
                Notebook(
                    id = nb.id,
                    title = nb.title,
                    colorHex = nb.colorHex,
                    sortOrder = nb.sortOrder,
                    createdAt = nb.createdAt,
                    updatedAt = nb.updatedAt,
                    pageCount = 0
                )
            }
        }
    }

    suspend fun createNotebook(title: String, colorHex: String): Long {
        val maxOrder = notebookDao.getMaxSortOrder() ?: 0
        val now = System.currentTimeMillis()
        return notebookDao.insert(
            NotebookEntity(
                title = title.trim(),
                colorHex = colorHex,
                sortOrder = maxOrder + 1,
                createdAt = now,
                updatedAt = now
            )
        )
    }

    suspend fun renameNotebook(id: Long, newTitle: String) {
        notebookDao.rename(id, newTitle.trim(), System.currentTimeMillis())
    }

    suspend fun deleteNotebook(id: Long) {
        val sections = db.sectionDao().getAllByNotebook(id)
        for (sec in sections) {
            val pages = db.pageDao().getAllBySection(sec.id)
            for (page in pages) {
                try {
                    File(page.internalFilePath).let { if (it.exists()) it.delete() }
                    page.thumbnailPath?.let { File(it).let { t -> if (t.exists()) t.delete() } }
                } catch (_: Exception) {}
            }
        }
        notebookDao.deleteById(id)
    }
}

class SectionRepository(private val db: AppDatabase) {
    private val sectionDao = db.sectionDao()

    fun observeSectionsForNotebook(notebookId: Long): Flow<List<Section>> {
        return sectionDao.observeByNotebook(notebookId).map { sections ->
            sections.map { sec ->
                Section(
                    id = sec.id,
                    notebookId = sec.notebookId,
                    title = sec.title,
                    colorHex = sec.colorHex,
                    sortOrder = sec.sortOrder,
                    createdAt = sec.createdAt,
                    pageCount = 0
                )
            }
        }
    }

    suspend fun createSection(notebookId: Long, title: String, colorHex: String): Long {
        val maxOrder = sectionDao.getMaxSortOrder(notebookId) ?: 0
        val cleanTitle = title.trim().ifEmpty { "New Section" }
        return sectionDao.insert(
            SectionEntity(
                notebookId = notebookId,
                title = cleanTitle,
                colorHex = colorHex,
                sortOrder = maxOrder + 1,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun renameSection(id: Long, newTitle: String) {
        sectionDao.rename(id, newTitle.trim())
    }

    suspend fun deleteSection(id: Long) {
        val pages = db.pageDao().getAllBySection(id)
        for (page in pages) {
            try {
                File(page.internalFilePath).let { if (it.exists()) it.delete() }
                page.thumbnailPath?.let { File(it).let { t -> if (t.exists()) t.delete() } }
            } catch (_: Exception) {}
        }
        sectionDao.deleteById(id)
    }
}

class PageRepository(
    private val db: AppDatabase,
    private val context: Context
) {
    private val pageDao = db.pageDao()
    private val progressDao = db.readingProgressDao()

    fun observePagesForSection(sectionId: Long): Flow<List<PageWithProgress>> {
        return combine(pageDao.observeBySection(sectionId), progressDao.observeAll()) { pages, progresses ->
            val progressMap = progresses.associateBy { it.pageId }
            pages.map { p ->
                val progress = progressMap[p.id]
                PageWithProgress(
                    page = Page(
                        id = p.id,
                        sectionId = p.sectionId,
                        title = p.title,
                        format = DocumentFormat.valueOf(p.format),
                        internalFilePath = p.internalFilePath,
                        thumbnailPath = p.thumbnailPath,
                        originalFileName = p.originalFileName,
                        fileSizeBytes = p.fileSizeBytes,
                        pageOrCellCount = p.pageOrCellCount,
                        sortOrder = p.sortOrder,
                        importedAt = p.importedAt,
                        lastOpenedAt = p.lastOpenedAt,
                        charset = p.charset ?: "UTF-8"
                    ),
                    progressFraction = progress?.progressFraction ?: 0f,
                    positionToken = progress?.positionToken
                )
            }
        }
    }

    fun observeRecentlyReadPages(limit: Int = 20): Flow<List<PageWithProgress>> {
        return combine(pageDao.observeRecentlyOpened(limit), progressDao.observeAll()) { pages, progresses ->
            val progressMap = progresses.associateBy { it.pageId }
            pages.map { p ->
                val progress = progressMap[p.id]
                PageWithProgress(
                    page = Page(
                        id = p.id,
                        sectionId = p.sectionId,
                        title = p.title,
                        format = DocumentFormat.valueOf(p.format),
                        internalFilePath = p.internalFilePath,
                        thumbnailPath = p.thumbnailPath,
                        originalFileName = p.originalFileName,
                        fileSizeBytes = p.fileSizeBytes,
                        pageOrCellCount = p.pageOrCellCount,
                        sortOrder = p.sortOrder,
                        importedAt = p.importedAt,
                        lastOpenedAt = p.lastOpenedAt,
                        charset = p.charset ?: "UTF-8"
                    ),
                    progressFraction = progress?.progressFraction ?: 0f,
                    positionToken = progress?.positionToken
                )
            }
        }
    }

    suspend fun getPageById(id: Long): Page? {
        val p = pageDao.getById(id) ?: return null
        return Page(
            id = p.id,
            sectionId = p.sectionId,
            title = p.title,
            format = DocumentFormat.valueOf(p.format),
            internalFilePath = p.internalFilePath,
            thumbnailPath = p.thumbnailPath,
            originalFileName = p.originalFileName,
            fileSizeBytes = p.fileSizeBytes,
            pageOrCellCount = p.pageOrCellCount,
            sortOrder = p.sortOrder,
            importedAt = p.importedAt,
            lastOpenedAt = p.lastOpenedAt,
            charset = p.charset ?: "UTF-8"
        )
    }

    suspend fun insertPage(
        sectionId: Long,
        title: String,
        originalFileName: String,
        format: DocumentFormat,
        filePath: String,
        thumbnailPath: String?,
        fileSizeBytes: Long,
        pageOrCellCount: Int?,
        charset: String? = null
    ): Long {
        val maxOrder = pageDao.getMaxSortOrder(sectionId) ?: 0
        return pageDao.insert(
            PageEntity(
                sectionId = sectionId,
                title = title.trim(),
                originalFileName = originalFileName,
                format = format.name,
                internalFilePath = filePath,
                thumbnailPath = thumbnailPath,
                fileSizeBytes = fileSizeBytes,
                pageOrCellCount = pageOrCellCount,
                sortOrder = maxOrder + 1,
                importedAt = System.currentTimeMillis(),
                lastOpenedAt = null,
                charset = charset ?: "UTF-8"
            )
        )
    }

    suspend fun updateThumbnailPath(pageId: Long, thumbnailPath: String?) {
        pageDao.updateThumbnail(pageId, thumbnailPath)
    }

    suspend fun updateLastOpened(pageId: Long) {
        pageDao.updateLastOpened(pageId, System.currentTimeMillis())
    }

    suspend fun renamePage(pageId: Long, newTitle: String) {
        pageDao.rename(pageId, newTitle.trim())
    }

    suspend fun deletePage(pageId: Long) {
        pageDao.getById(pageId)?.let { p ->
            try {
                File(p.internalFilePath).let { if (it.exists()) it.delete() }
                p.thumbnailPath?.let { File(it).let { t -> if (t.exists()) t.delete() } }
            } catch (_: Exception) {}
        }
        pageDao.deleteById(pageId)
    }

    suspend fun saveProgress(pageId: Long, fraction: Float, token: String) {
        progressDao.upsert(
            ReadingProgressEntity(
                pageId = pageId,
                progressFraction = fraction.coerceIn(0f, 1f),
                positionToken = token,
                updatedAt = System.currentTimeMillis()
            )
        )
    }

    fun observeProgress(pageId: Long): Flow<ReadingProgressEntity?> {
        return progressDao.observeByPageId(pageId)
    }
}

class BookmarkRepository(private val db: AppDatabase) {
    private val bookmarkDao = db.bookmarkDao()

    fun observeAllBookmarks(): Flow<List<Bookmark>> {
        return bookmarkDao.observeAll().map { list ->
            list.map { Bookmark(it.id, it.pageId, it.positionToken, it.label, it.createdAt) }
        }
    }

    fun observeBookmarks(pageId: Long): Flow<List<Bookmark>> {
        return bookmarkDao.observeByPage(pageId).map { list ->
            list.map { Bookmark(it.id, it.pageId, it.positionToken, it.label, it.createdAt) }
        }
    }

    suspend fun addBookmark(pageId: Long, positionToken: String, label: String? = null): Long {
        return bookmarkDao.insert(
            BookmarkEntity(
                pageId = pageId,
                positionToken = positionToken,
                label = label,
                createdAt = System.currentTimeMillis()
            )
        )
    }

    suspend fun deleteBookmark(id: Long) {
        bookmarkDao.deleteById(id)
    }

    suspend fun toggleBookmark(pageId: Long, positionToken: String, label: String? = null): Boolean {
        val existing = bookmarkDao.getByToken(pageId, positionToken)
        return if (existing != null) {
            bookmarkDao.deleteById(existing.id)
            false
        } else {
            bookmarkDao.insert(
                BookmarkEntity(
                    pageId = pageId,
                    positionToken = positionToken,
                    label = label,
                    createdAt = System.currentTimeMillis()
                )
            )
            true
        }
    }
}
