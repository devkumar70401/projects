package com.notereader.app.ui.notebook

import android.app.Application
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
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
import androidx.compose.ui.graphics.FilterQuality
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalHapticFeedback
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
    val haptic = LocalHapticFeedback.current

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
        // Header & Breadcrumbs
        Surface(
            color = MaterialTheme.colorScheme.surface,
            border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(modifier = Modifier.padding(horizontal = 28.dp, vertical = 18.dp)) {
                // Breadcrumb row
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

                Spacer(modifier = Modifier.height(14.dp))

                // Notebook Title Row
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
                                .size(width = 8.dp, height = 34.dp)
                                .clip(RoundedCornerShape(4.dp))
                                .background(col)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = notebook.title,
                            fontSize = 28.sp,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                    }

                    // Import Document Button
                    Button(
                        onClick = {
                            haptic.performHapticFeedback(HapticFeedbackType.LongPress)
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
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = SpruceGreen)
                    ) {
                        Icon(Icons.Default.UploadFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Import Document", fontWeight = FontWeight.SemiBold)
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // OneNote Section Tabs with Spring Motion
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

                            val animatedBg by animateColorAsState(
                                targetValue = if (isSelected) secColor.copy(alpha = 0.15f) else MaterialTheme.colorScheme.surfaceVariant,
                                animationSpec = spring(dampingRatio = Spring.DampingRatioNoBouncy, stiffness = Spring.StiffnessMedium),
                                label = "sec_tab_bg"
                            )

                            Surface(
                                shape = RoundedCornerShape(12.dp),
                                color = animatedBg,
                                border = if (isSelected) androidx.compose.foundation.BorderStroke(1.5.dp, secColor) else null,
                                modifier = Modifier
                                    .springPress(targetScale = 0.96f) {
                                        viewModel.selectSection(sec.id)
                                    }
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 9.dp),
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
                            .size(36.dp)
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
            LinearProgressIndicator(modifier = Modifier.fillMaxWidth(), color = SpruceGreen)
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
                columns = GridCells.Adaptive(minSize = 230.dp),
                horizontalArrangement = Arrangement.spacedBy(22.dp),
                verticalArrangement = Arrangement.spacedBy(22.dp),
                contentPadding = PaddingValues(28.dp),
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .height(240.dp)
            .softShadow(elevation = 4.dp, shape = RoundedCornerShape(16.dp), alpha = 0.08f)
            .springPress(targetScale = 0.97f, onClick = onClick)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // High-res Thumbnail Container
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(140.dp)
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                if (page.thumbnailPath != null && File(page.thumbnailPath).exists()) {
                    AsyncImage(
                        model = File(page.thumbnailPath),
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        filterQuality = FilterQuality.High,
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
                        tint = badgeColor.copy(alpha = 0.7f),
                        modifier = Modifier.size(48.dp)
                    )
                }

                // Format badge
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = badgeColor,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(10.dp)
                ) {
                    Text(
                        text = page.format.name,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.Bold,
                        fontFamily = FontFamily.Monospace,
                        modifier = Modifier.padding(horizontal = 7.dp, vertical = 2.5.dp)
                    )
                }

                // 3-dot Menu
                Box(
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(6.dp)
                ) {
                    IconButton(
                        onClick = { showMenu = true },
                        modifier = Modifier.size(30.dp)
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
                    .padding(14.dp),
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = page.title,
                    fontSize = 14.5.sp,
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
                                .height(5.dp)
                                .clip(RoundedCornerShape(3.dp)),
                            color = SpruceGreen,
                            trackColor = MaterialTheme.colorScheme.surfaceVariant
                        )
                        Spacer(modifier = Modifier.width(10.dp))
                        Text(
                            text = "${(pageWithProgress.progressFraction * 100).toInt()}%",
                            fontSize = 12.sp,
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
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Section", fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Section Name (e.g. Chapter 1, Deep Learning)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
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
                                .size(34.dp)
                                .clip(CircleShape)
                                .background(col)
                                .clickable {
                                    selectedColor = hex
                                    haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                },
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
                onClick = {
                    haptic.performHapticFeedback(HapticFeedbackType.LongPress)
                    onCreate(title, selectedColor)
                },
                enabled = title.isNotBlank(),
                shape = RoundedCornerShape(12.dp),
                colors = ButtonDefaults.buttonColors(containerColor = SpruceGreen)
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
