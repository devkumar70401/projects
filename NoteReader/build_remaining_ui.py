import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

settings_sheet = """package com.notereader.app.ui.reader

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.PageTurnStyle
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.model.ReadingTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReadingSettingsSheet(
    settings: ReadingSettings,
    onSettingsChanged: (ReadingSettings) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                "Reading Settings",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            // Themes
            Text("Themes", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                items(ReadingTheme.values()) { th ->
                    val isSelected = th == settings.theme
                    val bg = Color(android.graphics.Color.parseColor(th.backgroundHex))
                    val txt = Color(android.graphics.Color.parseColor(th.textHex))

                    Surface(
                        shape = RoundedCornerShape(10.dp),
                        color = bg,
                        border = if (isSelected) androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary) else androidx.compose.foundation.BorderStroke(1.dp, Color.Gray.copy(alpha = 0.3f)),
                        modifier = Modifier
                            .size(width = 90.dp, height = 50.dp)
                            .clickable { onSettingsChanged(settings.copy(theme = th)) }
                    ) {
                        Box(contentAlignment = Alignment.Center) {
                            Text(
                                text = th.displayName,
                                color = txt,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(20.dp))

            // Font Family
            Text("Font Family", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf("SansSerif", "Serif", "Monospace").forEach { fam ->
                    val isSelected = fam.equals(settings.fontFamily, ignoreCase = true)
                    FilterChip(
                        selected = isSelected,
                        onClick = { onSettingsChanged(settings.copy(fontFamily = fam)) },
                        label = { Text(fam) }
                    )
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Font Size Slider
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Font Size", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${settings.fontSizeSp.toInt()} sp", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.fontSizeSp,
                onValueChange = { onSettingsChanged(settings.copy(fontSizeSp = it)) },
                valueRange = 12f..32f,
                steps = 10
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Line Spacing
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Line Spacing", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text(String.format("%.2fx", settings.lineSpacingMultiplier), fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.lineSpacingMultiplier,
                onValueChange = { onSettingsChanged(settings.copy(lineSpacingMultiplier = it)) },
                valueRange = 1.0f..2.2f
            )

            Spacer(modifier = Modifier.height(12.dp))

            // Margin
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text("Page Margins", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                Text("${settings.marginDp.toInt()} dp", fontSize = 14.sp, color = MaterialTheme.colorScheme.primary)
            }
            Slider(
                value = settings.marginDp,
                onValueChange = { onSettingsChanged(settings.copy(marginDp = it)) },
                valueRange = 12f..48f
            )
        }
    }
}
"""

bookmarks_panel = """package com.notereader.app.ui.reader

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.Bookmark
import java.text.SimpleDateFormat
import java.util.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BookmarksPanel(
    bookmarks: List<Bookmark>,
    onSelectBookmark: (Bookmark) -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(
        onDismissRequest = onDismiss
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 24.dp)
                .padding(bottom = 36.dp)
        ) {
            Text(
                "Bookmarks",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
            Spacer(modifier = Modifier.height(16.dp))

            if (bookmarks.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(150.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.BookmarkBorder,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            "No bookmarks yet",
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    items(bookmarks, key = { it.id }) { bm ->
                        val dateStr = SimpleDateFormat("MMM d, yyyy HH:mm", Locale.getDefault()).format(Date(bm.createdAt))
                        Surface(
                            shape = RoundedCornerShape(10.dp),
                            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onSelectBookmark(bm)
                                    onDismiss()
                                }
                        ) {
                            Row(
                                modifier = Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Icon(
                                    Icons.Default.Bookmark,
                                    contentDescription = null,
                                    tint = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.size(24.dp)
                                )
                                Spacer(modifier = Modifier.width(12.dp))
                                Column {
                                    Text(
                                        text = bm.label ?: "Bookmark at ${bm.positionToken.take(24)}",
                                        fontWeight = FontWeight.Medium,
                                        fontSize = 14.sp
                                    )
                                    Text(
                                        text = dateStr,
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }
    }
}
"""

