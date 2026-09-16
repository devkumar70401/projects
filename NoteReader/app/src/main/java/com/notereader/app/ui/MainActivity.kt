package com.notereader.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.animation.*
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
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
        // Install Android 12+ SplashScreen with zero flash-to-white
        installSplashScreen()
        super.onCreate(savedInstanceState)
        // Enable edge-to-edge status bar & navigation bar rendering
        enableEdgeToEdge()

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
                    modifier = Modifier
                        .fillMaxSize()
                        .windowInsetsPadding(WindowInsets.safeDrawing),
                    color = MaterialTheme.colorScheme.background
                ) {
                    AnimatedContent(
                        targetState = currentScreen,
                        transitionSpec = {
                            (fadeIn(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            ) + scaleIn(
                                initialScale = 0.96f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioLowBouncy,
                                    stiffness = Spring.StiffnessMediumLow
                                )
                            )) togetherWith (fadeOut(
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ) + scaleOut(
                                targetScale = 1.02f,
                                animationSpec = spring(
                                    dampingRatio = Spring.DampingRatioNoBouncy,
                                    stiffness = Spring.StiffnessMedium
                                )
                            ))
                        },
                        label = "screen_transition"
                    ) { screen ->
                        when (screen) {
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
                                    onBackClick = { currentScreen = Screen.MainNav }
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
                                                    onBackClick = { activeNavTab = NavTab.HOME },
                                                    onNavigateToPage = { pageId ->
                                                        currentScreen = Screen.Reader(pageId)
                                                    }
                                                )
                                            }

                                            NavTab.SETTINGS -> {
                                                val settingsViewModel: SettingsViewModel = viewModel()
                                                SettingsScreen(
                                                    viewModel = settingsViewModel,
                                                    onBackClick = { activeNavTab = NavTab.HOME }
                                                )
                                            }
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
