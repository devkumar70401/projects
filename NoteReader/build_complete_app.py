import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

# 1. SideNav.kt
sidenav_kt = """package com.notereader.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.ui.theme.*

enum class NavTab(val id: String, val label: String) {
    HOME("home", "Home"),
    RECENT("recent", "Recent"),
    BOOKMARKS("bookmarks", "Bookmarks"),
    SEARCH("search", "Search"),
    SETTINGS("settings", "Settings")
}

@Composable
fun SideNav(
    activeTab: NavTab,
    onSelectTab: (NavTab) -> Unit,
    onOpenNewNotebook: () -> Unit,
    bookmarksCount: Int = 0,
    modifier: Modifier = Modifier
) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = modifier
            .fillMaxHeight()
            .width(260.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp, horizontal = 16.dp)
        ) {
            // Profile / Header
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 8.dp, vertical = 8.dp)
            ) {
                Box(
                    modifier = Modifier
                        .size(44.dp)
                        .clip(CircleShape)
                        .background(SpruceGreen),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.Person,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(24.dp)
                    )
                }
                Spacer(modifier = Modifier.width(12.dp))
                Column {
                    Text(
                        text = "NoteReader",
                        fontSize = 19.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "STUDENT / RESEARCH",
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Navigation Items
            val navItems = listOf(
                Triple(NavTab.HOME, "Home", Icons.Default.Home),
                Triple(NavTab.RECENT, "Recent", Icons.Default.History),
                Triple(NavTab.BOOKMARKS, "Bookmarks", Icons.Default.Bookmark),
                Triple(NavTab.SEARCH, "Search", Icons.Default.Search)
            )

            navItems.forEach { (tab, label, icon) ->
                val isSelected = activeTab == tab
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (isSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 3.dp)
                        .clickable { onSelectTab(tab) }
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            icon,
                            contentDescription = label,
                            tint = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(20.dp)
                        )
                        Spacer(modifier = Modifier.width(14.dp))
                        Text(
                            text = label,
                            fontSize = 15.sp,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            color = if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface,
                            modifier = Modifier.weight(1f)
                        )
                        if (tab == NavTab.BOOKMARKS && bookmarksCount > 0) {
                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)
                            ) {
                                Text(
                                    text = "$bookmarksCount",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = MaterialTheme.colorScheme.primary,
                                    modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.dp)
                                )
                            }
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick New Notebook button in sidebar
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenNewNotebook() }
            ) {
                Row(
                    modifier = Modifier.padding(vertical = 10.dp, horizontal = 12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "NEW NOTEBOOK",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }

            Spacer(modifier = Modifier.weight(1f))

            // Settings button at bottom
            val isSettingsSelected = activeTab == NavTab.SETTINGS
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (isSettingsSelected) MaterialTheme.colorScheme.surface else Color.Transparent,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelectTab(NavTab.SETTINGS) }
            ) {
                Row(
                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 11.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Settings,
                        contentDescription = "Settings",
                        tint = if (isSettingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(14.dp))
                    Text(
                        text = "Settings",
                        fontSize = 15.sp,
                        fontWeight = if (isSettingsSelected) FontWeight.Bold else FontWeight.Medium,
                        color = if (isSettingsSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
"""

