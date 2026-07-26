// Custom accent color picker dialog allowing user to choose custom MD3 theme accents.
package com.ray.flowmeter.ui.dialogs

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.res.stringResource
import androidx.core.graphics.toColorInt
import com.ray.flowmeter.R
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardCapitalization
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.ray.flowmeter.ui.theme.bounceClick
import kotlin.math.*

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccentColorDialog(
    currentColor: Long?,
    onDismiss: () -> Unit,
    onSelect: (Long?) -> Unit
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    
    val initialHsv = remember(currentColor) {
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(currentColor?.toInt() ?: 0xFF0056D2.toInt(), hsv)
        hsv
    }
    
    var hue by remember { mutableFloatStateOf(initialHsv[0]) }
    var saturation by remember { mutableFloatStateOf(initialHsv[1]) }
    var value by remember { mutableFloatStateOf(initialHsv[2]) }
    
    val pickedColor = remember(hue, saturation, value) {
        Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, value)))
    }
    
    var hexText by remember { mutableStateOf(pickedColor.toHexString()) }
    
    LaunchedEffect(pickedColor) {
        val newHex = pickedColor.toHexString()
        if (hexText.uppercase() != newHex.uppercase()) {
            hexText = newHex
        }
    }

    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        dragHandle = { BottomSheetDefaults.DragHandle() },
        containerColor = MaterialTheme.colorScheme.surfaceContainer,
        shape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp),
        properties = ModalBottomSheetProperties(shouldDismissOnBackPress = false)
    ) {
        AnimatedDialogContent(onBack = onDismiss) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp)
                    .padding(bottom = 40.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.title_custom_color),
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.ExtraBold,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.fillMaxWidth().padding(bottom = 20.dp),
                    textAlign = TextAlign.Center
                )

                val presets = listOf(
                    Color(0xFF0056D2),
                    Color(0xFFD32F2F),
                    Color(0xFF388E3C),
                    Color(0xFFFBC02D),
                    Color(0xFF7B1FA2),
                    Color(0xFFE64A19),
                    Color(0xFF00796B),
                )
                
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 24.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    presets.forEach { presetColor ->
                        Surface(
                            modifier = Modifier
                                .size(32.dp)
                                .bounceClick {
                                    val hsv = FloatArray(3)
                                    android.graphics.Color.colorToHSV(presetColor.toArgb(), hsv)
                                    hue = hsv[0]
                                    saturation = hsv[1]
                                    value = hsv[2]
                                },
                            shape = CircleShape,
                            color = presetColor,
                            border = if (pickedColor == presetColor) BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface) else null
                        ) {}
                    }
                }

                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center,
                        modifier = Modifier.padding(horizontal = 20.dp, vertical = 12.dp)
                    ) {
                        Surface(
                            shape = CircleShape,
                            color = pickedColor,
                            border = BorderStroke(2.dp, MaterialTheme.colorScheme.onSurface.copy(alpha = 0.1f)),
                            modifier = Modifier.size(36.dp)
                        ) {}

                        Spacer(modifier = Modifier.width(16.dp))

                        BasicTextField(
                            value = hexText,
                            onValueChange = {
                                val formatted = if (it.startsWith("#")) it else "#$it"
                                if (formatted.length <= 7) {
                                    hexText = formatted.uppercase()
                                    formatted.toColor()?.let { color ->
                                        val hsv = FloatArray(3)
                                        android.graphics.Color.colorToHSV(color.toArgb(), hsv)
                                        hue = hsv[0]
                                        saturation = hsv[1]
                                        value = hsv[2]
                                    }
                                }
                            },
                            textStyle = MaterialTheme.typography.bodyLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp,
                                textAlign = TextAlign.Center
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.width(110.dp)
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth().padding(bottom = 28.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    ColorWheel(
                        hue = hue,
                        saturation = saturation,
                        modifier = Modifier.size(200.dp),
                        onColorChanged = { h, s ->
                            hue = h
                            saturation = s
                        }
                    )
                    
                    Spacer(modifier = Modifier.width(24.dp))
                    
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.height(200.dp)
                    ) {
                        Text(
                            "B",
                            style = MaterialTheme.typography.labelMedium,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        BrightnessSlider(
                            value = value,
                            hue = hue,
                            saturation = saturation,
                            onValueChange = { value = it },
                            modifier = Modifier.weight(1f).width(40.dp)
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                        .bounceClick {
                            onSelect(pickedColor.toArgb().toLong() and 0xFFFFFFFFL)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = MaterialTheme.colorScheme.primary,
                    contentColor = MaterialTheme.colorScheme.onPrimary
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Text(
                            text = stringResource(R.string.btn_apply),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun ColorWheel(
    hue: Float,
    saturation: Float,
    modifier: Modifier = Modifier,
    onColorChanged: (Float, Float) -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableFloatStateOf(0f) }
    val density = LocalDensity.current
    val selectorPaddingPx = remember(density) { with(density) { 12.dp.toPx() } }

    Canvas(
        modifier = modifier
            .aspectRatio(1f)
            .onSizeChanged { size ->
                val side = min(size.width, size.height).toFloat()
                radius = side / 2f
                center = Offset(side / 2f, side / 2f)
            }
            .pointerInput(center, radius) {
                if (radius <= 0) return@pointerInput
                val maxSelectorRadius = radius - selectorPaddingPx
                detectDragGestures { change, _ ->
                    val (h, s) = getHueSatAtPoint(change.position, center, maxSelectorRadius)
                    onColorChanged(h, s)
                    change.consume()
                }
            }
            .pointerInput(center, radius) {
                if (radius <= 0) return@pointerInput
                val maxSelectorRadius = radius - selectorPaddingPx
                detectTapGestures { position ->
                    val (h, s) = getHueSatAtPoint(position, center, maxSelectorRadius)
                    onColorChanged(h, s)
                }
            }
    ) {
        if (radius <= 0) return@Canvas

        val colors = listOf(
            Color.Red, Color.Yellow, Color.Green, Color.Cyan, Color.Blue, Color.Magenta, Color.Red
        )

        drawCircle(
            brush = Brush.sweepGradient(colors, center),
            radius = radius
        )
        drawCircle(
            brush = Brush.radialGradient(
                0.0f to Color.White,
                1.0f to Color.Transparent,
                center = center,
                radius = radius
            ),
            radius = radius
        )

        val angle = (hue.toDouble() * PI / 180.0)
        val maxSelectorRadius = radius - selectorPaddingPx
        val selectorRadius = saturation * maxSelectorRadius
        val x = center.x + cos(angle).toFloat() * selectorRadius
        val y = center.y + sin(angle).toFloat() * selectorRadius

        drawCircle(
            color = Color.White,
            radius = 10.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 3.dp.toPx())
        )
        drawCircle(
            color = Color.Black.copy(alpha = 0.2f),
            radius = 11.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 1.dp.toPx())
        )
    }
}

@Composable
fun BrightnessSlider(
    value: Float,
    hue: Float,
    saturation: Float,
    onValueChange: (Float) -> Unit,
    modifier: Modifier = Modifier
) {
    var width by remember { mutableFloatStateOf(0f) }
    var height by remember { mutableFloatStateOf(0f) }

    Canvas(
        modifier = modifier
            .fillMaxHeight()
            .onSizeChanged { size ->
                width = size.width.toFloat()
                height = size.height.toFloat()
            }
            .pointerInput(width, height) {
                if (width <= 0f || height <= 0f) return@pointerInput
                val radius = width / 2f
                val range = height - 2 * radius
                detectTapGestures { offset ->
                    if (range > 0f) {
                        val fraction = ((offset.y - radius) / range).coerceIn(0f, 1f)
                        onValueChange(1f - fraction)
                    }
                }
            }
            .pointerInput(width, height) {
                if (width <= 0f || height <= 0f) return@pointerInput
                val radius = width / 2f
                val range = height - 2 * radius
                detectDragGestures { change, _ ->
                    if (range > 0f) {
                        val fraction = ((change.position.y - radius) / range).coerceIn(0f, 1f)
                        onValueChange(1f - fraction)
                    }
                    change.consume()
                }
            }
    ) {
        if (width <= 0f || height <= 0f) return@Canvas
        val strokeWidth = size.width
        val cornerRadius = strokeWidth / 2

        val colorTop = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 1f)))
        val colorBottom = Color(android.graphics.Color.HSVToColor(floatArrayOf(hue, saturation, 0f)))
        
        drawRoundRect(
            brush = Brush.verticalGradient(listOf(colorTop, colorBottom)),
            size = size,
            cornerRadius = androidx.compose.ui.geometry.CornerRadius(cornerRadius, cornerRadius)
        )

        val radius = strokeWidth / 2f
        val range = height - 2 * radius
        val selectorY = if (range > 0f) radius + (1f - value) * range else radius
        drawCircle(
            color = Color.White,
            radius = radius - 2.dp.toPx(),
            center = Offset(strokeWidth / 2, selectorY),
            style = Stroke(width = 3.dp.toPx())
        )
    }
}

private fun getHueSatAtPoint(position: Offset, center: Offset, radius: Float): Pair<Float, Float> {
    val dx = position.x - center.x
    val dy = position.y - center.y
    val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)

    val angle = atan2(dy, dx)
    var hue = angle * 180f / PI.toFloat()
    if (hue < 0) hue += 360f

    val saturation = distance / radius
    return hue to saturation
}

private fun Color.toHexString(): String {
    val argb = this.toArgb()
    return String.format("#%06X", 0xFFFFFF and argb)
}

private fun String.toColor(): Color? {
    return try {
        Color((if (startsWith("#")) this else "#$this").toColorInt())
    } catch (_: Exception) {
        null
    }
}
