package app.sbandcode.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.Shapes
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.ui.unit.dp
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Graphite = darkColorScheme(
    primary = Color(0xFFE7E7E7), onPrimary = Color(0xFF191919),
    primaryContainer = Color(0xFF343434), onPrimaryContainer = Color(0xFFF4F4F4),
    secondary = Color(0xFFC8C8C8), onSecondary = Color(0xFF1C1C1C),
    secondaryContainer = Color(0xFF303030), onSecondaryContainer = Color(0xFFE9E9E9),
    tertiary = Color(0xFFB9B9B9), onTertiary = Color(0xFF212121),
    tertiaryContainer = Color(0xFF383838), onTertiaryContainer = Color(0xFFEAEAEA),
    background = Color(0xFF0B0B0B), onBackground = Color(0xFFF1F1F1),
    surface = Color(0xFF0B0B0B), onSurface = Color(0xFFF1F1F1),
    surfaceVariant = Color(0xFF303030), onSurfaceVariant = Color(0xFFBDBDBD),
    surfaceContainerLowest = Color(0xFF080808), surfaceContainerLow = Color(0xFF151515),
    surfaceContainer = Color(0xFF1C1C1C), surfaceContainerHigh = Color(0xFF252525),
    surfaceContainerHighest = Color(0xFF333333),
    outline = Color(0xFF929292), outlineVariant = Color(0xFF363636),
    inverseSurface = Color(0xFFE7E7E7), inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF525252), surfaceTint = Color(0xFFD0D0D0),
    error = Color(0xFFF0F0F0), onError = Color(0xFF2B2B2B),
    errorContainer = Color(0xFF424242), onErrorContainer = Color(0xFFFAFAFA),
)

private val SbandTypography = Typography().let {
    it.copy(
        headlineMedium = it.headlineMedium.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.9).sp),
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.35).sp),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.35).sp),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.Medium),
        bodyLarge = it.bodyLarge.copy(lineHeight = 25.sp),
        labelLarge = it.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    )
}

private val SbandShapes = Shapes(
    small = RoundedCornerShape(12.dp),
    medium = RoundedCornerShape(16.dp),
    large = RoundedCornerShape(23.dp),
    extraLarge = RoundedCornerShape(30.dp),
)

@Composable
fun SbandTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Graphite, typography = SbandTypography, shapes = SbandShapes, content = content)
}
