package com.notereader.app.ui.theme

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.gestures.waitForUpOrCancellation
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

fun Dp.toPx(density: Density): Int = with(density) { this@toPx.roundToPx() }

/**
 * Spring-based tactile click feedback matching Samsung Notes interaction model.
 * Scales slightly down on press and bounces back smoothly with zero frame delay.
 */
fun Modifier.springPress(
    targetScale: Float = 0.96f,
    onClick: (() -> Unit)? = null
): Modifier = composed {
    var isPressed by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isPressed) targetScale else 1.0f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioLowBouncy,
            stiffness = Spring.StiffnessMediumLow
        ),
        label = "spring_press"
    )

    this
        .graphicsLayer {
            scaleX = scale
            scaleY = scale
        }
        .pointerInput(Unit) {
            awaitPointerEventScope {
                while (true) {
                    awaitFirstDown(requireUnconsumed = false)
                    isPressed = true
                    val up = waitForUpOrCancellation()
                    isPressed = false
                    if (up != null && onClick != null) {
                        onClick()
                    }
                }
            }
        }
}

/**
 * Soft ambient shadow system matching first-party Samsung apps.
 */
fun Modifier.softShadow(
    elevation: Dp = 4.dp,
    shape: Shape = RoundedCornerShape(16.dp),
    alpha: Float = 0.08f
): Modifier = this.shadow(
    elevation = elevation,
    shape = shape,
    ambientColor = Color.Black.copy(alpha = alpha),
    spotColor = Color.Black.copy(alpha = alpha * 1.5f)
)
