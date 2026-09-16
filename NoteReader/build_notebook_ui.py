import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

notebook_code = """package com.notereader.app.ui.notebook

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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import coil.compose.AsyncImage
import com.notereader.app.NoteReaderApplication
import com.notereader.app.domain.model.DocumentFormat
import com.notereader.app.domain.model.PageWithProgress
import com.notereader.app.domain.model.Section
import com.notereader.app.ui.theme.NotePalette
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

    private val _importError = MutableStateFlow<String?>(null)
    val importError: StateFlow<String?> = _importError.asStateFlow()

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
            _importError.value = null
            val res = importUseCase(secId, uri)
            _isImporting.value = false
            if (res.isFailure) {
                _importError.value = res.exceptionOrNull()?.message ?: "Import failed"
            }
        }
    }

    fun clearImportError() {
        _importError.value = null
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NotebookScreen(
    viewModel: NotebookViewModel,
    onBackClick: () -> Unit,
    onPageClick: (Long) -> Unit
) {
    val sections by viewModel.sections.collectAsState()
    val selectedSecId by viewModel.selectedSectionId.collectAsState()
    val pages by viewModel.pages.collectAsState()
    val isImporting by viewModel.isImporting.collectAsState()
    val importError by viewModel.importError.collectAsState()

    var showCreateSectionDialog by remember { mutableStateOf(false) }

    // Auto-select first section when sections load
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

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Notebook", fontWeight = FontWeight.Bold) },
                navigationIcon = {
                    IconButton(onClick = onBackClick) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        floatingActionButton = {
            if (selectedSecId != null) {
                ExtendedFloatingActionButton(
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
                    icon = { Icon(Icons.Default.UploadFile, contentDescription = null) },
                    text = { Text("Import Document") },
                    containerColor = MaterialTheme.colorScheme.primary
                )
            }
        }
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
        ) {
            // OneNote-style Section Tabs
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(MaterialTheme.colorScheme.surface)
                    .padding(vertical = 8.dp, horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.weight(1f)
                ) {
                    items(sections, key = { it.id }) { sec ->
                        val isSelected = sec.id == selectedSecId
                        val color = runCatching { Color(android.graphics.Color.parseColor(sec.colorHex)) }
                            .getOrDefault(MaterialTheme.colorScheme.primary)

                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) color.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                            border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, color) else null,
                            modifier = Modifier
                                .clickable { viewModel.selectSection(sec.id) }
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .clip(CircleShape)
                                        .background(color)
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Text(
                                    text = sec.title,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
                                    color = if (isSelected) color else MaterialTheme.colorScheme.onSurface,
                                    fontSize = 14.sp
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.width(8.dp))
                IconButton(
                    onClick = { showCreateSectionDialog = true },
                    modifier = Modifier
                        .size(36.dp)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f), CircleShape)
                ) {
                    Icon(
                        Icons.Default.Add,
                        contentDescription = "New Section",
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }

            Divider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.5f))

            if (isImporting) {
                LinearProgressIndicator(modifier = Modifier.fillMaxWidth())
            }

            // Page Content Grid
            if (sections.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.FolderOpen,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("No Sections Yet", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Tap '+' above to create a Section (e.g. 'Math', 'Physics', 'Notes')",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else if (pages.isEmpty()) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(24.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.NoteAdd,
                            contentDescription = null,
                            modifier = Modifier.size(64.dp),
                            tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.5f)
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Text("This section is empty", fontSize = 18.sp, fontWeight = FontWeight.Medium)
                        Text(
                            "Tap 'Import Document' to add PDF, Markdown, Text, or Jupyter notebooks.",
                            fontSize = 14.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 200.dp),
                    contentPadding = PaddingValues(20.dp),
                    horizontalArrangement = Arrangement.spacedBy(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                    modifier = Modifier.fillMaxSize()
                ) {
                    items(pages, key = { it.page.id }) { pageWithProgress ->
                        PageCard(
                            pageWithProgress = pageWithProgress,
                            onClick = { onPageClick(pageWithProgress.page.id) },
                            onDelete = { viewModel.deletePage(pageWithProgress.page.id) }
                        )
                    }
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

    importError?.let { err ->
        AlertDialog(
            onDismissRequest = { viewModel.clearImportError() },
            title = { Text("Import Error") },
            text = { Text(err) },
            confirmButton = {
                TextButton(onClick = { viewModel.clearImportError() }) {
                    Text("OK")
                }
            }
        )
    }
}

@Composable
fun PageCard(
    pageWithProgress: PageWithProgress,
    onClick: () -> Unit,
    onDelete: () -> Unit
) {
    val page = pageWithProgress.page
    val formatColor = when (page.format) {
        DocumentFormat.PDF -> Color(0xFFEF4444)
        DocumentFormat.MARKDOWN -> Color(0xFF3B82F6)
        DocumentFormat.IPYNB -> Color(0xFFF59E0B)
        DocumentFormat.TXT -> Color(0xFF10B981)
    }
    var showMenu by remember { mutableStateOf(false) }

    ElevatedCard(
        modifier = Modifier
            .fillMaxWidth()
            .height(230.dp)
            .clickable { onClick() },
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.elevatedCardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Thumbnail / Cover area
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
                        tint = formatColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Format badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = formatColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(8.dp)
                ) {
                    Text(
                        text = page.format.name,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }

                // Menu button
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
                            contentDescription = "Options",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    DropdownMenu(
                        expanded = showMenu,
                        onDismissRequest = { showMenu = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text("Delete Document", color = MaterialTheme.colorScheme.error) },
                            onClick = {
                                showMenu = false
                                onDelete()
                            },
                            leadingIcon = {
                                Icon(Icons.Default.Delete, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                            }
                        )
                    }
                }
            }

            // Info area
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = page.title,
                    fontSize = 14.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                    color = MaterialTheme.colorScheme.onSurface
                )

                Column {
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
                                color = MaterialTheme.colorScheme.primary,
                                trackColor = MaterialTheme.colorScheme.surfaceVariant
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "${(pageWithProgress.progressFraction * 100).toInt()}%",
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    } else {
                        val subtitle = if (page.pageOrCellCount != null) {
                            when (page.format) {
                                DocumentFormat.PDF -> "${page.pageOrCellCount} pages"
                                DocumentFormat.IPYNB -> "${page.pageOrCellCount} cells"
                                else -> "${page.fileSizeBytes / 1024} KB"
                            }
                        } else {
                            "${page.fileSizeBytes / 1024} KB"
                        }
                        Text(
                            text = subtitle,
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
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
    var selectedColor by remember { mutableStateOf(NotePalette[1]) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Section") },
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Section Name (e.g. Chapter 1, Math)") },
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

with open(f"{base_pkg}/ui/notebook/NotebookScreen.kt", "w") as f:
    f.write(notebook_code)

print("Created NotebookScreen.kt successfully!")
