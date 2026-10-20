package com.health.tracker.ui.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

internal object HealthPalette {
    val Mint = Color(0xFF1F7A55)
    val MintDeep = Color(0xFF12513A)
    val MintBright = Color(0xFF3AA87A)
    val MintSoft = Color(0xFFD9F3E5)
    val Amber = Color(0xFFB96427)
    val Ink = Color(0xFF15251E)
    val InkMuted = Color(0xFF5B6E65)
    val Canvas = Color(0xFFF1F7F3)
}

private val Scheme = lightColorScheme(
    primary = HealthPalette.Mint,
    onPrimary = Color.White,
    primaryContainer = HealthPalette.MintSoft,
    onPrimaryContainer = HealthPalette.MintDeep,
    secondary = HealthPalette.Amber,
    onSecondary = Color.White,
    background = HealthPalette.Canvas,
    onBackground = HealthPalette.Ink,
    surface = Color.White,
    onSurface = HealthPalette.Ink,
    surfaceVariant = Color(0xFFE6F0E9),
    onSurfaceVariant = HealthPalette.InkMuted,
    outline = Color(0xFFB9CCC0),
    outlineVariant = Color(0xFFD9E6DD),
    error = Color(0xFFB3261E),
    errorContainer = Color(0xFFFCE3E0),
    onErrorContainer = Color(0xFF5C1310),
)

private val Shapes = Shapes(
    extraSmall = RoundedCornerShape(8.dp),
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(22.dp),
    extraLarge = RoundedCornerShape(28.dp),
)

private val Typography = Typography(
    displaySmall = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 38.sp,
        lineHeight = 42.sp,
        letterSpacing = (-0.8).sp,
    ),
    headlineMedium = TextStyle(
        fontWeight = FontWeight.Bold,
        fontSize = 25.sp,
        lineHeight = 31.sp,
    ),
    titleMedium = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 17.sp,
        lineHeight = 22.sp,
    ),
    bodyLarge = TextStyle(fontSize = 16.sp, lineHeight = 22.sp),
    bodyMedium = TextStyle(fontSize = 14.sp, lineHeight = 19.sp),
    bodySmall = TextStyle(fontSize = 12.5.sp, lineHeight = 17.sp),
    labelLarge = TextStyle(
        fontWeight = FontWeight.SemiBold,
        fontSize = 14.sp,
        lineHeight = 18.sp,
    ),
)

@Composable
fun HealthTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = Scheme,
        typography = Typography,
        shapes = Shapes,
        content = content,
    )
}
