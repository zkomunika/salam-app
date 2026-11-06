package app.pantauizin.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp

val LightColorScheme = lightColorScheme(
    primary = MaroonPrimary,
    onPrimary = MaroonOnPrimary,
    primaryContainer = MaroonPrimaryContainer,
    onPrimaryContainer = MaroonOnPrimaryContainer,
    background = WarmBackgroundLight,
    onBackground = WarmOnSurfaceLight,
    surface = WarmSurfaceLight,
    onSurface = WarmOnSurfaceLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = WarmOnSurfaceVariantLight,
    error = StatusColors.Merah
)

val DarkColorScheme = darkColorScheme(
    primary = MaroonPrimaryDark,
    onPrimary = MaroonOnPrimaryDark,
    primaryContainer = MaroonPrimaryContainerDark,
    onPrimaryContainer = MaroonOnPrimaryContainerDark,
    background = WarmBackgroundDark,
    onBackground = WarmOnSurfaceDark,
    surface = WarmSurfaceDark,
    onSurface = WarmOnSurfaceDark,
    surfaceVariant = WarmSurfaceVariantDark,
    onSurfaceVariant = WarmOnSurfaceVariantDark,
    error = StatusColors.Merah
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
        shapes = AppShapes,
        content = content
    )
}

