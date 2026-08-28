// GPU-accelerated theme transition engine for Jetpack Compose.
// Provides 12 distinct native reveal transitions (Circular Reveal, Iris, Wipe, Blinds, Split, etc.)
// inspired by react-native-nitro-theme-transition.
package com.ray.flowmeter.ui.theme

import android.graphics.Bitmap
import android.graphics.Canvas
import android.view.View
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathFillType
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.drawscope.clipPath
import androidx.compose.ui.graphics.drawscope.clipRect
import androidx.compose.ui.graphics.drawscope.withTransform
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.IntSize
import kotlinx.coroutines.launch
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.hypot
import kotlin.math.sin

/**
 * Transition kinds supported by the Nitro Theme Transition engine.
 */
enum class ThemeTransitionKind(val key: String) {
    NONE("NONE"),
    CIRCULAR_REVEAL("CIRCULAR_REVEAL"),
    CIRCULAR_REVEAL_INVERSE("CIRCULAR_REVEAL_INVERSE"),
    IRIS_HEXAGON("IRIS_HEXAGON"),
    IRIS_DIAMOND("IRIS_DIAMOND"),
    WIPE_RIGHT("WIPE_RIGHT"),
    WIPE_DOWN("WIPE_DOWN"),
    SPLIT("SPLIT"),
    BARN_DOOR("BARN_DOOR"),
    BLINDS("BLINDS"),
    RIPPLE("RIPPLE"),
    STRIPES("STRIPES"),
    FADE("FADE"),
    ZOOM("ZOOM"),
    RANDOM("RANDOM");

    companion object {
        val concreteKinds = listOf(
            CIRCULAR_REVEAL,
            CIRCULAR_REVEAL_INVERSE,
            IRIS_HEXAGON,
            IRIS_DIAMOND,
            WIPE_RIGHT,
            WIPE_DOWN,
            SPLIT,
            BARN_DOOR,
            BLINDS,
            RIPPLE,
            STRIPES,
            FADE,
            ZOOM
        )

        fun fromKey(key: String?): ThemeTransitionKind {
            return entries.find { it.key.equals(key, ignoreCase = true) } ?: CIRCULAR_REVEAL
        }
    }
}

/**
 * Controller interface exposed via [LocalThemeTransition].
 */
interface ThemeTransitionController {
    fun startTransition(
        origin: Offset? = null,
        kind: ThemeTransitionKind? = null,
        durationMs: Int = 550,
        onChange: () -> Unit
    )
}

val LocalThemeTransition = staticCompositionLocalOf<ThemeTransitionController> {
    object : ThemeTransitionController {
        override fun startTransition(
            origin: Offset?,
            kind: ThemeTransitionKind?,
            durationMs: Int,
            onChange: () -> Unit
        ) {
            onChange()
        }
    }
}

/**
 * Captures the current content of a View into a software [Bitmap].
 */
