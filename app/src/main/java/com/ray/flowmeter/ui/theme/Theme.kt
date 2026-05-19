package com.ray.flowmeter.ui.theme

import android.app.Activity
import android.os.Build
import androidx.compose.animation.*
import androidx.compose.animation.core.SpringSpec
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.composed
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import android.graphics.Color as AndroidColor

// Light theme colors
val PrimaryLight = Color(0xFF0056D2)
val OnPrimaryLight = Color(0xFFFFFFFF)
val PrimaryContainerLight = Color(0xFFD8E2FF)
val OnPrimaryContainerLight = Color(0xFF001A41)

val SecondaryLight = Color(0xFF535E78)
val OnSecondaryLight = Color(0xFFFFFFFF)
val SecondaryContainerLight = Color(0xFFD8E2FF)
val OnSecondaryContainerLight = Color(0xFF0F1B32)

val TertiaryLight = Color(0xFF006495)
val OnTertiaryLight = Color(0xFFFFFFFF)
val TertiaryContainerLight = Color(0xFFCBE6FF)
val OnTertiaryContainerLight = Color(0xFF001E30)

val ErrorLight = Color(0xFFBA1A1A)
val OnErrorLight = Color(0xFFFFFFFF)
val ErrorContainerLight = Color(0xFFFFDAD6)
val OnErrorContainerLight = Color(0xFF410002)

val BackgroundLight = Color(0xFFFDFBFF)
val OnBackgroundLight = Color(0xFF1A1B1E)
val SurfaceLight = Color(0xFFFDFBFF)
val OnSurfaceLight = Color(0xFF1A1B1E)
val SurfaceVariantLight = Color(0xFFE1E2EC)
val OnSurfaceVariantLight = Color(0xFF44474F)
val OutlineLight = Color(0xFF74777F)

// Dark theme colors
val PrimaryDark = Color(0xFFAFC6FF)
val OnPrimaryDark = Color(0xFF002D6D)
val PrimaryContainerDark = Color(0xFF00419E)
val OnPrimaryContainerDark = Color(0xFFD8E2FF)

val SecondaryDark = Color(0xFFBBC7E4)
val OnSecondaryDark = Color(0xFF253048)
val SecondaryContainerDark = Color(0xFF3B475F)
val OnSecondaryContainerDark = Color(0xFFD8E2FF)

val TertiaryDark = Color(0xFF8FCDFF)
val OnTertiaryDark = Color(0xFF00344E)
val TertiaryContainerDark = Color(0xFF004B6F)
val OnTertiaryContainerDark = Color(0xFFCBE6FF)

val ErrorDark = Color(0xFFFFB4AB)
val OnErrorDark = Color(0xFF690005)
val ErrorContainerDark = Color(0xFF93000A)
val OnErrorContainerDark = Color(0xFFFFDAD6)

val BackgroundDark = Color(0xFF1A1B1E)
val OnBackgroundDark = Color(0xFFE3E2E6)
val SurfaceDark = Color(0xFF1A1B1E)
val OnSurfaceDark = Color(0xFFE3E2E6)
val SurfaceVariantDark = Color(0xFF44474F)
val OnSurfaceVariantDark = Color(0xFFC4C6D0)
val OutlineDark = Color(0xFF8E9099)

val AppTypography = Typography(
    displayLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Black,
        fontSize = 64.sp,
        lineHeight = 72.sp,
        letterSpacing = (-0.5).sp
    ),
    displayMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 52.sp,
        lineHeight = 60.sp,
        letterSpacing = (-0.25).sp
    ),
    displaySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 40.sp,
        lineHeight = 48.sp,
        letterSpacing = 0.sp
    ),
    headlineLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 34.sp,
        lineHeight = 42.sp,
        letterSpacing = 0.sp,
    ),
    headlineMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 30.sp,
        lineHeight = 38.sp,
        letterSpacing = 0.sp
    ),
    headlineSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 26.sp,
        lineHeight = 34.sp,
        letterSpacing = 0.sp
    ),
    titleLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 24.sp,
        lineHeight = 32.sp,
        letterSpacing = 0.sp
    ),
    titleMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 18.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.15.sp
    ),
    titleSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.SemiBold,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.1.sp
    ),
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 17.sp,
        lineHeight = 26.sp,
        letterSpacing = 0.25.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.25.sp
    ),
    bodySmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.4.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.ExtraBold,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        letterSpacing = 0.5.sp
    ),
    labelMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    ),
    labelSmall = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Bold,
        fontSize = 12.sp,
        lineHeight = 18.sp,
        letterSpacing = 0.5.sp
    )
)

