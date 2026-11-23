package app.sbandcode.android.ui

import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import app.sbandcode.android.data.Prefs

// SbandCode's blue palette.
private val Light = lightColorScheme(
    primary = Color(0xFF1467AA), onPrimary = Color.White,
    primaryContainer = Color(0xFFCCE8FF), onPrimaryContainer = Color(0xFF001D35),
    onBackground = Color(0xFF152F45), onSurface = Color(0xFF152F45),
    secondary = Color(0xFF405F78), secondaryContainer = Color(0xFFD4E8F9), onSecondaryContainer = Color(0xFF102C43),
    tertiary = Color(0xFF365DA0), tertiaryContainer = Color(0xFFD9E4FF),
    background = Color(0xFFEFF8FF), surface = Color(0xFFEFF8FF),
    surfaceVariant = Color(0xFFD6E8F6), onSurfaceVariant = Color(0xFF375269),
    surfaceContainerLowest = Color.White, surfaceContainerLow = Color(0xFFE5F3FF),
    surfaceContainer = Color(0xFFDEEFFF), surfaceContainerHigh = Color(0xFFD6EAFC),
    surfaceContainerHighest = Color(0xFFCFE5F8),
)
private val Dark = darkColorScheme(
    primary = Color(0xFF9AD2FF), onPrimary = Color(0xFF003553),
    primaryContainer = Color(0xFF004B75), onPrimaryContainer = Color(0xFFCCE8FF),
    onBackground = Color(0xFFE2F1FF), onSurface = Color(0xFFE2F1FF),
    secondary = Color(0xFFB1D1EB), secondaryContainer = Color(0xFF294963), onSecondaryContainer = Color(0xFFD4E8F9),
    tertiary = Color(0xFFB2C8FF), tertiaryContainer = Color(0xFF2C4678),
    background = Color(0xFF102538), surface = Color(0xFF102538),
    surfaceVariant = Color(0xFF375269), onSurfaceVariant = Color(0xFFC0D5E7),
    surfaceContainerLowest = Color(0xFF0B1C2B), surfaceContainerLow = Color(0xFF152E44),
    surfaceContainer = Color(0xFF1A354C), surfaceContainerHigh = Color(0xFF203E57),
    surfaceContainerHighest = Color(0xFF284962),
)

@Composable
fun MargyTheme(prefs: Prefs, content: @Composable () -> Unit) {
    val dark = when (prefs.theme) {
        "light" -> false
        "dark" -> true
        else -> isSystemInDarkTheme()
    }
    val context = LocalContext.current
    val colours = when {
        prefs.dynamic && Build.VERSION.SDK_INT >= 31 ->
            if (dark) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        dark -> Dark
        else -> Light
    }
    MaterialTheme(colorScheme = colours, content = content)
}
