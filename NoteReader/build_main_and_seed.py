import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

# 1. NoteReaderApplication.kt with seed data
app_kt = """package com.notereader.app

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
import java.io.FileOutputStream

class NoteReaderApplication : Application() {

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
    lateinit var searchInDocumentUseCase: SearchInDocumentUseCase
        private set

    override fun onCreate() {
        super.onCreate()

        database = AppDatabase.getInstance(this)

        notebookRepository = NotebookRepository(database.notebookDao())
        sectionRepository = SectionRepository(database.sectionDao())
        pageRepository = PageRepository(database.pageDao(), database.readingProgressDao())
        bookmarkRepository = BookmarkRepository(database.bookmarkDao())
        settingsRepository = SettingsRepository(this)

        fileImportManager = FileImportManager(this)
        thumbnailGenerator = ThumbnailGenerator(this)
        libraryExportManager = LibraryExportManager(this, database)

        importFileUseCase = ImportFileAsPageUseCase(
            fileImportManager = fileImportManager,
            thumbnailGenerator = thumbnailGenerator,
            pageRepository = pageRepository,
            sectionRepository = sectionRepository
        )
        saveReadingProgressUseCase = SaveReadingProgressUseCase(pageRepository)
        searchLibraryUseCase = SearchLibraryUseCase(database.pageDao(), database.notebookDao())
        searchInDocumentUseCase = SearchInDocumentUseCase()

        // Auto-seed initial research notebooks and sample notes if DB is empty
        CoroutineScope(Dispatchers.IO).launch {
            seedInitialDataIfEmpty()
        }
    }

    private suspend fun seedInitialDataIfEmpty() {
        val existing = notebookRepository.observeAllNotebooks().first()
        if (existing.isNotEmpty()) return

        // Create Notebooks matching Stitch Design
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

        // Populate sample documents
        try {
            val pagesDir = File(filesDir, "pages").apply { mkdirs() }

            // 1. Markdown note
            val mdFile = File(pagesDir, "${System.currentTimeMillis()}_transformers.md")
            mdFile.writeText(\"\"\"# Notes on Transformers

## 1. Self-Attention Mechanism
The scaled dot-product attention maps a set of query, key, and value vectors into an output matrix:

$$\\text{Attention}(Q, K, V) = \\text{softmax}\\left(\\frac{QK^T}{\\sqrt{d_k}}\\right)V$$

| Vector | Role | Dimension |
| :--- | :--- | :--- |
| **Query (Q)** | Token search intent | $(B, N, d_k)$ |
| **Key (K)** | Token features | $(B, N, d_k)$ |
| **Value (V)** | Retrieved embedding | $(B, N, d_v)$ |

## 2. Multi-Head Attention
Multi-head attention allows the model to jointly attend to information from different representation subspaces at different positions.
\"\"\")
            val mdThumb = thumbnailGenerator.generateThumbnail(mdFile, DocumentFormat.MARKDOWN)
            val mdPageId = pageRepository.insertPage(
                sectionId = secTransformers,
                title = "Notes on Transformers.md",
                originalFileName = "Notes on Transformers.md",
                format = DocumentFormat.MARKDOWN,
                filePath = mdFile.absolutePath,
                thumbnailPath = mdThumb?.absolutePath,
                fileSizeBytes = mdFile.length(),
                pageOrCellCount = 15
            )
            pageRepository.saveProgress(mdPageId, 1.0f, "offset:100%")

            // 2. Jupyter Notebook
            val ipynbFile = File(pagesDir, "${System.currentTimeMillis()}_data_analysis.ipynb")
            ipynbFile.writeText(\"\"\"{
 "cells": [
  {
   "cell_type": "markdown",
   "metadata": {},
   "source": [
    "# Data Science & Regression Lab\\n",
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
            val ipynbThumb = thumbnailGenerator.generateThumbnail(ipynbFile, DocumentFormat.IPYNB)
            val ipynbPageId = pageRepository.insertPage(
                sectionId = secDataAnalysis,
                title = "Data Analysis.ipynb",
                originalFileName = "Data Analysis.ipynb",
                format = DocumentFormat.IPYNB,
                filePath = ipynbFile.absolutePath,
                thumbnailPath = ipynbThumb?.absolutePath,
                fileSizeBytes = ipynbFile.length(),
                pageOrCellCount = 4
            )
            pageRepository.saveProgress(ipynbPageId, 0.25f, "cell:1")

            // 3. Plain text note
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
            val txtThumb = thumbnailGenerator.generateThumbnail(txtFile, DocumentFormat.TXT)
            val txtPageId = pageRepository.insertPage(
                sectionId = secQuantum,
                title = "Quantum Computing Fundamentals.txt",
                originalFileName = "Quantum Computing Fundamentals.txt",
                format = DocumentFormat.TXT,
                filePath = txtFile.absolutePath,
                thumbnailPath = txtThumb?.absolutePath,
                fileSizeBytes = txtFile.length(),
                pageOrCellCount = 8
            )
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

# 2. MainActivity.kt with SideNav layout
main_activity_kt = """package com.notereader.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.notereader.app.NoteReaderApplication
import com.notereader.app.domain.model.Notebook
import com.notereader.app.ui.bookmarks.BookmarksView
import com.notereader.app.ui.components.NavTab
import com.notereader.app.ui.components.SideNav
import com.notereader.app.ui.notebook.NotebookDetailView
import com.notereader.app.ui.notebook.NotebookViewModel
import com.notereader.app.ui.reader.ReaderScreen
import com.notereader.app.ui.reader.ReaderViewModel
import com.notereader.app.ui.recent.RecentView
import com.notereader.app.ui.search.GlobalSearchScreen
import com.notereader.app.ui.search.SearchViewModel
import com.notereader.app.ui.settings.SettingsScreen
import com.notereader.app.ui.settings.SettingsViewModel
import com.notereader.app.ui.shelf.CreateNotebookDialog
import com.notereader.app.ui.shelf.ShelfView
import com.notereader.app.ui.shelf.ShelfViewModel
import com.notereader.app.ui.theme.NoteReaderTheme

