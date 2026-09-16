import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

search_screen = """package com.notereader.app.ui.search

import android.app.Application
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notereader.app.NoteReaderApplication
import com.notereader.app.domain.usecase.SearchResultItem
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SearchViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoteReaderApplication
    private val searchUseCase = app.searchLibraryUseCase

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()

    private val _results = MutableStateFlow<List<SearchResultItem>>(emptyList())
    val results: StateFlow<List<SearchResultItem>> = _results.asStateFlow()

    fun onQueryChange(newQuery: String) {
        _query.value = newQuery
        viewModelScope.launch {
            _results.value = searchUseCase(newQuery)
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GlobalSearchScreen(
    viewModel: SearchViewModel,
    onBackClick: () -> Unit,
    onNavigateToPage: (Long) -> Unit
) {
    val query by viewModel.query.collectAsState()
    val results by viewModel.results.collectAsState()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    OutlinedTextField(
                        value = query,
                        onValueChange = { viewModel.onQueryChange(it) },
                        placeholder = { Text("Search documents...") },
                        singleLine = true,
                        leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
                        trailingIcon = {
                            if (query.isNotEmpty()) {
                                IconButton(onClick = { viewModel.onQueryChange("") }) {
                                    Icon(Icons.Default.Clear, contentDescription = "Clear")
                                }
                            }
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(end = 8.dp)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(horizontal = 24.dp, vertical = 16.dp)
        ) {
            if (query.isBlank()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("Type a query to search across all your documents", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else if (results.isEmpty()) {
                Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                    Text("No results found for '$query'", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            } else {
                LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    items(results) { item ->
                        when (item) {
                            is SearchResultItem.PageMatch -> {
                                Surface(
                                    shape = RoundedCornerShape(12.dp),
                                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                                    modifier = Modifier
                                        .fillMaxWidth()
                                        .clickable { onNavigateToPage(item.id) }
                                ) {
                                    Row(
                                        modifier = Modifier.padding(16.dp),
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        Icon(
                                            Icons.Default.Description,
                                            contentDescription = null,
                                            tint = MaterialTheme.colorScheme.primary
                                        )
                                        Spacer(modifier = Modifier.width(16.dp))
                                        Column {
                                            Text(
                                                text = item.title,
                                                fontWeight = FontWeight.SemiBold,
                                                fontSize = 15.sp
                                            )
                                            Text(
                                                text = item.format.displayName,
                                                fontSize = 12.sp,
                                                color = MaterialTheme.colorScheme.onSurfaceVariant
                                            )
                                        }
                                    }
                                }
                            }
                            else -> {}
                        }
                    }
                }
            }
        }
    }
}
"""

settings_screen = """package com.notereader.app.ui.settings

import android.app.Application
import android.widget.Toast
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notereader.app.NoteReaderApplication
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoteReaderApplication
    private val exportManager = app.libraryExportManager

    fun exportLibrary(onSuccess: (String) -> Unit, onError: (String) -> Unit) {
        viewModelScope.launch {
            val res = exportManager.exportFullLibrary()
            if (res.isSuccess) {
                onSuccess(res.getOrThrow().absolutePath)
            } else {
                onError(res.exceptionOrNull()?.message ?: "Export failed")
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel,
    onBackClick: () -> Unit
) {
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Settings", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text("Backup & Storage", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)

            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("Export Full Library Backup", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        "Packages all your imported documents and annotations into a single zip file.",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Button(
                        onClick = {
                            viewModel.exportLibrary(
                                onSuccess = { path ->
                                    Toast.makeText(context, "Export saved to: $path", Toast.LENGTH_LONG).show()
                                },
                                onError = { err ->
                                    Toast.makeText(context, "Error: $err", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    ) {
                        Icon(Icons.Default.Backup, contentDescription = null)
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Export Backup (.zip)")
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))
            Text("About NoteReader", fontSize = 16.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
            ElevatedCard(
                shape = RoundedCornerShape(14.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text("NoteReader for Android Tablet", fontWeight = FontWeight.Bold, fontSize = 15.sp)
                    Text("Version 1.0.0 (Native Kotlin + Jetpack Compose)", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("Designed specifically for studying: PDF, Markdown, Text, and Jupyter Notebook (.ipynb) with Moon+ Reader style gestures and S Pen stylus support.", fontSize = 13.sp)
                }
            }
        }
    }
}
"""

