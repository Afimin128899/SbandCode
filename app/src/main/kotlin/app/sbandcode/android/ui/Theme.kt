package app.sbandcode.android.ui

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

private val Graphite = darkColorScheme(
    primary = Color(0xFFE7E7E7), onPrimary = Color(0xFF191919),
    primaryContainer = Color(0xFF343434), onPrimaryContainer = Color(0xFFF4F4F4),
    secondary = Color(0xFFC8C8C8), onSecondary = Color(0xFF202020),
    secondaryContainer = Color(0xFF303030), onSecondaryContainer = Color(0xFFE9E9E9),
    tertiary = Color(0xFFB9B9B9), onTertiary = Color(0xFF212121),
    tertiaryContainer = Color(0xFF383838), onTertiaryContainer = Color(0xFFEAEAEA),
    background = Color(0xFF090A0B), onBackground = Color(0xFFF1F2F4),
    surface = Color(0xFF090A0B), onSurface = Color(0xFFF1F2F4),
    surfaceVariant = Color(0xFF2B2E32), onSurfaceVariant = Color(0xFFC2C5CA),
    surfaceContainerLowest = Color(0xFF070708), surfaceContainerLow = Color(0xFF111214),
    surfaceContainer = Color(0xFF181A1D), surfaceContainerHigh = Color(0xFF24262A),
    surfaceContainerHighest = Color(0xFF34383D),
    outline = Color(0xFF999EA5), outlineVariant = Color(0xFF3B3E43),
    inverseSurface = Color(0xFFE7E7E7), inverseOnSurface = Color(0xFF1A1A1A),
    inversePrimary = Color(0xFF525252), surfaceTint = Color(0xFFD0D0D0),
    error = Color(0xFFFFB4AB), onError = Color(0xFF360C08),
    errorContainer = Color(0xFF382226), onErrorContainer = Color(0xFFFFDAD6),
)

private val SbandTypography = Typography().let {
    it.copy(
        headlineSmall = it.headlineSmall.copy(fontWeight = FontWeight.SemiBold, letterSpacing = (-0.35).sp),
        titleLarge = it.titleLarge.copy(fontWeight = FontWeight.SemiBold),
        labelLarge = it.labelLarge.copy(fontWeight = FontWeight.Medium, letterSpacing = 0.1.sp),
    )
}

@Composable
fun SbandTheme(content: @Composable () -> Unit) {
    MaterialTheme(colorScheme = Graphite, typography = SbandTypography, content = content)
}

private val panelFill = Brush.verticalGradient(listOf(Color(0xFF232529), Color(0xFF16171A)))
private val panelEdge = Brush.verticalGradient(listOf(Color(0x44ECEFF4), Color(0x1DECEFF4)))

fun Modifier.graphitePanel(shape: RoundedCornerShape = RoundedCornerShape(22.dp)): Modifier = clip(shape)
    .background(panelFill).border(1.dp, panelEdge, shape)
