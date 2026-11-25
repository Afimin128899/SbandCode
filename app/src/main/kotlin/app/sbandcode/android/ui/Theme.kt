package app.sbandcode.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp

private val Graphite = darkColorScheme(
    primary = Color(0xFFB5C8FF), onPrimary = Color(0xFF152B58),
    primaryContainer = Color(0xFF293E68), onPrimaryContainer = Color(0xFFDFE7FF),
    secondary = Color(0xFFC0C9DD), onSecondary = Color(0xFF273143),
    secondaryContainer = Color(0xFF2B3446), onSecondaryContainer = Color(0xFFE3E9F6),
    tertiary = Color(0xFFACCFC4), onTertiary = Color(0xFF16382F),
    tertiaryContainer = Color(0xFF2D4F45), onTertiaryContainer = Color(0xFFC8EBDD),
    background = Color(0xFF14161C), onBackground = Color(0xFFF4F5F7),
    surface = Color(0xFF14161C), onSurface = Color(0xFFF4F5F7),
    surfaceVariant = Color(0xFF303541), onSurfaceVariant = Color(0xFFB8C0CF),
    surfaceContainerLowest = Color(0xFF0F1116), surfaceContainerLow = Color(0xFF191C23),
    surfaceContainer = Color(0xFF1D2028), surfaceContainerHigh = Color(0xFF252A34),
    surfaceContainerHighest = Color(0xFF303541),
    outline = Color(0xFF909AAA), outlineVariant = Color(0xFF424A59),
    inverseSurface = Color(0xFFE3E6ED), inverseOnSurface = Color(0xFF292D36),
    inversePrimary = Color(0xFF405D99), surfaceTint = Color(0xFFB5C8FF),
    error = Color(0xFFFFB4AB), onError = Color(0xFF690005),
    errorContainer = Color(0xFF532523), onErrorContainer = Color(0xFFFFDAD6),
)

private val SbandTypography = Typography().let {
    it.copy(
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.35).sp),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        titleMedium = it.titleMedium.copy(fontWeight = FontWeight.SemiBold),
        bodyLarge = it.bodyLarge.copy(lineHeight = 25.sp),
        bodyMedium = it.bodyMedium.copy(lineHeight = 21.sp),
        labelLarge = it.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    )
}

@Composable
fun SbandTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Graphite, typography = SbandTypography, content = content)
}