sealed class Screen {
    object MainNav : Screen()
    data class NotebookDetail(val notebook: Notebook) : Screen()
    data class Reader(val pageId: Long) : Screen()
}

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteReaderTheme {
                val app = application as NoteReaderApplication
                val shelfViewModel: ShelfViewModel = viewModel()

                val notebooks by shelfViewModel.notebooks.collectAsState()
                val recentPages by shelfViewModel.recentlyReadPages.collectAsState()
                val bookmarks by app.bookmarkRepository.observeAllBookmarks().collectAsState(initial = emptyList())

                var currentScreen by remember { mutableStateOf<Screen>(Screen.MainNav) }
                var activeNavTab by remember { mutableStateOf(NavTab.HOME) }
                var showCreateNotebookDialog by remember { mutableStateOf(false) }

                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    when (val screen = currentScreen) {
                        is Screen.Reader -> {
                            val readerViewModel: ReaderViewModel = viewModel(
                                key = "reader_${screen.pageId}",
                                factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                    override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                        return ReaderViewModel(app, screen.pageId) as T
                                    }
                                }
                            )
                            ReaderScreen(
                                viewModel = readerViewModel,
                                onBack = { currentScreen = Screen.MainNav }
                            )
                        }

                        is Screen.NotebookDetail -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                SideNav(
                                    activeTab = activeNavTab,
                                    onSelectTab = { tab ->
                                        activeNavTab = tab
                                        currentScreen = Screen.MainNav
                                    },
                                    onOpenNewNotebook = { showCreateNotebookDialog = true },
                                    bookmarksCount = bookmarks.size
                                )

                                val notebookVm: NotebookViewModel = viewModel(
                                    key = "notebook_${screen.notebook.id}",
                                    factory = object : androidx.lifecycle.ViewModelProvider.Factory {
                                        override fun <T : androidx.lifecycle.ViewModel> create(modelClass: Class<T>): T {
                                            return NotebookViewModel(app, screen.notebook.id) as T
                                        }
                                    }
                                )

                                NotebookDetailView(
                                    notebook = screen.notebook,
                                    viewModel = notebookVm,
                                    onBackToShelf = { currentScreen = Screen.MainNav },
                                    onPageClick = { pageId ->
                                        currentScreen = Screen.Reader(pageId)
                                    }
                                )
                            }
                        }

                        is Screen.MainNav -> {
                            Row(modifier = Modifier.fillMaxSize()) {
                                SideNav(
                                    activeTab = activeNavTab,
                                    onSelectTab = { activeNavTab = it },
                                    onOpenNewNotebook = { showCreateNotebookDialog = true },
                                    bookmarksCount = bookmarks.size
                                )

                                Box(modifier = Modifier.weight(1f)) {
                                    when (activeNavTab) {
                                        NavTab.HOME -> {
                                            ShelfView(
                                                notebooks = notebooks,
                                                onSelectNotebook = { nb ->
                                                    currentScreen = Screen.NotebookDetail(nb)
                                                },
                                                onOpenNewNotebookModal = { showCreateNotebookDialog = true },
                                                onDeleteNotebook = { shelfViewModel.deleteNotebook(it) }
                                            )
                                        }

                                        NavTab.RECENT -> {
                                            RecentView(
                                                recentPages = recentPages,
                                                onOpenPage = { pageId ->
                                                    currentScreen = Screen.Reader(pageId)
                                                }
                                            )
                                        }

                                        NavTab.BOOKMARKS -> {
                                            BookmarksView(
                                                bookmarks = bookmarks,
                                                onOpenBookmark = { pageId, _ ->
                                                    currentScreen = Screen.Reader(pageId)
                                                },
                                                onDeleteBookmark = {
                                                    shelfViewModel.deleteBookmark(it)
                                                }
                                            )
                                        }

                                        NavTab.SEARCH -> {
                                            val searchViewModel: SearchViewModel = viewModel()
                                            GlobalSearchScreen(
                                                viewModel = searchViewModel,
                                                onBack = { activeNavTab = NavTab.HOME },
                                                onPageClick = { pageId ->
                                                    currentScreen = Screen.Reader(pageId)
                                                }
                                            )
                                        }

                                        NavTab.SETTINGS -> {
                                            val settingsViewModel: SettingsViewModel = viewModel()
                                            SettingsScreen(
                                                viewModel = settingsViewModel,
                                                onBack = { activeNavTab = NavTab.HOME }
                                            )
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                if (showCreateNotebookDialog) {
                    CreateNotebookDialog(
                        onDismiss = { showCreateNotebookDialog = false },
                        onCreate = { title, color ->
                            shelfViewModel.createNotebook(title, color)
                            showCreateNotebookDialog = false
                        }
                    )
                }
            }
        }
    }
}
"""

with open(f"{base_pkg}/NoteReaderApplication.kt", "w") as f:
    f.write(app_kt)
with open(f"{base_pkg}/ui/MainActivity.kt", "w") as f:
    f.write(main_activity_kt)

print("Updated NoteReaderApplication.kt and MainActivity.kt")
