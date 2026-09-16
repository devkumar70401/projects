import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

# 1. Repositories.kt
repos_code = """package com.notereader.app.data.repository

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
        notebookDao.rename(id, newTitle.trim())
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
                charset = charset
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
"""

with open(f"{base_pkg}/data/repository/Repositories.kt", "w") as f:
    f.write(repos_code)

# 2. NoteReaderApplication.kt
app_code = """package com.notereader.app

import android.app.Application
import com.notereader.app.data.db.AppDatabase
import com.notereader.app.data.files.FileImportManager
import com.notereader.app.data.files.LibraryExportManager
import com.notereader.app.data.files.ThumbnailGenerator
import com.notereader.app.data.repository.*
import com.notereader.app.domain.model.DocumentFormat
import com.notereader.app.domain.usecase.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.io.File

class NoteReaderApplication : Application() {

    val applicationScope = CoroutineScope(Dispatchers.Default)

    lateinit var database: AppDatabase
        private set

    lateinit var notebookRepository: NotebookRepository
        private set
    lateinit var sectionRepository: SectionRepository
        private set
    lateinit var pageRepository: PageRepository
        private set
    lateinit var bookmarkRepository: BookmarkRepository
        private set
    lateinit var settingsRepository: SettingsRepository
        private set

    lateinit var fileImportManager: FileImportManager
        private set
    lateinit var thumbnailGenerator: ThumbnailGenerator
        private set
    lateinit var libraryExportManager: LibraryExportManager
        private set

    lateinit var importFileUseCase: ImportFileAsPageUseCase
        private set
    lateinit var saveReadingProgressUseCase: SaveReadingProgressUseCase
        private set
    lateinit var searchLibraryUseCase: SearchLibraryUseCase
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)

        notebookRepository = NotebookRepository(database)
        sectionRepository = SectionRepository(database)
        pageRepository = PageRepository(database, this)
        bookmarkRepository = BookmarkRepository(database)
        settingsRepository = SettingsRepository(this)

        fileImportManager = FileImportManager(this)
        thumbnailGenerator = ThumbnailGenerator(this)
        libraryExportManager = LibraryExportManager(this, database)

        importFileUseCase = ImportFileAsPageUseCase(
            fileImportManager = fileImportManager,
            pageRepository = pageRepository,
            thumbnailGenerator = thumbnailGenerator,
            applicationScope = applicationScope
        )
        saveReadingProgressUseCase = SaveReadingProgressUseCase(database)
        searchLibraryUseCase = SearchLibraryUseCase(database)

        // Seed initial data asynchronously on IO
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        val existing = notebookRepository.observeAllNotebooks().first()
        if (existing.isNotEmpty()) return

        // Create initial notebooks matching Stitch UI
        val researchId = notebookRepository.createNotebook("Research", "#3E6259")
        val alphaId = notebookRepository.createNotebook("Project Alpha", "#7C4D3E")
        val readingId = notebookRepository.createNotebook("Reading List", "#2F4F4F")
        val journalId = notebookRepository.createNotebook("Journal", "#7C4D3E")

        // Sections
        val secDeepLearning = sectionRepository.createSection(researchId, "Deep Learning", "#3E6259")
        val secTransformers = sectionRepository.createSection(researchId, "Transformers", "#4A3E7C")
        val secDataAnalysis = sectionRepository.createSection(researchId, "Data Analysis", "#D99026")
        val secQuantum = sectionRepository.createSection(researchId, "Quantum Basics", "#31685A")

        sectionRepository.createSection(alphaId, "System Architecture", "#7C4D3E")
        sectionRepository.createSection(readingId, "Distributed Systems", "#2F4F4F")
        sectionRepository.createSection(journalId, "Q4 Reflections", "#7C4D3E")

        // Populate sample documents into internal storage
        try {
            val pagesDir = File(filesDir, "pages").apply { mkdirs() }

            // 1. Markdown Note
            val mdFile = File(pagesDir, "${System.currentTimeMillis()}_transformers.md")
            mdFile.writeText(\"\"\"# Notes on Transformers

## 1. Self-Attention Mechanism
The scaled dot-product attention is defined as:

$$\\text{Attention}(Q, K, V) = \\text{softmax}\\left(\\frac{QK^T}{\\sqrt{d_k}}\\right)V$$

| Vector | Purpose | Dimension |
| :--- | :--- | :--- |
| **Query (Q)** | Token query | $(B, N, d_k)$ |
| **Key (K)** | Token key | $(B, N, d_k)$ |
| **Value (V)** | Retrieved value | $(B, N, d_v)$ |

## 2. Multi-Head Attention
Allows joint representation learning across different projection spaces.
\"\"\")
            val mdPageId = pageRepository.insertPage(
                sectionId = secTransformers,
                title = "Notes on Transformers.md",
                originalFileName = "Notes on Transformers.md",
                format = DocumentFormat.MARKDOWN,
                filePath = mdFile.absolutePath,
                thumbnailPath = null,
                fileSizeBytes = mdFile.length(),
                pageOrCellCount = 15
            )
            val mdThumb = thumbnailGenerator.generate(mdPageId, mdFile.absolutePath, DocumentFormat.MARKDOWN)
            pageRepository.updateThumbnailPath(mdPageId, mdThumb)
            pageRepository.saveProgress(mdPageId, 1.0f, "offset:100%")

            // 2. Jupyter Notebook
            val ipynbFile = File(pagesDir, "${System.currentTimeMillis()}_data_analysis.ipynb")
            ipynbFile.writeText(\"\"\"{
 "cells": [
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "# Data Analysis & Regression Lab\\n",
    "In this notebook, we explore foundational statistical modeling, regression fit, and loss convergence."
   ]
  },
  {
   "cell_type": "code",
   "execution_count": 1,
   "metadata": {},
   "outputs": [
    {
     "name": "stdout",
     "output_type": "stream",
     "text": [
      "Matrix multiplication completed in 0.0018s\\n",
      "Tensor shape: (100, 100)\\n"
     ]
    }
   ],
   "source": [
    "import numpy as np\\n",
    "import matplotlib.pyplot as plt\\n",
    "\\n",
    "A = np.random.randn(100, 100)\\n",
    "B = np.random.randn(100, 100)\\n",
    "C = np.dot(A, B)\\n",
    "print('Matrix multiplication completed in 0.0018s')\\n",
    "print(f'Tensor shape: {C.shape}')"
   ]
  },
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "### Loss Convergence\\n",
    "Mean accuracy over 5 cross-validation folds."
   ]
  },
  {
   "cell_type": "code",
   "execution_count": 2,
   "metadata": {},
   "outputs": [
    {
     "data": {
      "text/plain": [
       "Mean Accuracy: 98.4% (+/- 0.6%)"
      ]
     },
     "execution_count": 2,
     "output_type": "execute_result"
    }
   ],
   "source": [
    "scores = [0.982, 0.988, 0.979, 0.985, 0.986]\\n",
    "f'Mean Accuracy: {np.mean(scores)*100:.1f}% (+/- {np.std(scores)*100:.1f}%)'"
   ]
  }
 ],
 "metadata": {
  "language_info": {"name": "python"}
 },
 "nbformat": 4,
 "nbformat_minor": 4
}\"\"\")
            val ipynbPageId = pageRepository.insertPage(
                sectionId = secDataAnalysis,
                title = "Data Analysis.ipynb",
                originalFileName = "Data Analysis.ipynb",
                format = DocumentFormat.IPYNB,
                filePath = ipynbFile.absolutePath,
                thumbnailPath = null,
                fileSizeBytes = ipynbFile.length(),
                pageOrCellCount = 4
            )
            val ipynbThumb = thumbnailGenerator.generate(ipynbPageId, ipynbFile.absolutePath, DocumentFormat.IPYNB)
            pageRepository.updateThumbnailPath(ipynbPageId, ipynbThumb)
            pageRepository.saveProgress(ipynbPageId, 0.25f, "cell:1")

            // 3. Plain Text
            val txtFile = File(pagesDir, "${System.currentTimeMillis()}_quantum.txt")
            txtFile.writeText(\"\"\"Quantum Computing Fundamentals

1. Quantum Bits (Qubits)
Unlike classical bits which can exist in a definite state of 0 or 1, a quantum bit (qubit) can exist in a superposition of both states |0> and |1>.
Mathematically, a pure qubit state can be written as a linear combination:
|psi> = alpha|0> + beta|1>
where alpha and beta are complex probability amplitudes such that |alpha|^2 + |beta|^2 = 1.

2. Quantum Superposition
Superposition allows a quantum computer to evaluate many possible combinations simultaneously. When measured, however, the superposition collapses into one of the basis states with probability proportional to the square of its amplitude.

3. Quantum Entanglement
Entanglement is a phenomenon where quantum particles become inextricably linked, such that the state of one instantly correlates with the state of another, regardless of distance.
Einstein famously described this as "spooky action at a distance".
\"\"\")
            val txtPageId = pageRepository.insertPage(
                sectionId = secQuantum,
                title = "Quantum Computing Fundamentals.txt",
                originalFileName = "Quantum Computing Fundamentals.txt",
                format = DocumentFormat.TXT,
                filePath = txtFile.absolutePath,
                thumbnailPath = null,
                fileSizeBytes = txtFile.length(),
                pageOrCellCount = 8
            )
            val txtThumb = thumbnailGenerator.generate(txtPageId, txtFile.absolutePath, DocumentFormat.TXT)
            pageRepository.updateThumbnailPath(txtPageId, txtThumb)
            pageRepository.saveProgress(txtPageId, 0.53f, "offset:53%")

            // Sample Bookmark
            bookmarkRepository.addBookmark(
                pageId = mdPageId,
                positionToken = "offset:0",
                label = "Attention Matrix Definition"
            )
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
"""

with open(f"{base_pkg}/NoteReaderApplication.kt", "w") as f:
    f.write(app_code)

print("Repositories.kt and NoteReaderApplication.kt updated.")
