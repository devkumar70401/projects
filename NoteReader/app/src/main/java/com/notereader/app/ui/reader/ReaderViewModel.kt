package com.notereader.app.ui.reader

import android.app.Application
import android.graphics.Bitmap
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notereader.app.NoteReaderApplication
import com.notereader.app.domain.model.*
import com.notereader.app.domain.usecase.ReaderRawState
import com.notereader.app.rendering.ipynb.IpynbNotebook
import com.notereader.app.rendering.ipynb.IpynbParser
import com.notereader.app.rendering.markdown.MarkwonProvider
import com.notereader.app.rendering.pdf.PdfDocumentSession
import com.notereader.app.rendering.text.CharsetDetectingReader
import com.notereader.app.rendering.text.ReflowableTextPaginator
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import java.io.File

sealed class ReaderContentState {
    object Loading : ReaderContentState()
    data class Error(val message: String) : ReaderContentState()
    data class PdfReady(val session: PdfDocumentSession, val totalPages: Int, val initialPage: Int) : ReaderContentState()
    data class TextReady(val fullText: CharSequence, val isMarkdown: Boolean, val initialOffset: Int) : ReaderContentState()
    data class IpynbReady(val notebook: IpynbNotebook, val initialCell: Int) : ReaderContentState()
}

class ReaderViewModel(
    application: Application,
    val pageId: Long
) : AndroidViewModel(application) {
    private val app = application as NoteReaderApplication
    private val pageRepo = app.pageRepository
    private val bookmarkRepo = app.bookmarkRepository
    private val settingsRepo = app.settingsRepository
    private val saveProgressUseCase = app.saveReadingProgressUseCase

    private val _contentState = MutableStateFlow<ReaderContentState>(ReaderContentState.Loading)
    val contentState: StateFlow<ReaderContentState> = _contentState.asStateFlow()

    private val _currentPage = MutableStateFlow<Page?>(null)
    val currentPage: StateFlow<Page?> = _currentPage.asStateFlow()

    val settings: StateFlow<ReadingSettings> = settingsRepo.readingSettingsFlow
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), ReadingSettings())

    val bookmarks: StateFlow<List<Bookmark>> = bookmarkRepo.observeBookmarks(pageId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isBookmarked = MutableStateFlow(false)
    val isBookmarked: StateFlow<Boolean> = _isBookmarked.asStateFlow()

    private val _isInkMode = MutableStateFlow(false)
    val isInkMode: StateFlow<Boolean> = _isInkMode.asStateFlow()

    private val _chromeVisible = MutableStateFlow(true)
    val chromeVisible: StateFlow<Boolean> = _chromeVisible.asStateFlow()

    private val _progressFraction = MutableStateFlow(0f)
    val progressFraction: StateFlow<Float> = _progressFraction.asStateFlow()

    private var activePdfSession: PdfDocumentSession? = null
    private var saveJob: Job? = null

    init {
        loadDocument()
    }

    private fun loadDocument() {
        viewModelScope.launch(Dispatchers.IO) {
            val page = pageRepo.getPageById(pageId)
            if (page == null) {
                _contentState.value = ReaderContentState.Error("Document not found")
                return@launch
            }
            _currentPage.value = page
            pageRepo.updateLastOpened(pageId)

            val progress = app.database.readingProgressDao().getByPageId(pageId)
            _progressFraction.value = progress?.progressFraction ?: 0f
            val token = progress?.positionToken ?: ""

            when (page.format) {
                DocumentFormat.PDF -> {
                    try {
                        val session = PdfDocumentSession(File(page.internalFilePath))
                        session.open()
                        activePdfSession = session
                        val initPage = parsePdfPage(token)
                        _contentState.value = ReaderContentState.PdfReady(session, session.pageCount(), initPage)
                    } catch (e: Exception) {
                        _contentState.value = ReaderContentState.Error("Failed to open PDF: ${e.message}")
                    }
                }
                DocumentFormat.TXT -> {
                    val text = CharsetDetectingReader.readString(page.internalFilePath, page.charset)
                    val initOffset = parseCharOffset(token)
                    _contentState.value = ReaderContentState.TextReady(text, isMarkdown = false, initialOffset = initOffset)
                }
                DocumentFormat.MARKDOWN -> {
                    val rawMd = CharsetDetectingReader.readString(page.internalFilePath, "UTF-8")
                    val spanned = MarkwonProvider.renderMarkdown(getApplication(), rawMd)
                    val initOffset = parseCharOffset(token)
                    _contentState.value = ReaderContentState.TextReady(spanned, isMarkdown = true, initialOffset = initOffset)
                }
                DocumentFormat.IPYNB -> {
                    val rawJson = CharsetDetectingReader.readString(page.internalFilePath, "UTF-8")
                    val parseRes = IpynbParser.parse(rawJson)
                    if (parseRes.isSuccess) {
                        val initCell = parseIpynbCell(token)
                        _contentState.value = ReaderContentState.IpynbReady(parseRes.getOrThrow(), initCell)
                    } else {
                        _contentState.value = ReaderContentState.Error("Failed to parse Jupyter Notebook")
                    }
                }
            }
        }
    }

    private fun parsePdfPage(token: String): Int {
        if (!token.startsWith("pdf:page=")) return 0
        return token.substringAfter("pdf:page=").substringBefore(";").toIntOrNull() ?: 0
    }

    private fun parseCharOffset(token: String): Int {
        if (!token.startsWith("text:charOffset=")) return 0
        return token.substringAfter("text:charOffset=").toIntOrNull() ?: 0
    }

    private fun parseIpynbCell(token: String): Int {
        if (!token.startsWith("ipynb:cellIndex=")) return 0
        return token.substringAfter("ipynb:cellIndex=").substringBefore(";").toIntOrNull() ?: 0
    }

    fun toggleChrome() {
        _chromeVisible.value = !_chromeVisible.value
    }

    fun toggleInkMode() {
        _isInkMode.value = !_isInkMode.value
    }

    fun updateProgress(rawState: ReaderRawState) {
        val format = _currentPage.value?.format ?: return
        val frac = when (format) {
            DocumentFormat.PDF -> rawState.pdfPageIndex.toFloat() / rawState.totalPdfPages.coerceAtLeast(1)
            DocumentFormat.TXT, DocumentFormat.MARKDOWN -> rawState.charOffset.toFloat() / rawState.totalCharCount.coerceAtLeast(1)
            DocumentFormat.IPYNB -> rawState.cellIndex.toFloat() / rawState.totalCellCount.coerceAtLeast(1)
        }.coerceIn(0f, 1f)
        _progressFraction.value = frac

        saveJob?.cancel()
        saveJob = viewModelScope.launch(Dispatchers.IO) {
            delay(1500) // 1.5s debounce
            saveProgressUseCase(pageId, format, rawState)
        }
    }

    fun updateSettings(newSettings: ReadingSettings) {
        viewModelScope.launch {
            settingsRepo.updateSettings(newSettings)
        }
    }

    fun toggleBookmark(token: String) {
        viewModelScope.launch {
            val bookmarked = bookmarkRepo.toggleBookmark(pageId, token)
            _isBookmarked.value = bookmarked
        }
    }

    override fun onCleared() {
        super.onCleared()
        activePdfSession?.close()
    }
}
