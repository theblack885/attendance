package com.hazri.attendance.ui

import android.app.Activity
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.view.WindowCompat
import com.hazri.attendance.data.Status

private val LightColors = lightColorScheme(
    primary = Color(0xFF5B5BF0),
    onPrimary = Color.White,
    primaryContainer = Color(0xFFE4E4FF),
    onPrimaryContainer = Color(0xFF1B1B6B),
    secondary = Color(0xFF00B8A0),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFFD2F7F1),
    onSecondaryContainer = Color(0xFF00423A),
    tertiary = Color(0xFFFF7A59),
    onTertiary = Color.White,
    background = Color(0xFFF5F6FB),
    onBackground = Color(0xFF12141C),
    surface = Color.White,
    onSurface = Color(0xFF12141C),
    surfaceVariant = Color(0xFFEBEDF6),
    onSurfaceVariant = Color(0xFF5A6072),
    outline = Color(0xFFC9CDDC),
    outlineVariant = Color(0xFFE3E6F0),
    error = Color(0xFFEF4444)
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF9494FF),
    onPrimary = Color(0xFF14146B),
    primaryContainer = Color(0xFF2A2A78),
    onPrimaryContainer = Color(0xFFE4E4FF),
    secondary = Color(0xFF2DD4BF),
    onSecondary = Color(0xFF00382F),
    secondaryContainer = Color(0xFF0B3F38),
    onSecondaryContainer = Color(0xFFBFF5EC),
    tertiary = Color(0xFFFF9378),
    onTertiary = Color(0xFF4A1100),
    background = Color(0xFF0B0D14),
    onBackground = Color(0xFFEDEFF7),
    surface = Color(0xFF141824),
    onSurface = Color(0xFFEDEFF7),
    surfaceVariant = Color(0xFF1D2231),
    onSurfaceVariant = Color(0xFFA2A9BD),
    outline = Color(0xFF3A4157),
    outlineVariant = Color(0xFF242A3B),
    error = Color(0xFFFF6B6B)
)

private val AppShapes = Shapes(
    extraSmall = RoundedCornerShape(10.dp),
    small = RoundedCornerShape(14.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(28.dp),
    extraLarge = RoundedCornerShape(36.dp)
)

private val AppTypography = Typography(
    headlineLarge = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 34.sp, lineHeight = 40.sp, letterSpacing = (-0.8).sp),
    headlineMedium = TextStyle(fontWeight = FontWeight.ExtraBold, fontSize = 28.sp, lineHeight = 34.sp, letterSpacing = (-0.5).sp),
    headlineSmall = TextStyle(fontWeight = FontWeight.Bold, fontSize = 22.sp, lineHeight = 28.sp),
    titleLarge = TextStyle(fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 26.sp),
    titleMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 16.sp, lineHeight = 22.sp),
    titleSmall = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    bodyLarge = TextStyle(fontWeight = FontWeight.Normal, fontSize = 16.sp, lineHeight = 24.sp),
    bodyMedium = TextStyle(fontWeight = FontWeight.Normal, fontSize = 14.sp, lineHeight = 20.sp),
    bodySmall = TextStyle(fontWeight = FontWeight.Normal, fontSize = 12.sp, lineHeight = 16.sp),
    labelLarge = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 14.sp, lineHeight = 20.sp),
    labelMedium = TextStyle(fontWeight = FontWeight.SemiBold, fontSize = 12.sp, lineHeight = 16.sp),
    labelSmall = TextStyle(fontWeight = FontWeight.Medium, fontSize = 11.sp, lineHeight = 14.sp)
)

@Composable
fun HazriTheme(mode: String, content: @Composable () -> Unit) {
    val dark = when (mode) {
        "dark" -> true
        "light" -> false
        else -> isSystemInDarkTheme()
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                val c = WindowCompat.getInsetsController(window, view)
                c.isAppearanceLightStatusBars = !dark
                c.isAppearanceLightNavigationBars = !dark
            }
        }
    }
    MaterialTheme(
        colorScheme = if (dark) DarkColors else LightColors,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

fun statusColor(s: String): Color = when (s) {
    Status.PRESENT -> Color(0xFF22C55E)
    Status.LATE -> Color(0xFFF59E0B)
    Status.HALF_DAY -> Color(0xFFFB923C)
    Status.ABSENT -> Color(0xFFEF4444)
    Status.LEAVE -> Color(0xFF3B82F6)
    Status.HOLIDAY -> Color(0xFF8B5CF6)
    Status.WEEKOFF -> Color(0xFF94A3B8)
    Status.PENDING -> Color(0xFF64748B)
    else -> Color(0xFF94A3B8)
}

fun statusLabel(s: String): String = when (s) {
    Status.PRESENT -> "Present"
    Status.LATE -> "Late"
    Status.HALF_DAY -> "Half day"
    Status.ABSENT -> "Absent"
    Status.LEAVE -> "On leave"
    Status.HOLIDAY -> "Holiday"
    Status.WEEKOFF -> "Week off"
    Status.PENDING -> "Not in yet"
    Status.FUTURE -> "Upcoming"
    else -> "-"
}
