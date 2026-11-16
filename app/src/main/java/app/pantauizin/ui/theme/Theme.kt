package app.pantauizin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

val LightColorScheme = lightColorScheme(
    primary = MaroonPrimary,
    onPrimary = MaroonOnPrimary,
    primaryContainer = MaroonPrimaryContainer,
    onPrimaryContainer = MaroonOnPrimaryContainer,
    secondary = MaroonPrimary,
    onSecondary = MaroonOnPrimary,
    secondaryContainer = MaroonPrimaryContainer,
    onSecondaryContainer = MaroonOnPrimaryContainer,
    background = WarmBackgroundLight,
    onBackground = WarmOnSurfaceLight,
    surface = WarmSurfaceLight,
    onSurface = WarmOnSurfaceLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = WarmOnSurfaceVariantLight,
    outlineVariant = WarmOutlineVariantLight,
    error = StatusColors.Merah
)

val DarkColorScheme = darkColorScheme(
    primary = MaroonPrimaryDark,
    onPrimary = MaroonOnPrimaryDark,
    primaryContainer = MaroonPrimaryContainerDark,
    onPrimaryContainer = MaroonOnPrimaryContainerDark,
    secondary = MaroonPrimaryDark,
    onSecondary = MaroonOnPrimaryDark,
    secondaryContainer = MaroonPrimaryContainerDark,
    onSecondaryContainer = MaroonOnPrimaryContainerDark,
    background = WarmBackgroundDark,
    onBackground = WarmOnSurfaceDark,
    surface = WarmSurfaceDark,
    onSurface = WarmOnSurfaceDark,
    surfaceVariant = WarmSurfaceVariantDark,
    onSurfaceVariant = WarmOnSurfaceVariantDark,
    outlineVariant = WarmOutlineVariantDark,
    error = StatusColors.Merah
)

val AppTypography = Typography(
    titleLarge = TextStyle(
        fontSize = 20.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold
    ),
    titleMedium = TextStyle(
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold
    ),
    titleSmall = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.SemiBold
    ),
    bodyLarge = TextStyle(
        fontSize = 16.sp,
        lineHeight = 24.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    )
)

val AppShapes = Shapes(
    small = RoundedCornerShape(8.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(16.dp)
)

@Composable
fun PantauIzinTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme
    MaterialTheme(
        colorScheme = colorScheme,
        typography = AppTypography,
        shapes = AppShapes,
        content = content
    )
}

