package com.notereader.app.ui.reader

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
