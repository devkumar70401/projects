import os

base_pkg = "/home/dev/SE/NoteReader/app/src/main/java/com/notereader/app"

pdf_canvas = """package com.notereader.app.ui.reader

import android.graphics.Bitmap
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ChevronLeft
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.model.ReadingTheme
import com.notereader.app.domain.usecase.ReaderRawState
import com.notereader.app.rendering.pdf.PdfDocumentSession
import kotlinx.coroutines.launch

@Composable
fun PdfReaderCanvas(
    session: PdfDocumentSession,
    totalPages: Int,
    initialPage: Int,
    settings: ReadingSettings,
    onProgressChange: (ReaderRawState) -> Unit,
    onToggleChrome: () -> Unit
) {
    var currentPageIndex by remember { mutableStateOf(initialPage.coerceIn(0, (totalPages - 1).coerceAtLeast(0))) }
    var currentBitmap by remember { mutableStateOf<Bitmap?>(null) }
    var canvasSize by remember { mutableStateOf(IntSize(1200, 1800)) }
    val coroutineScope = rememberCoroutineScope()

    LaunchedEffect(currentPageIndex, canvasSize) {
        if (canvasSize.width > 0 && canvasSize.height > 0) {
            val bmp = session.renderPage(currentPageIndex, canvasSize.width, canvasSize.height)
            currentBitmap = bmp
            session.prefetchAround(currentPageIndex, canvasSize.width, canvasSize.height, coroutineScope)
            onProgressChange(
                ReaderRawState(
                    pdfPageIndex = currentPageIndex,
                    totalPdfPages = totalPages
                )
            )
        }
    }

    // Color matrix filter for themes
    val colorFilter = remember(settings.theme) {
        when (settings.theme) {
            ReadingTheme.DARK, ReadingTheme.AMOLED, ReadingTheme.NORD, ReadingTheme.FOREST -> {
                // Invert colors for dark reading
                val matrix = ColorMatrix(
                    floatArrayOf(
                        -1f, 0f, 0f, 0f, 255f,
                        0f, -1f, 0f, 0f, 255f,
                        0f, 0f, -1f, 0f, 255f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            ReadingTheme.SEPIA -> {
                val matrix = ColorMatrix(
                    floatArrayOf(
                        0.9f, 0f, 0f, 0f, 20f,
                        0f, 0.85f, 0f, 0f, 10f,
                        0f, 0f, 0.7f, 0f, 0f,
                        0f, 0f, 0f, 1f, 0f
                    )
                )
                ColorFilter.colorMatrix(matrix)
            }
            else -> null
        }
    }

    val bgColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.backgroundHex)) }
        .getOrDefault(Color.White)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .onSizeChanged { canvasSize = it }
            .pointerInput(totalPages) {
                detectTapGestures { offset ->
                    val width = size.width
                    val x = offset.x
                    when {
                        x < width * 0.30f -> {
                            if (currentPageIndex > 0) currentPageIndex--
                        }
                        x > width * 0.70f -> {
                            if (currentPageIndex < totalPages - 1) currentPageIndex++
                        }
                        else -> {
                            onToggleChrome()
                        }
                    }
                }
            },
        contentAlignment = Alignment.Center
    ) {
        if (currentBitmap != null) {
            Image(
                bitmap = currentBitmap!!.asImageBitmap(),
                contentDescription = "PDF Page ${currentPageIndex + 1}",
                colorFilter = colorFilter,
                modifier = Modifier.fillMaxSize()
            )
        } else {
            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
        }
    }
}
"""

