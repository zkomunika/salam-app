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
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.pantauizin.R

val DisplayFontFamily = FontFamily(
    Font(R.font.outfit_semibold, FontWeight.SemiBold),
    Font(R.font.outfit_bold, FontWeight.Bold),
    Font(R.font.outfit_extrabold, FontWeight.ExtraBold)
)

val BodyFontFamily = FontFamily(
    Font(R.font.plus_jakarta_sans_regular, FontWeight.Normal),
    Font(R.font.plus_jakarta_sans_medium, FontWeight.Medium),
    Font(R.font.plus_jakarta_sans_semibold, FontWeight.SemiBold),
    Font(R.font.plus_jakarta_sans_bold, FontWeight.Bold)
)

val LightColorScheme = lightColorScheme(
    primary = MaroonPrimary,
    onPrimary = MaroonOnPrimary,
    primaryContainer = MaroonPrimaryContainer,
    onPrimaryContainer = MaroonOnPrimaryContainer,
    secondary = MaroonPrimary,
    onSecondary = MaroonOnPrimary,
    secondaryContainer = BrandColors.SoftMaroonTintLight,
    onSecondaryContainer = MaroonPrimary,
    background = WarmBackgroundLight,
    onBackground = WarmOnSurfaceLight,
    surface = WarmSurfaceLight,
    onSurface = WarmOnSurfaceLight,
    surfaceVariant = WarmSurfaceVariantLight,
    onSurfaceVariant = WarmOnSurfaceVariantLight,
    outline = WarmOutlineLight,
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
    secondaryContainer = BrandColors.SoftMaroonTintDark,
    onSecondaryContainer = MaroonPrimaryContainer,
    background = WarmBackgroundDark,
    onBackground = WarmOnSurfaceDark,
    surface = WarmSurfaceDark,
    onSurface = WarmOnSurfaceDark,
    surfaceVariant = WarmSurfaceVariantDark,
    onSurfaceVariant = WarmOnSurfaceVariantDark,
    outline = WarmOutlineDark,
    outlineVariant = WarmOutlineVariantDark,
    error = StatusColors.Merah
)

val AppTypography = Typography(
    displaySmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontSize = 34.sp,
        lineHeight = 40.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = (-0.8).sp
    ),
    headlineSmall = TextStyle(
        fontFamily = DisplayFontFamily,
        fontSize = 22.sp,
        lineHeight = 28.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.4).sp
    ),
    titleLarge = TextStyle(
        fontFamily = DisplayFontFamily,
        fontSize = 20.sp,
        lineHeight = 26.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.3).sp
    ),
    titleMedium = TextStyle(
        fontFamily = DisplayFontFamily,
        fontSize = 16.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.2).sp
    ),
    titleSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = (-0.1).sp
    ),
    bodyLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 15.sp,
        lineHeight = 22.sp,
        fontWeight = FontWeight.Normal
    ),
    bodyMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 14.sp,
        lineHeight = 20.sp,
        fontWeight = FontWeight.Normal
    ),
    bodySmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 12.sp,
        lineHeight = 17.sp,
        fontWeight = FontWeight.Normal
    ),
    labelLarge = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 13.sp,
        lineHeight = 18.sp,
        fontWeight = FontWeight.SemiBold
    ),
    labelMedium = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 12.sp,
        lineHeight = 16.sp,
        fontWeight = FontWeight.Medium
    ),
    labelSmall = TextStyle(
        fontFamily = BodyFontFamily,
        fontSize = 11.sp,
        lineHeight = 14.sp,
        fontWeight = FontWeight.SemiBold,
        letterSpacing = 0.2.sp
    )
)

val AppShapes = Shapes(
    small = RoundedCornerShape(10.dp),
    medium = RoundedCornerShape(20.dp),
    large = RoundedCornerShape(24.dp)
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