# 2. ShelfScreen.kt with 3D Book Cover Spines
shelf_screen_kt = """package com.notereader.app.ui.shelf

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.Notebook
import com.notereader.app.ui.theme.*

@Composable
fun ShelfView(
    notebooks: List<Notebook>,
    onSelectNotebook: (Notebook) -> Unit,
    onOpenNewNotebookModal: () -> Unit,
    onDeleteNotebook: (Long) -> Unit
) {
    var isGridView by remember { mutableStateOf(true) }
    var filterQuery by remember { mutableStateOf("") }
    var showFilterInput by remember { mutableStateOf(false) }

    val filtered = notebooks.filter {
        it.title.contains(filterQuery, ignoreCase = true)
    }

    Box(modifier = Modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp, vertical = 24.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Bottom
            ) {
                Column {
                    Text(
                        text = "Shelf",
                        fontSize = 36.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Text(
                        text = "Your collected notebooks and research.",
                        fontSize = 15.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                // Controls
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (showFilterInput) {
                        OutlinedTextField(
                            value = filterQuery,
                            onValueChange = { filterQuery = it },
                            placeholder = { Text("Filter notebooks...") },
                            singleLine = true,
                            modifier = Modifier
                                .width(220.dp)
                                .height(50.dp)
                                .padding(end = 8.dp)
                        )
                    }
                    IconButton(onClick = { showFilterInput = !showFilterInput }) {
                        Icon(
                            Icons.Default.FilterList,
                            contentDescription = "Filter",
                            tint = if (showFilterInput) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    IconButton(onClick = { isGridView = !isGridView }) {
                        Icon(
                            if (isGridView) Icons.Default.ViewList else Icons.Default.GridView,
                            contentDescription = "Toggle View",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            if (filtered.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.AutoStories,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f),
                            modifier = Modifier.size(64.dp)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text(
                            text = "No notebooks found",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        Text(
                            text = "Create a new notebook to start organizing your documents.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(onClick = onOpenNewNotebookModal) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Notebook")
                        }
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 180.dp),
                    horizontalArrangement = Arrangement.spacedBy(24.dp),
                    verticalArrangement = Arrangement.spacedBy(24.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { notebook ->
                        RealisticBookCoverCard(
                            notebook = notebook,
                            onClick = { onSelectNotebook(notebook) },
                            onDelete = { onDeleteNotebook(notebook.id) }
                        )
                    }
                }
            } else {
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(12.dp),
                    contentPadding = PaddingValues(bottom = 96.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(filtered, key = { it.id }) { notebook ->
                        NotebookListRow(
                            notebook = notebook,
                            onClick = { onSelectNotebook(notebook) },
                            onDelete = { onDeleteNotebook(notebook.id) }
                        )
                    }
                }
            }
        }

        // FAB: + New Notebook
        FloatingActionButton(
            onClick = onOpenNewNotebookModal,
            containerColor = SpruceGreen,
            contentColor = Color.White,
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 18.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.Add, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("New Notebook", fontWeight = FontWeight.Bold, fontSize = 15.sp)
            }
        }
    }
}

@Composable
fun RealisticBookCoverCard(
    notebook: Notebook,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val coverColor = runCatching { Color(android.graphics.Color.parseColor(notebook.colorHex)) }
        .getOrDefault(SpruceGreen)
    var showMenu by remember { mutableStateOf(false) }

    Column(modifier = Modifier.width(190.dp)) {
        // Book Cover with Spine Shadow Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(240.dp)
                .shadow(6.dp, RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp, topStart = 3.dp, bottomStart = 3.dp))
                .clip(RoundedCornerShape(topEnd = 10.dp, bottomEnd = 10.dp, topStart = 3.dp, bottomStart = 3.dp))
                .background(coverColor)
                .clickable { onClick() }
                .padding(16.dp)
        ) {
            // Realistic spine gradient (darker on the left hinge)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(18.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.35f),
                                Color.Black.copy(alpha = 0.15f),
                                Color.Transparent
                            )
                        )
                    )
            )

            // Book Title & Description
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(start = 8.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = notebook.title,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 22.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Study notes & research",
                    fontSize = 11.sp,
                    color = Color.White.copy(alpha = 0.75f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }

        // Meta row beneath book cover
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 8.dp, start = 4.dp, end = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "${notebook.pageCount} ${if (notebook.pageCount == 1) "page" else "pages"}",
                fontSize = 13.sp,
                fontFamily = FontFamily.Monospace,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )

            Box {
                IconButton(
                    onClick = { showMenu = true },
                    modifier = Modifier.size(24.dp)
                ) {
                    Icon(
                        Icons.Default.MoreVert,
                        contentDescription = "Options",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(18.dp)
                    )
                }
                DropdownMenu(
                    expanded = showMenu,
                    onDismissRequest = { showMenu = false }
                ) {
                    DropdownMenuItem(
                        text = { Text("Open Notebook") },
                        onClick = {
                            showMenu = false
                            onClick()
                        },
                        leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) }
                    )
                    DropdownMenuItem(
                        text = { Text("Delete Notebook", color = MaterialTheme.colorScheme.error) },
                        onClick = {
                            showMenu = false
                            onDelete()
                        },
                        leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                    )
                }
            }
        }
    }
}

@Composable
fun NotebookListRow(
    notebook: Notebook,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val coverColor = runCatching { Color(android.graphics.Color.parseColor(notebook.colorHex)) }
        .getOrDefault(SpruceGreen)

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 36.dp, height = 50.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .background(coverColor)
            )
            Spacer(modifier = Modifier.width(16.dp))
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = notebook.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "${notebook.pageCount} pages",
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            IconButton(onClick = onDelete) {
                Icon(
                    Icons.Default.Delete,
                    contentDescription = "Delete",
                    tint = MaterialTheme.colorScheme.error
                )
            }
        }
    }
}
"""