main_activity = """package com.notereader.app.ui

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.notereader.app.ui.notebook.NotebookScreen
import com.notereader.app.ui.notebook.NotebookViewModel
import com.notereader.app.ui.reader.ReaderScreen
import com.notereader.app.ui.reader.ReaderViewModel
import com.notereader.app.ui.search.GlobalSearchScreen
import com.notereader.app.ui.search.SearchViewModel
import com.notereader.app.ui.settings.SettingsScreen
import com.notereader.app.ui.settings.SettingsViewModel
import com.notereader.app.ui.shelf.ShelfScreen
import com.notereader.app.ui.shelf.ShelfViewModel
import com.notereader.app.ui.theme.NoteReaderTheme

class MainActivity : ComponentActivity() {

    private val shelfViewModel: ShelfViewModel by viewModels()
    private val searchViewModel: SearchViewModel by viewModels()
    private val settingsViewModel: SettingsViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            NoteReaderTheme {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    NoteReaderApp(
                        shelfViewModel = shelfViewModel,
                        searchViewModel = searchViewModel,
                        settingsViewModel = settingsViewModel
                    )
                }
            }
        }
    }
}

@Composable
fun NoteReaderApp(
    shelfViewModel: ShelfViewModel,
    searchViewModel: SearchViewModel,
    settingsViewModel: SettingsViewModel
) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "shelf") {
        composable("shelf") {
            ShelfScreen(
                viewModel = shelfViewModel,
                onNotebookClick = { notebookId ->
                    navController.navigate("notebook/$notebookId")
                },
                onPageClick = { pageId ->
                    navController.navigate("reader/$pageId")
                },
                onSearchClick = { navController.navigate("search") },
                onSettingsClick = { navController.navigate("settings") }
            )
        }

        composable(
            route = "notebook/{notebookId}",
            arguments = listOf(navArgument("notebookId") { type = NavType.LongType })
        ) { backStackEntry ->
            val notebookId = backStackEntry.arguments?.getLong("notebookId") ?: 0L
            val context = androidx.compose.ui.platform.LocalContext.current
            val app = context.applicationContext as android.app.Application
            val notebookViewModel = androidx.lifecycle.viewmodel.compose.viewModel<NotebookViewModel>(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return NotebookViewModel(app, notebookId) as T
                    }
                }
            )

            NotebookScreen(
                viewModel = notebookViewModel,
                onBackClick = { navController.popBackStack() },
                onPageClick = { pageId ->
                    navController.navigate("reader/$pageId")
                }
            )
        }

        composable(
            route = "reader/{pageId}",
            arguments = listOf(navArgument("pageId") { type = NavType.LongType })
        ) { backStackEntry ->
            val pageId = backStackEntry.arguments?.getLong("pageId") ?: 0L
            val context = androidx.compose.ui.platform.LocalContext.current
            val app = context.applicationContext as android.app.Application
            val readerViewModel = androidx.lifecycle.viewmodel.compose.viewModel<ReaderViewModel>(
                factory = object : ViewModelProvider.Factory {
                    override fun <T : ViewModel> create(modelClass: Class<T>): T {
                        @Suppress("UNCHECKED_CAST")
                        return ReaderViewModel(app, pageId) as T
                    }
                }
            )

            ReaderScreen(
                viewModel = readerViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }

        composable("search") {
            GlobalSearchScreen(
                viewModel = searchViewModel,
                onBackClick = { navController.popBackStack() },
                onNavigateToPage = { pageId ->
                    navController.navigate("reader/$pageId")
                }
            )
        }

        composable("settings") {
            SettingsScreen(
                viewModel = settingsViewModel,
                onBackClick = { navController.popBackStack() }
            )
        }
    }
}
"""

with open(f"{base_pkg}/ui/search/GlobalSearchScreen.kt", "w") as f:
    f.write(search_screen)
with open(f"{base_pkg}/ui/settings/SettingsScreen.kt", "w") as f:
    f.write(settings_screen)
with open(f"{base_pkg}/ui/MainActivity.kt", "w") as f:
    f.write(main_activity)

print("Created GlobalSearchScreen, SettingsScreen, and MainActivity")
