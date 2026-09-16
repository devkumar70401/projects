package com.notereader.app.ui.reader

import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.DocumentFormat
import com.notereader.app.ui.theme.SpruceGreen

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
    val haptic = LocalHapticFeedback.current

    var showSettingsSheet by remember { mutableStateOf(false) }
    var showBookmarksSheet by remember { mutableStateOf(false) }

    Scaffold(
        topBar = {
            AnimatedVisibility(
                visible = chromeVisible,
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) { -it } + fadeIn(spring()),
                exit = slideOutVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) { -it } + fadeOut(spring())
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
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleInkMode()
                        }) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "S Pen Ink Mode",
                                tint = if (isInkMode) SpruceGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Bookmark Toggle
                        IconButton(onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                            viewModel.toggleBookmark("bookmark:${(progressFraction * 100).toInt()}%")
                        }) {
                            Icon(
                                if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                                contentDescription = "Bookmark",
                                tint = if (isBookmarked) SpruceGreen else MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        // Bookmarks list
                        IconButton(onClick = { showBookmarksSheet = true }) {
                            Icon(Icons.Default.Bookmarks, contentDescription = "Saved Bookmarks")
                        }
                        // Settings (Aa Typography & Themes)
                        IconButton(onClick = { showSettingsSheet = true }) {
                            Icon(Icons.Default.Tune, contentDescription = "Appearance & Themes")
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
                enter = slideInVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) { it } + fadeIn(spring()),
                exit = slideOutVertically(
                    animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium)
                ) { it } + fadeOut(spring())
            ) {
                Surface(
                    color = MaterialTheme.colorScheme.surface.copy(alpha = 0.95f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 28.dp, vertical = 14.dp)
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
                        Spacer(modifier = Modifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = { progressFraction },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(6.dp),
                            color = SpruceGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                    }
                }
            }
        }
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when (val state = contentState) {
                is ReaderContentState.Loading -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        CircularProgressIndicator(color = SpruceGreen)
                    }
                }

                is ReaderContentState.Error -> {
                    Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                        Text(state.message, color = MaterialTheme.colorScheme.error)
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

            // Stylus Ink Annotation Overlay (Active when Ink mode is enabled)
            InkAnnotationOverlay(
                isInkMode = isInkMode,
                inkColorHex = settings.sPenInkColor,
                baseStrokeWidth = settings.sPenStrokeWidth,
                modifier = Modifier.fillMaxSize()
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
            onSelectBookmark = { bm ->
                showBookmarksSheet = false
            },
            onDismiss = { showBookmarksSheet = false }
        )
    }
}