# 3. NotebookScreen.kt with Breadcrumbs and Format Badges
notebook_screen_kt = """package com.notereader.app.ui.notebook

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.notereader.app.NoteReaderApplication
import com.notereader.app.domain.model.DocumentFormat
import com.notereader.app.domain.model.Notebook
import com.notereader.app.domain.model.PageWithProgress
import com.notereader.app.domain.model.Section
import com.notereader.app.ui.theme.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.io.File

class NotebookViewModel(
    application: Application,
    val notebookId: Long
) : AndroidViewModel(application) {
    private val app = application as NoteReaderApplication
    private val sectionRepo = app.sectionRepository
    private val pageRepo = app.pageRepository
    private val importUseCase = app.importFileUseCase

    val sections: StateFlow<List<Section>> = sectionRepo.observeSectionsForNotebook(notebookId)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _selectedSectionId = MutableStateFlow<Long?>(null)
    val selectedSectionId: StateFlow<Long?> = _selectedSectionId.asStateFlow()

    val pages: StateFlow<List<PageWithProgress>> = _selectedSectionId
        .flatMapLatest { secId ->
            if (secId == null) flowOf(emptyList())
            else pageRepo.observePagesForSection(secId)
        }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    private val _isImporting = MutableStateFlow(false)
    val isImporting: StateFlow<Boolean> = _isImporting.asStateFlow()

    fun selectSection(id: Long) {
        _selectedSectionId.value = id
    }

    fun createSection(title: String, colorHex: String) {
        viewModelScope.launch {
            val newId = sectionRepo.createSection(notebookId, title, colorHex)
            _selectedSectionId.value = newId
        }
    }

    fun deleteSection(id: Long) {
        viewModelScope.launch {
            sectionRepo.deleteSection(id)
            if (_selectedSectionId.value == id) {
                _selectedSectionId.value = sections.value.firstOrNull { it.id != id }?.id
            }
        }
    }

    fun deletePage(id: Long) {
        viewModelScope.launch {
            pageRepo.deletePage(id)
        }
    }

    fun importDocument(uri: Uri) {
        val secId = _selectedSectionId.value ?: return
        viewModelScope.launch {
            _isImporting.value = true
            importUseCase(secId, uri)
            _isImporting.value = false
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookDetailView(
    notebook: Notebook,
    viewModel: NotebookViewModel,
    onBackToShelf: () -> Unit,
    onPageClick: (Long) -> Unit
) {
    val sections by viewModel.sections.collectAsState()
    val selectedSecId by viewModel.selectedSectionId.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()

    var showCreateSectionDialog by remember { mutableStateOf(false) }

    LaunchedEffect(sections) {
        if (selectedSecId == null && sections.isNotEmpty()) {
            viewModel.selectSection(sections.first().id)
        }
    }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri: Uri? ->
        uri?.let { viewModel.importDocument(it) }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Breadcrumb & Header
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 24.dp, vertical = 16.dp)) {
                // Breadcrumbs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onBackToShelf() }
                ) {
                    Icon(
                        Icons.Default.Home,
                        contentDescription = "Home",
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "Shelf",
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontWeight = FontWeight.Medium
                    )
                    Icon(
                        Icons.Default.ChevronRight,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(16.dp)
                    )
                    Text(
                        text = notebook.title,
                        fontSize = 13.sp,
                        color = MaterialTheme.colorScheme.primary,
                        fontWeight = FontWeight.Bold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Notebook Title
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        val col = runCatching { Color(android.graphics.Color.parseColor(notebook.colorHex)) }
                            .getOrDefault(SpruceGreen)
                        Box(
                            modifier = Modifier
                                .size(width = 8.dp, height = 32.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(col)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = notebook.title,
                            fontSize = 26.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Import document button
                    Button(
                        onClick = {
                            launcher.launch(
                                arrayOf(
                                    "application/pdf",
                                    "text/plain",
                                    "text/markdown",
                                    "application/x-ipynb+json",
                                    "*/*"
                                )
                            )
                        },
                        colors = ButtonDefaults.buttonColors(containerColor = SpruceGreen)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Document")
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                // OneNote Section Tabs
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        items(sections, key = { it.id }) { sec ->
                            val isSelected = sec.id == selectedSecId
                            val secColor = runCatching { Color(android.graphics.Color.parseColor(sec.colorHex)) }
                                .getOrDefault(MaterialTheme.colorScheme.primary)

                            Surface(
                                shape = RoundedCornerShape(10.dp),
                                color = if (isSelected) secColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, secColor) else null,
                                modifier = Modifier.clickable { viewModel.selectSection(sec.id) }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Box(
                                        modifier = Modifier
                                            .size(8.dp)
                                            .clip(CircleShape)
                                            .background(secColor)
                                    )
                                    Spacer(modifier = Modifier.width(8.dp))
                                    Text(
                                        text = sec.title,
                                        fontSize = 14.sp,
                                        fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (isSelected) secColor else MaterialTheme.colorScheme.onSurface
                                    )
                                }
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))
                    IconButton(
                        onClick = { showCreateSectionDialog = true },
                        modifier = Modifier
                            .size(34.dp)
                            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), CircleShape)
                    ) {
                        Icon(
                            Icons.Default.Add,
                            contentDescription = "New Section",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                }
            }
        }

        if (isImporting) {
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
        }

        // Pages Grid
        if (sections.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text("No sections yet. Tap '+' to create a section.")
            }
        } else if (pages.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Icon(
                        Icons.Default.NoteAdd,
                        contentDescription = null,
                        modifier = Modifier.size(56.dp),
                        tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.4f)
                    )
                    Spacer(modifier = Modifier.height(12.dp))
                    Text("This section is empty", fontSize = 17.sp, fontWeight = FontWeight.SemiBold)
                    Text("Tap 'Import Document' to add PDF, Markdown, Text, or Jupyter notebook files.", fontSize = 13.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        } else {
            LazyVerticalGrid(
                columns = GridCells.Adaptive(minSize = 220.dp),
                horizontalArrangement = Arrangement.spacedBy(20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
                contentPadding = PaddingValues(24.dp),
                modifier = Modifier.fillMaxSize()
            ) {
                items(pages, key = { it.page.id }) { item ->
                    DocumentGridCard(
                        pageWithProgress = item,
                        onClick = { onPageClick(item.page.id) },
                        onDelete = { viewModel.deletePage(item.page.id) }
                    )
                }
            }
        }
    }

    if (showCreateSectionDialog) {
        CreateSectionDialog(
            onDismiss = { showCreateSectionDialog = false },
            onCreate = { title, color ->
                viewModel.createSection(title, color)
                showCreateSectionDialog = false
            }
        )
    }
}

@Composable
fun DocumentGridCard(
    pageWithProgress: PageWithProgress,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val page = pageWithProgress.page
    val badgeColor = when (page.format) {
        DocumentFormat.PDF -> BadgePdf
        DocumentFormat.MARKDOWN -> BadgeMd
        DocumentFormat.IPYNB -> BadgeIpynb
        DocumentFormat.TXT -> BadgeDoc
    }
    var showMenu by remember { mutableStateOf(false) }

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable { onClick() }
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Thumbnail container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(130.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (page.thumbnailPath != null && File(page.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(page.thumbnailPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                } else {
                    Icon(
                        when (page.format) {
                            DocumentFormat.PDF -> Icons.Default.PictureAsPdf
                            DocumentFormat.MARKDOWN -> Icons.Default.Description
                            DocumentFormat.IPYNB -> Icons.Default.Code
                            DocumentFormat.TXT -> Icons.Default.Article
                        },
                        contentDescription = null,
                        tint = badgeColor.copy(alpha = 0.6f),
                        modifier = Modifier.size(44.dp)
                    )
                }

                // Format badge
                Surface(
                    shape = RoundedCornerShape(4.dp),
                    color = badgeColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = page.format.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Menu
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(28.dp)
                    ) {
                        Icon(
                            Icons.Default.MoreVert,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Open Document") },
                            onClick = {
                                showMenu = false
                                onClick()
                            },
                            leadingIcon = { Icon(Icons.Default.Book, contentDescription = null) }
                        )
                        DropdownMenuItem(
                            text = { Text("Delete Document", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = { Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error) }
                        )
                    }
                }
            }

            // Info
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = page.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                if (pageWithProgress.progressFraction > 0f) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        LinearProgressIndicator(
                            progress = { pageWithProgress.progressFraction },
                            modifier = Modifier
                                .weight(1f)
                                .height(4.dp)
                                .clip(RoundedCornerShape(2.dp)),
                            color = SpruceGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "${(pageWithProgress.progressFraction * 100).toInt()}%",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            fontFamily = FontFamily.Monospace,
                            color = SpruceGreen
                        )
                    }
                } else {
                    val label = if (page.pageOrCellCount != null) {
                        "${page.pageOrCellCount} ${if (page.format == DocumentFormat.IPYNB) "cells" else "pages"}"
                    } else {
                        "${page.fileSizeBytes / 1024} KB"
                    }
                    Text(
                        text = label,
                        fontSize = 12.sp,
                        fontFamily = FontFamily.Monospace,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
        }
    }
}

@Composable
fun CreateSectionDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(NotePalette.first()) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Section") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Section Name (e.g. Chapter 1, Deep Learning)") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Pick a Color", fontSize = 14.sp, fontWeight = FontWeight.Medium)
                Spacer(modifier = Modifier.height(8.dp))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NotePalette.forEach { hex ->
                        val col = Color(android.graphics.Color.parseColor(hex))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable { selectedColor = hex },
                            contentAlignment = Alignment.Center
                        ) {
                            if (selectedColor == hex) {
                                Icon(
                                    Icons.Default.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onCreate(title, selectedColor) },
                enabled = title.isNotBlank()
            ) {
                Text("Create")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    )
}
"""

with open(f"{base_pkg}/ui/components/SideNav.kt", "w") as f:
    f.write(sidenav_kt)
with open(f"{base_pkg}/ui/shelf/ShelfScreen.kt", "w") as f:
    f.write(shelf_screen_kt)
with open(f"{base_pkg}/ui/notebook/NotebookScreen.kt", "w") as f:
    f.write(notebook_screen_kt)

print("Wrote SideNav, ShelfScreen, and NotebookScreen")
