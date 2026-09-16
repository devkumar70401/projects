package com.notereader.app.ui.shelf

import android.app.Application
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.notereader.app.NoteReaderApplication
import com.notereader.app.domain.model.Notebook
import com.notereader.app.domain.model.PageWithProgress
import com.notereader.app.ui.theme.*
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class ShelfViewModel(application: Application) : AndroidViewModel(application) {
    private val app = application as NoteReaderApplication
    private val notebookRepo = app.notebookRepository
    private val pageRepo = app.pageRepository
    private val bookmarkRepo = app.bookmarkRepository

    val notebooks: StateFlow<List<Notebook>> = notebookRepo.observeAllNotebooks()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    val recentlyReadPages: StateFlow<List<PageWithProgress>> = pageRepo.observeRecentlyReadPages(20)
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun createNotebook(title: String, colorHex: String) {
        viewModelScope.launch {
            notebookRepo.createNotebook(title, colorHex)
        }
    }

    fun deleteNotebook(id: Long) {
        viewModelScope.launch {
            notebookRepo.deleteNotebook(id)
        }
    }

    fun deleteBookmark(id: Long) {
        viewModelScope.launch {
            bookmarkRepo.deleteBookmark(id)
        }
    }
}

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
                            shape = RoundedCornerShape(12.dp),
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
                        Button(
                            onClick = onOpenNewNotebookModal,
                            shape = RoundedCornerShape(12.dp),
                            colors = ButtonDefaults.buttonColors(containerColor = SpruceGreen)
                        ) {
                            Icon(Icons.Default.Add, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text("Create Notebook")
                        }
                    }
                }
            } else if (isGridView) {
                LazyVerticalGrid(
                    columns = GridCells.Adaptive(minSize = 190.dp),
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

        // Floating Action Button: + New Notebook
        FloatingActionButton(
            onClick = onOpenNewNotebookModal,
            containerColor = SpruceGreen,
            contentColor = Color.White,
            shape = RoundedCornerShape(24.dp),
            modifier = Modifier
                .align(Alignment.BottomEnd)
                .padding(32.dp)
                .softShadow(elevation = 8.dp, shape = RoundedCornerShape(24.dp), alpha = 0.15f)
        ) {
            Row(
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 14.dp),
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

    Column(
        modifier = Modifier
            .width(200.dp)
            .springPress(targetScale = 0.95f, onClick = onClick)
    ) {
        // Book Cover Card with Soft Shadows and 3D Spine Gradient
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(250.dp)
                .softShadow(elevation = 6.dp, shape = RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp, topStart = 4.dp, bottomStart = 4.dp), alpha = 0.12f)
                .clip(RoundedCornerShape(topEnd = 14.dp, bottomEnd = 14.dp, topStart = 4.dp, bottomStart = 4.dp))
                .background(coverColor)
                .padding(18.dp)
        ) {
            // Realistic spine gradient (darker on the left hinge)
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .width(20.dp)
                    .align(Alignment.CenterStart)
                    .background(
                        Brush.horizontalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.40f),
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
                    .padding(start = 10.dp),
                verticalArrangement = Arrangement.Bottom
            ) {
                Text(
                    text = notebook.title,
                    fontSize = 19.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White,
                    maxLines = 3,
                    overflow = TextOverflow.Ellipsis,
                    lineHeight = 23.sp
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = "Study notes & research",
                    fontSize = 12.sp,
                    color = Color.White.copy(alpha = 0.80f),
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
        shape = RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surface,
        border = androidx.compose.foundation.BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
        modifier = Modifier
            .fillMaxWidth()
            .springPress(targetScale = 0.98f, onClick = onClick)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(width = 38.dp, height = 52.dp)
                    .clip(RoundedCornerShape(6.dp))
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

@Composable
fun CreateNotebookDialog(
    onDismiss: () -> Unit,
    onCreate: (String, String) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var selectedColor by remember { mutableStateOf(NotePalette.first()) }
    val haptic = LocalHapticFeedback.current

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("New Notebook", fontWeight = FontWeight.Bold) },
        shape = RoundedCornerShape(16.dp),
        text = {
            Column {
                OutlinedTextField(
                    value = title,
                    onValueChange = { title = it },
                    label = { Text("Notebook Title (e.g. Research, ML Papers)") },
                    singleLine = true,
                    shape = RoundedCornerShape(12.dp),
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(modifier = Modifier.height(16.dp))
                Text("Cover Color", fontSize = 14.sp, fontWeight = FontWeight.Medium)
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
