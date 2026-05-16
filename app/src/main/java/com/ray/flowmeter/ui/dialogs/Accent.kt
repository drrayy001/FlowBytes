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
    var pickedColor by remember { mutableStateOf(currentColor?.let { Color(it) } ?: Color(0xFF0056D2)) }
    var hexText by remember { mutableStateOf(pickedColor.toHexString()) }

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
                    modifier = Modifier.fillMaxWidth().padding(bottom = 24.dp),
                    textAlign = TextAlign.Center
                )

                // Centered Color Preview and Hex above the Color Wheel
                Surface(
                    color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(24.dp),
                    modifier = Modifier.padding(bottom = 28.dp)
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
                                    formatted.toColor()?.let { color -> pickedColor = color }
                                }
                            },
                            textStyle = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface,
                                letterSpacing = 1.sp
                            ),
                            singleLine = true,
                            keyboardOptions = KeyboardOptions(capitalization = KeyboardCapitalization.Characters),
                            modifier = Modifier.width(100.dp)
                        )
                    }
                }

                // Adjusted size for better visual proportion
                ColorWheel(
                    selectedColor = pickedColor,
                    modifier = Modifier
                        .size(220.dp)
                        .padding(bottom = 28.dp),
                    onColorChanged = {
                        pickedColor = it
                        hexText = it.toHexString()
                    }
                )

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
                            text = stringResource(R.string.btn_apply_globally),
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Surface(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .bounceClick {
                            onSelect(null)
                            onDismiss()
                        },
                    shape = RoundedCornerShape(16.dp),
                    color = Color.Transparent,
                    contentColor = MaterialTheme.colorScheme.primary
                ) {
                    Text(
                        text = stringResource(R.string.btn_reset_default),
                        modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold
                    )
                }
            }
        }
    }
}

@Composable
fun ColorWheel(
    selectedColor: Color,
    modifier: Modifier = Modifier,
    onColorChanged: (Color) -> Unit
) {
    var center by remember { mutableStateOf(Offset.Zero) }
    var radius by remember { mutableFloatStateOf(0f) }

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
                detectDragGestures { change, _ ->
                    val color = getColorAtPoint(change.position, center, radius)
                    onColorChanged(color)
                    change.consume()
                }
            }
            .pointerInput(center, radius) {
                if (radius <= 0) return@pointerInput
                detectTapGestures { position ->
                    val color = getColorAtPoint(position, center, radius)
                    onColorChanged(color)
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

        // Selector
        val hsv = FloatArray(3)
        android.graphics.Color.colorToHSV(selectedColor.toArgb(), hsv)
        val hue = hsv[0]
        val saturation = hsv[1]

        val angle = (hue.toDouble() * PI / 180.0)
        val selectorRadius = saturation * radius
        val x = center.x + cos(angle).toFloat() * selectorRadius
        val y = center.y + sin(angle).toFloat() * selectorRadius

        drawCircle(
            color = Color.White,
            radius = 12.dp.toPx(),
            center = Offset(x, y),
            style = Stroke(width = 4.dp.toPx())
        )
    }
}

private fun getColorAtPoint(position: Offset, center: Offset, radius: Float): Color {
    val dx = position.x - center.x
    val dy = position.y - center.y
    val distance = sqrt(dx * dx + dy * dy).coerceAtMost(radius)

    val angle = atan2(dy, dx)
    var hue = angle * 180f / PI.toFloat()
    if (hue < 0) hue += 360f

    val saturation = distance / radius

    val hsv = floatArrayOf(hue, saturation, 1f)
    return Color(android.graphics.Color.HSVToColor(hsv))
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