reader_screen = """package com.notereader.app.ui.reader

import androidx.compose.animation.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.DocumentFormat

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ReaderScreen(
    viewModel: ReaderViewModel,
    onBackClick: () -> Unit
) {
    val page by viewModel.currentPage.collectAsState()
    val contentState by viewModel.contentState.collectAsState()
    val settings by viewModel.settings.collectAsState()
    val chromeVisible by viewModel.chromeVisible.collectAsState()
    val progressFraction by viewModel.progressFraction.collectAsState()
    val isBookmarked by viewModel.isBookmarked.collectAsState()
    val isInkMode by viewModel.isInkMode.collectAsState()
    val bookmarks by viewModel.bookmarks.collectAsState()

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically { -it } + fadeIn(),
                exit = slideOutVertically { -it } + fadeOut()
            ) {
                TopAppBar(
                    title = {
                        Column {
                            Text(
                                text = page?.title ?: "Document",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis
                            )
                            page?.format?.let { fmt ->
                                Text(
                                    text = fmt.displayName,
                                    fontSize = 11.sp,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    },
                    navigationIcon = {
                        IconButton(onClick = onBackClick) {
                            Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                        }
                    },
                    actions = {
                        // S Pen Ink Mode Toggle
                        IconButton(onClick = { viewModel.toggleInkMode() }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "S Pen Ink Mode",
                                tint = if (isInkMode) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Bookmark Toggle
                        IconButton(onClick = {
                            viewModel.toggleBookmark("bookmark:${(progressFraction * 100).toInt()}%")
                        }) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Bookmarks list
                        IconButton(onClick = { showBookmarksSheet = true }) {
                            Icon(Icons.Default.Bookmarks, contentDescription = "Saved Bookmarks")
                        }
                        // Settings
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Settings")
                        }
                    },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f)
                    )
                )
            }
        },
        bottomBar = {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically { it } + fadeIn(),
                exit = slideOutVertically { it } + fadeOut()
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 24.dp, vertical = 12.dp)
                    ) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = "${(progressFraction * 100).toInt()}% completed",
                                fontSize = 13.sp,
                                fontWeight = FontWeight.SemiBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                        }
                        Spacer(modifier = Modifier.height(6.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = MaterialTheme.colorScheme.primary,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    ) { padding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            when (val state = contentState) {
                is ReaderContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator()
                    }
                }
                is ReaderContentState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(
                            text = state.message,
                            color = MaterialTheme.colorScheme.error,
                            fontSize = 16.sp
                        )
                    }
                }
                is ReaderContentState.PdfReady -> {
                    PdfReaderCanvas(
                        session = state.session,
                        totalPages = state.totalPages,
                        initialPage = state.initialPage,
                        settings = settings,
                        onProgressChange = { viewModel.updateProgress(it) },
                        onToggleChrome = { viewModel.toggleChrome() }
                    )
                }
                is ReaderContentState.TextReady -> {
                    ReflowableReaderCanvas(
                        fullText = state.fullText,
                        isMarkdown = state.isMarkdown,
                        initialOffset = state.initialOffset,
                        settings = settings,
                        onProgressChange = { viewModel.updateProgress(it) },
                        onToggleChrome = { viewModel.toggleChrome() }
                    )
                }
                is ReaderContentState.IpynbReady -> {
                    IpynbReaderCanvas(
                        notebook = state.notebook,
                        initialCell = state.initialCell,
                        settings = settings,
                        onProgressChange = { viewModel.updateProgress(it) },
                        onToggleChrome = { viewModel.toggleChrome() }
                    )
                }
            }

            // Ink Annotation Overlay on top of reading canvas
            InkAnnotationOverlay(
                isInkMode = isInkMode,
                inkColorHex = settings.sPenInkColor,
                baseStrokeWidth = settings.sPenStrokeWidth
            )
        }
    }

    if (showSettingsSheet) {
        ReadingSettingsSheet(
            settings = settings,
            onSettingsChanged = { viewModel.updateSettings(it) },
            onDismiss = { showSettingsSheet = false }
        )
    }

    if (showBookmarksSheet) {
        BookmarksPanel(
            bookmarks = bookmarks,
            onSelectBookmark = { /* jump */ },
            onDismiss = { showBookmarksSheet = false }
        )
    }
}
"""

with open(f"{base_pkg}/ui/reader/ReadingSettingsSheet.kt", "w") as f:
    f.write(settings_sheet)
with open(f"{base_pkg}/ui/reader/BookmarksPanel.kt", "w") as f:
    f.write(bookmarks_panel)
with open(f"{base_pkg}/ui/reader/ReaderScreen.kt", "w") as f:
    f.write(reader_screen)

print("Created ReadingSettingsSheet, BookmarksPanel, and ReaderScreen")
