package com.notereader.app.ui.settings

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