fun captureViewToBitmap(view: View): Bitmap? {
    if (view.width <= 0 || view.height <= 0) return null
    return try {
        val bitmap = Bitmap.createBitmap(view.width, view.height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(bitmap)
        view.draw(canvas)
        bitmap
    } catch (_: Exception) {
        null
    }
}

/**
 * Root container providing theme transition interception and GPU overlay animation.
 */
@Composable
fun ThemeTransitionContainer(
    defaultKind: ThemeTransitionKind = ThemeTransitionKind.CIRCULAR_REVEAL,
    content: @Composable () -> Unit
) {
    val view = LocalView.current
    val coroutineScope = rememberCoroutineScope()
    var transitionSnapshot by remember { mutableStateOf<ImageBitmap?>(null) }
    var transitionOrigin by remember { mutableStateOf<Offset?>(null) }
    var activeKind by remember { mutableStateOf(defaultKind) }
    val animatable = remember { Animatable(0f) }
    var rawBitmap by remember { mutableStateOf<Bitmap?>(null) }

    val controller = remember(defaultKind, view) {
        object : ThemeTransitionController {
            override fun startTransition(
                origin: Offset?,
                kind: ThemeTransitionKind?,
                durationMs: Int,
                onChange: () -> Unit
            ) {
                val chosenKind = kind ?: defaultKind
                if (chosenKind == ThemeTransitionKind.NONE) {
                    onChange()
                    return
                }

                if (animatable.isRunning) {
                    onChange()
                    return
                }

                val captured = captureViewToBitmap(view)
                if (captured == null) {
                    onChange()
                    return
                }

                rawBitmap?.recycle()
                rawBitmap = captured
                transitionSnapshot = captured.asImageBitmap()
                transitionOrigin = origin ?: Offset(view.width / 2f, view.height / 2f)

                activeKind = if (chosenKind == ThemeTransitionKind.RANDOM) {
                    ThemeTransitionKind.concreteKinds.random()
                } else {
                    chosenKind
                }

                onChange()

                coroutineScope.launch {
                    animatable.snapTo(0f)
                    animatable.animateTo(
                        targetValue = 1f,
                        animationSpec = tween(durationMillis = durationMs, easing = FastOutSlowInEasing)
                    )
                    transitionSnapshot = null
                    rawBitmap?.recycle()
                    rawBitmap = null
                }
            }
        }
    }

    CompositionLocalProvider(LocalThemeTransition provides controller) {
        Box(modifier = Modifier.fillMaxSize()) {
            content()

            val snapshot = transitionSnapshot
            val origin = transitionOrigin
            if (snapshot != null && origin != null && animatable.value < 1f) {
                ThemeTransitionOverlay(
                    snapshot = snapshot,
                    origin = origin,
                    kind = activeKind,
                    progress = animatable.value,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
    }
}

/**
 * Overlay Canvas that renders the previous theme state snapshot animated away
 * with GPU-accelerated clipping and transform algorithms.
 */
@Composable
fun ThemeTransitionOverlay(
    snapshot: ImageBitmap,
    origin: Offset,
    kind: ThemeTransitionKind,
    progress: Float,
    modifier: Modifier = Modifier
) {
    Canvas(modifier = modifier) {
        val w = size.width
        val h = size.height
        val dstSize = IntSize(w.toInt(), h.toInt())

        when (kind) {
            ThemeTransitionKind.NONE -> {
                // No-op
            }

            ThemeTransitionKind.CIRCULAR_REVEAL -> {
                val maxRadius = hypot(maxOf(origin.x, w - origin.x), maxOf(origin.y, h - origin.y))
                val currentRadius = progress * maxRadius
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, w, h))
                    addOval(Rect(center = origin, radius = currentRadius))
                }
                clipPath(path) {
                    drawImage(snapshot, dstSize = dstSize)
                }
            }

            ThemeTransitionKind.CIRCULAR_REVEAL_INVERSE -> {
                val maxRadius = hypot(maxOf(origin.x, w - origin.x), maxOf(origin.y, h - origin.y))
                val currentRadius = (1f - progress) * maxRadius
                val path = Path().apply {
                    addOval(Rect(center = origin, radius = currentRadius))
                }
                clipPath(path) {
                    drawImage(snapshot, dstSize = dstSize)
                }
            }

            ThemeTransitionKind.IRIS_HEXAGON -> {
                val maxRadius = hypot(maxOf(origin.x, w - origin.x), maxOf(origin.y, h - origin.y)) * 1.15f
                val radius = progress * maxRadius
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, w, h))
                    for (i in 0 until 6) {
                        val angle = (i * 60f) * (PI / 180f)
                        val px = origin.x + radius * cos(angle).toFloat()
                        val py = origin.y + radius * sin(angle).toFloat()
                        if (i == 0) moveTo(px, py) else lineTo(px, py)
                    }
                    close()
                }
                clipPath(path) {
                    drawImage(snapshot, dstSize = dstSize)
                }
            }

            ThemeTransitionKind.IRIS_DIAMOND -> {
                val maxRadius = hypot(maxOf(origin.x, w - origin.x), maxOf(origin.y, h - origin.y)) * 1.4f
                val radius = progress * maxRadius
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, w, h))
                    moveTo(origin.x, origin.y - radius)
                    lineTo(origin.x + radius, origin.y)
                    lineTo(origin.x, origin.y + radius)
                    lineTo(origin.x - radius, origin.y)
                    close()
                }
                clipPath(path) {
                    drawImage(snapshot, dstSize = dstSize)
                }
            }

            ThemeTransitionKind.WIPE_RIGHT -> {
                val currentX = progress * w
                clipRect(left = currentX, top = 0f, right = w, bottom = h) {
                    drawImage(snapshot, dstSize = dstSize)
                }
            }

            ThemeTransitionKind.WIPE_DOWN -> {
                val currentY = progress * h
                clipRect(left = 0f, top = currentY, right = w, bottom = h) {
                    drawImage(snapshot, dstSize = dstSize)
                }
            }

            ThemeTransitionKind.SPLIT -> {
                val halfH = h / 2f
                val offset = progress * halfH
                val alpha = 1f - (progress * 0.4f)
                // Top half
                clipRect(left = 0f, top = 0f, right = w, bottom = halfH - offset) {
                    withTransform({ translate(0f, -offset) }) {
                        drawImage(snapshot, dstSize = dstSize, alpha = alpha)
                    }
                }
                // Bottom half
                clipRect(left = 0f, top = halfH + offset, right = w, bottom = h) {
                    withTransform({ translate(0f, offset) }) {
                        drawImage(snapshot, dstSize = dstSize, alpha = alpha)
                    }
                }
            }

            ThemeTransitionKind.BARN_DOOR -> {
                val halfW = w / 2f
                val offset = progress * halfW
                val alpha = 1f - (progress * 0.4f)
                // Left door
                clipRect(left = 0f, top = 0f, right = halfW - offset, bottom = h) {
                    withTransform({ translate(-offset, 0f) }) {
                        drawImage(snapshot, dstSize = dstSize, alpha = alpha)
                    }
                }
                // Right door
                clipRect(left = halfW + offset, top = 0f, right = w, bottom = h) {
                    withTransform({ translate(offset, 0f) }) {
                        drawImage(snapshot, dstSize = dstSize, alpha = alpha)
                    }
                }
            }

            ThemeTransitionKind.BLINDS -> {
                val bandCount = 8
                val bandHeight = h / bandCount
                val currentBandHeight = bandHeight * (1f - progress)
                for (i in 0 until bandCount) {
                    val top = i * bandHeight + (bandHeight - currentBandHeight) / 2f
                    val bottom = top + currentBandHeight
                    clipRect(left = 0f, top = top, right = w, bottom = bottom) {
                        drawImage(snapshot, dstSize = dstSize)
                    }
                }
            }

            ThemeTransitionKind.STRIPES -> {
                val bandCount = 10
                val bandHeight = h / bandCount
                for (i in 0 until bandCount) {
                    val top = i * bandHeight
                    val bottom = top + bandHeight
                    val direction = if (i % 2 == 0) -1f else 1f
                    val shiftX = direction * progress * w
                    clipRect(left = 0f, top = top, right = w, bottom = bottom) {
                        withTransform({ translate(shiftX, 0f) }) {
                            drawImage(snapshot, dstSize = dstSize, alpha = 1f - progress * 0.3f)
                        }
                    }
                }
            }

            ThemeTransitionKind.RIPPLE -> {
                val maxRadius = hypot(maxOf(origin.x, w - origin.x), maxOf(origin.y, h - origin.y))
                val ringRadius = progress * maxRadius
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, w, h))
                    addOval(Rect(center = origin, radius = ringRadius))
                }
                clipPath(path) {
                    drawImage(snapshot, dstSize = dstSize, alpha = 1f - (progress * 0.3f))
                }
            }

            ThemeTransitionKind.FADE -> {
                drawImage(
                    image = snapshot,
                    dstSize = dstSize,
                    alpha = 1f - progress
                )
            }

            ThemeTransitionKind.ZOOM -> {
                val scale = 1f + (progress * 0.18f)
                val alpha = (1f - progress).coerceIn(0f, 1f)
                withTransform({
                    scale(scale, scale, origin)
                }) {
                    drawImage(
                        image = snapshot,
                        dstSize = dstSize,
                        alpha = alpha
                    )
                }
            }

            ThemeTransitionKind.RANDOM -> {
                // Resolved in container, fallback to CIRCULAR_REVEAL
                val maxRadius = hypot(maxOf(origin.x, w - origin.x), maxOf(origin.y, h - origin.y))
                val currentRadius = progress * maxRadius
                val path = Path().apply {
                    fillType = PathFillType.EvenOdd
                    addRect(Rect(0f, 0f, w, h))
                    addOval(Rect(center = origin, radius = currentRadius))
                }
                clipPath(path) {
                    drawImage(snapshot, dstSize = dstSize)
                }
            }
        }
    }
}