reflowable_canvas = """package com.notereader.app.ui.reader

import android.text.Spanned
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.notereader.app.domain.model.ReadingSettings
import com.notereader.app.domain.usecase.ReaderRawState
import com.notereader.app.rendering.text.PaginationKey
import com.notereader.app.rendering.text.ReflowableTextPaginator
import com.notereader.app.rendering.text.TextRange

@Composable
fun ReflowableReaderCanvas(
    fullText: CharSequence,
    isMarkdown: Boolean,
    initialOffset: Int,
    settings: ReadingSettings,
    onProgressChange: (ReaderRawState) -> Unit,
    onToggleChrome: () -> Unit
) {
    val paginator = remember { ReflowableTextPaginator() }
    val density = LocalDensity.current.density
    var canvasSize by remember { mutableStateOf(IntSize(1200, 1800)) }

    val paginationKey = remember(settings, canvasSize) {
        PaginationKey(
            documentId = 0,
            fontFamily = settings.fontFamily,
            fontSizeSp = settings.fontSizeSp,
            lineSpacingMultiplier = settings.lineSpacingMultiplier,
            marginDp = settings.marginDp,
            viewportWidthPx = canvasSize.width.coerceAtLeast(300),
            viewportHeightPx = canvasSize.height.coerceAtLeast(300)
        )
    }

    val pageRanges = remember(fullText, paginationKey, density) {
        paginator.paginate(fullText, paginationKey, density)
    }

    var currentPageIndex by remember(pageRanges) {
        val mapped = paginator.mapOffsetToPageIndex(initialOffset, pageRanges)
        mutableStateOf(mapped.coerceIn(0, (pageRanges.size - 1).coerceAtLeast(0)))
    }

    LaunchedEffect(currentPageIndex, pageRanges) {
        if (pageRanges.isNotEmpty() && currentPageIndex in pageRanges.indices) {
            val range = pageRanges[currentPageIndex]
            onProgressChange(
                ReaderRawState(
                    charOffset = range.start,
                    totalCharCount = fullText.length
                )
            )
        }
    }

    val bgColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.backgroundHex)) }
        .getOrDefault(Color.White)
    val textColor = runCatching { Color(android.graphics.Color.parseColor(settings.theme.textHex)) }
        .getOrDefault(Color.Black)

    val currentText = remember(currentPageIndex, pageRanges, fullText) {
        if (pageRanges.isNotEmpty() && currentPageIndex in pageRanges.indices) {
            val range = pageRanges[currentPageIndex]
            fullText.subSequence(range.start, range.end).toString()
        } else ""
    }

    val font = when (settings.fontFamily.lowercase()) {
        "serif" -> FontFamily.Serif
        "monospace" -> FontFamily.Monospace
        else -> FontFamily.SansSerif
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(bgColor)
            .onSizeChanged { canvasSize = it }
            .pointerInput(pageRanges.size) {
                detectTapGestures { offset ->
                    val width = size.width
                    val x = offset.x
                    when {
                        x < width * 0.30f -> {
                            if (currentPageIndex > 0) currentPageIndex--
                        }
                        x > width * 0.70f -> {
                            if (currentPageIndex < pageRanges.size - 1) currentPageIndex++
                        }
                        else -> {
                            onToggleChrome()
                        }
                    }
                }
            }
            .padding(horizontal = settings.marginDp.dp, vertical = (settings.marginDp + 16).dp)
    ) {
        Text(
            text = currentText,
            color = textColor,
            fontSize = settings.fontSizeSp.sp,
            lineHeight = (settings.fontSizeSp * settings.lineSpacingMultiplier).sp,
            fontFamily = font,
            modifier = Modifier.fillMaxSize()
        )
    }
}
"""

ipynb_canvas = """package com.notereader.app.ui.reader

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
"""

ink_overlay = """package com.notereader.app.ui.reader

import android.view.MotionEvent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInteropFilter

data class DrawnStroke(
    val path: Path,
    val color: Color,
    val strokeWidth: Float
)

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun InkAnnotationOverlay(
    isInkMode: Boolean,
    inkColorHex: String,
    baseStrokeWidth: Float,
    modifier: Modifier = Modifier
) {
    if (!isInkMode) return

    val strokes = remember { mutableStateListOf<DrawnStroke>() }
    var currentPath by remember { mutableStateOf<Path?>(null) }
    val inkColor = remember(inkColorHex) {
        runCatching { Color(android.graphics.Color.parseColor(inkColorHex)) }
            .getOrDefault(Color.Red)
    }

    Canvas(
        modifier = modifier
            .fillMaxSize()
            .pointerInteropFilter { motionEvent ->
                // Support stylus drawing or touch fallback in ink mode
                val x = motionEvent.x
                val y = motionEvent.y
                val pressure = motionEvent.pressure.coerceIn(0.2f, 1.5f)
                val width = baseStrokeWidth * pressure * 2f

                when (motionEvent.actionMasked) {
                    MotionEvent.ACTION_DOWN -> {
                        val p = Path().apply { moveTo(x, y) }
                        currentPath = p
                        true
                    }
                    MotionEvent.ACTION_MOVE -> {
                        currentPath?.lineTo(x, y)
                        true
                    }
                    MotionEvent.ACTION_UP, MotionEvent.ACTION_CANCEL -> {
                        currentPath?.let {
                            it.lineTo(x, y)
                            strokes.add(DrawnStroke(it, inkColor, width))
                        }
                        currentPath = null
                        true
                    }
                    else -> false
                }
            }
    ) {
        // Draw existing strokes
        strokes.forEach { stroke ->
            drawPath(
                path = stroke.path,
                color = stroke.color,
                style = Stroke(
                    width = stroke.strokeWidth,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }

        // Draw active stroke
        currentPath?.let {
            drawPath(
                path = it,
                color = inkColor,
                style = Stroke(
                    width = baseStrokeWidth * 2f,
                    cap = StrokeCap.Round,
                    join = StrokeJoin.Round
                )
            )
        }
    }
}
"""

with open(f"{base_pkg}/ui/reader/PdfReaderCanvas.kt", "w") as f:
    f.write(pdf_canvas)
with open(f"{base_pkg}/ui/reader/ReflowableReaderCanvas.kt", "w") as f:
    f.write(reflowable_canvas)
with open(f"{base_pkg}/ui/reader/IpynbReaderCanvas.kt", "w") as f:
    f.write(ipynb_canvas)
with open(f"{base_pkg}/ui/reader/InkAnnotationOverlay.kt", "w") as f:
    f.write(ink_overlay)

print("Created Canvases and InkOverlay")