object ThemeMode {
    const val SYSTEM = "System"
    const val DARK = "Dark"
    const val LIGHT = "Light"
}

private val DarkColorScheme = darkColorScheme(
    primary = PrimaryDark,
    onPrimary = OnPrimaryDark,
    primaryContainer = PrimaryContainerDark,
    onPrimaryContainer = OnPrimaryContainerDark,
    secondary = SecondaryDark,
    onSecondary = OnSecondaryDark,
    secondaryContainer = SecondaryContainerDark,
    onSecondaryContainer = OnSecondaryContainerDark,
    tertiary = TertiaryDark,
    onTertiary = OnTertiaryDark,
    tertiaryContainer = TertiaryContainerDark,
    onTertiaryContainer = OnTertiaryContainerDark,
    error = ErrorDark,
    onError = OnErrorDark,
    errorContainer = ErrorContainerDark,
    onErrorContainer = OnErrorContainerDark,
    background = BackgroundDark,
    onBackground = OnBackgroundDark,
    surface = SurfaceDark,
    onSurface = OnSurfaceDark,
    surfaceVariant = SurfaceVariantDark,
    onSurfaceVariant = OnSurfaceVariantDark,
    outline = OutlineDark,
    surfaceContainerLowest = Color(0xFF0C0D0F),
    surfaceContainerLow = Color(0xFF1A1B1E),
    surfaceContainer = Color(0xFF1E1F22),
    surfaceContainerHigh = Color(0xFF252629),
    surfaceContainerHighest = Color(0xFF2F3033),
    surfaceDim = Color(0xFF111114),
    surfaceBright = Color(0xFF38393C),
)

private val LightColorScheme = lightColorScheme(
    primary = PrimaryLight,
    onPrimary = OnPrimaryLight,
    primaryContainer = PrimaryContainerLight,
    onPrimaryContainer = OnPrimaryContainerLight,
    secondary = SecondaryLight,
    onSecondary = OnSecondaryLight,
    secondaryContainer = SecondaryContainerLight,
    onSecondaryContainer = OnSecondaryContainerLight,
    tertiary = TertiaryLight,
    onTertiary = OnTertiaryLight,
    tertiaryContainer = TertiaryContainerLight,
    onTertiaryContainer = OnTertiaryContainerLight,
    error = ErrorLight,
    onError = OnErrorLight,
    errorContainer = ErrorContainerLight,
    onErrorContainer = OnErrorContainerLight,
    background = BackgroundLight,
    onBackground = OnBackgroundLight,
    surface = SurfaceLight,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceVariantLight,
    onSurfaceVariant = OnSurfaceVariantLight,
    outline = OutlineLight,
    surfaceContainerLowest = Color(0xFFFFFFFF),
    surfaceContainerLow = Color(0xFFF7F6FA),
    surfaceContainer = Color(0xFFF3F3F7),
    surfaceContainerHigh = Color(0xFFEDEDF2),
    surfaceContainerHighest = Color(0xFFE7E7EC),
    surfaceDim = Color(0xFFD9D9DD),
    surfaceBright = Color(0xFFFDFBFF)
)

@Composable
fun FlowMeterTheme(
    themeMode: String = ThemeMode.SYSTEM,
    useMaterialYou: Boolean = true,
    useAmoled: Boolean = false,
    accentColor: Long? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    val systemInDark = isSystemInDarkTheme()

    val isDark = when (themeMode) {
        ThemeMode.DARK -> true
        ThemeMode.LIGHT -> false
        else -> systemInDark
    }

    var colorScheme = when {
        useMaterialYou && (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) -> {
            if (isDark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        isDark -> DarkColorScheme
        else -> LightColorScheme
    }

    if (!useMaterialYou && (accentColor != null)) {
        colorScheme = deriveCustomColorScheme(Color(accentColor), isDark)
    }

    if (isDark && useAmoled) {
        colorScheme = colorScheme.toAmoled()
    }

    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            window?.let {
                WindowCompat.getInsetsController(it, view).apply {
                    isAppearanceLightStatusBars = !isDark
                    isAppearanceLightNavigationBars = !isDark
                }
            }
        }
    }

    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        content = content
    )
}

