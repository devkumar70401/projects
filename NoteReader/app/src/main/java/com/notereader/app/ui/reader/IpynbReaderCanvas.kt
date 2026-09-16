package com.notereader.app.ui.reader

import android.graphics.BitmapFactory
import android.util.Base64
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Code
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.usecase.ReaderRawState
import com.notereader.app.rendering.ipynb.IpynbCell
import com.notereader.app.rendering.ipynb.IpynbNotebook
import com.notereader.app.rendering.ipynb.IpynbParser
import com.notereader.app.rendering.ipynb.OutputRenderable

@Composable
fun IpynbReaderCanvas(
    notebook: IpynbNotebook,
    initialCell: Int,
    settings: ReadingSettings,
    onProgressChange: (ReaderRawState) -> Unit,
    onToggleChrome: () -> Unit
) {
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = initialCell.coerceIn(0, notebook.cells.size - 1))

    LaunchedEffect(listState.firstVisibleItemIndex) {
        onProgressChange(
            ReaderRawState(
                cellIndex = listState.firstVisibleItemIndex,
                totalCellCount = notebook.cells.size
            )
        )
    }

    val bgColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.backgroundHex)) }
        .getOrDefault(Color(0xFF0F172A))
    val textColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.textHex)) }
        .getOrDefault(Color(0xFFE2E8F0))
    val codeBgColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.codeBackgroundHex)) }
        .getOrDefault(Color(0xFF1E293B))

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .pointerInput(Unit) {
                detectTapGestures { offset ->
                    val width = size.width
                    val x = offset.x
                    if (x in (width * 0.35f)..(width * 0.65f)) {
                        onToggleChrome()
                    }
                }
            }
    ) {
        LazyColumn(
            state = listState,
            contentPadding = PaddingValues(horizontal = settings.marginDp.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.fillMaxSize()
        ) {
            itemsIndexed(notebook.cells) { index, cell ->
                IpynbCellItem(
                    cell = cell,
                    cellIndex = index,
                    settings = settings,
                    textColor = textColor,
                    codeBgColor = codeBgColor
                )
            }
        }
    }
}

@Composable
fun IpynbCellItem(
    cell: IpynbCell,
    cellIndex: Int,
    settings: ReadingSettings,
    textColor: Color,
    codeBgColor: Color
) {
    val sourceText = remember(cell.source) {
        IpynbParser.normalizeSource(cell.source)
    }

    when (cell.cell_type) {
        "markdown" -> {
            Surface(
                color = Color.Transparent,
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = sourceText,
                    color = textColor,
                    fontSize = settings.fontSizeSp.sp,
                    lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp,
                    modifier = Modifier.padding(vertical = 4.dp)
                )
            }
        }
        "code" -> {
            ElevatedCard(
                shape = RoundedCornerShape(12.dp),
                colors = CardDefaults.elevatedCardColors(
                    containerColor = codeBgColor
                ),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(14.dp)) {
                    // Code cell header
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (cell.execution_count != null) "In [${cell.execution_count}]:" else "In [ ]:",
                            color = Color(0xFF38BDF8),
                            fontFamily = FontFamily.Monospace,
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                        Icon(
                            Icons.Default.Code,
                            contentDescription = null,
                            tint = Color(0xFF94A3B8),
                            modifier = Modifier.size(16.dp)
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Code source
                    Text(
                        text = sourceText,
                        color = Color(0xFFF1F5F9),
                        fontFamily = FontFamily.Monospace,
                        fontSize = (settings.fontSizeSp - 2).sp,
                        lineHeight = (settings.fontSizeSp * 1.3f).sp
                    )

                    // Outputs
                    if (!cell.outputs.isNullOrEmpty()) {
                        Spacer(modifier = Modifier.height(12.dp))
                        Divider(color = Color(0xFF334155))
                        Spacer(modifier = Modifier.height(8.dp))

                        cell.outputs.forEach { output ->
                            when (val rep = IpynbParser.bestOutputRepresentation(output)) {
                                is OutputRenderable.Base64Image -> {
                                    val bitmap = remember(rep.base64) {
                                        runCatching {
                                            val bytes = Base64.decode(rep.base64, Base64.DEFAULT)
                                            BitmapFactory.decodeByteArray(bytes, 0, bytes.size)
                                        }.getOrNull()
                                    }
                                    if (bitmap != null) {
                                        Image(
                                            bitmap = bitmap.asImageBitmap(),
                                            contentDescription = "Output Image",
                                            modifier = Modifier
                                                .fillMaxWidth()
                                                .clip(RoundedCornerShape(8.dp))
                                                .padding(vertical = 4.dp)
                                        )
                                    }
                                }
                                is OutputRenderable.PlainText -> {
                                    Text(
                                        text = rep.text,
                                        color = if (rep.isStderr) Color(0xFFF87171) else Color(0xFFCBD5E1),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp,
                                        modifier = Modifier.padding(vertical = 2.dp)
                                    )
                                }
                                is OutputRenderable.ErrorTraceback -> {
                                    Column(modifier = Modifier.padding(vertical = 4.dp)) {
                                        Text(
                                            text = "${rep.name}: ${rep.message}",
                                            color = Color(0xFFEF4444),
                                            fontFamily = FontFamily.Monospace,
                                            fontWeight = FontWeight.Bold,
                                            fontSize = 12.sp
                                        )
                                        rep.lines.take(10).forEach { line ->
                                            Text(
                                                text = line,
                                                color = Color(0xFFFCA5A5),
                                                fontFamily = FontFamily.Monospace,
                                                fontSize = 11.sp
                                            )
                                        }
                                    }
                                }
                                is OutputRenderable.UnsupportedPlaceholder -> {
                                    Text(
                                        text = rep.message,
                                        color = Color(0xFF94A3B8),
                                        fontFamily = FontFamily.Monospace,
                                        fontSize = 12.sp
                                    )
                                }
                                OutputRenderable.Empty -> {}
                            }
                        }
                    }
                }
            }
        }
    }
}