private fun ColorScheme.toAmoled(): ColorScheme {
    return copy(
        background = Color.Black,
        surface = Color.Black,
        surfaceVariant = Color(0xFF0A0A0A),
        surfaceContainer = Color(0xFF050505),
        surfaceContainerLow = Color.Black,
        surfaceContainerHigh = Color(0xFF0F0F0F),
        surfaceContainerHighest = Color(0xFF141414),
        surfaceDim = Color.Black,
        surfaceBright = Color(0xFF1A1B1E)
    )
}

private fun deriveCustomColorScheme(accent: Color, isDark: Boolean): ColorScheme {
    val hsv = FloatArray(3)
    AndroidColor.colorToHSV(accent.toArgb(), hsv)
    val hue = hsv[0]
    val sat = hsv[1]

    val secondary = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat * 0.4f, if (isDark) 0.8f else 0.4f)))
    val tertiary = Color(AndroidColor.HSVToColor(floatArrayOf((hue + 340) % 360, sat * 0.6f, if (isDark) 0.8f else 0.5f)))

    val base = if (isDark) DarkColorScheme else LightColorScheme

    return if (isDark) {
        base.copy(
            primary = accent,
            onPrimary = Color.Black,
            primaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat * 0.8f, 0.3f))),
            onPrimaryContainer = Color.White,
            secondary = secondary,
            onSecondary = Color.Black,
            secondaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat * 0.3f, 0.2f))),
            onSecondaryContainer = Color.White,
            tertiary = tertiary,
            onTertiary = Color.Black,
            tertiaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf((hue + 340) % 360, sat * 0.4f, 0.2f))),
            onTertiaryContainer = Color.White,
        )
    } else {
        base.copy(
            primary = accent,
            onPrimary = Color.White,
            primaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat * 0.2f, 0.95f))),
            onPrimaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat, 0.3f))),
            secondary = secondary,
            onSecondary = Color.White,
            secondaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat * 0.1f, 0.97f))),
            onSecondaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf(hue, sat * 0.6f, 0.2f))),
            tertiary = tertiary,
            onTertiary = Color.White,
            tertiaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf((hue + 340) % 360, sat * 0.15f, 0.95f))),
            onTertiaryContainer = Color(AndroidColor.HSVToColor(floatArrayOf((hue + 340) % 360, sat, 0.3f))),
        )
    }
}

fun <T> premiumSpring(): SpringSpec<T> = spring(
    dampingRatio = 0.7f,
    stiffness = 350f,
)

object AppTransitions {
    val ScreenEnter: EnterTransition = fadeIn(premiumSpring())
    val ScreenExit: ExitTransition = fadeOut(premiumSpring())

    val ScreenPopEnter: EnterTransition = fadeIn(premiumSpring())
    val ScreenPopExit: ExitTransition = fadeOut(premiumSpring())
}

fun Modifier.bounceClick(
    enabled: Boolean = true,
    scaleDown: Float = 0.94f,
    interactionSource: MutableInteractionSource? = null,
    onClick: (() -> Unit)? = null,
): Modifier = if (enabled) composed {
    val actualInteractionSource = interactionSource ?: remember { MutableInteractionSource() }
    val isPressed by actualInteractionSource.collectIsPressedAsState()

    val scale by animateFloatAsState(
        targetValue = if (isPressed) scaleDown else 1f,
        animationSpec = premiumSpring(),
        label = "bounceScale"
    )

    this.then(
        Modifier
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .clickable(
                interactionSource = actualInteractionSource,
                indication = null,
            ) { onClick?.invoke() }
    )
} else this

@Composable
fun StaggeredEntrance(
    content: @Composable () -> Unit,
) {
    content()
}