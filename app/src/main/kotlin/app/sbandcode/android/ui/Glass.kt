package app.sbandcode.android.ui

import android.animation.ValueAnimator
import android.database.ContentObserver
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import kotlin.math.PI
import kotlin.math.sin

val LocalGlassMotion = staticCompositionLocalOf { true }
val GlassSheetShape = RoundedCornerShape(topStart = 32.dp, topEnd = 32.dp)
val GlassSheetColor = Color(0xF416191D)

@Composable
fun GlassSheetHandle() {
    Box(Modifier.padding(top = 13.dp, bottom = 10.dp).width(36.dp).height(4.dp)
        .background(Brush.verticalGradient(listOf(Color(0xFF909AA7), Color(0xFF444A52))), CircleShape))
}

@Composable
fun rememberSystemMotion(): State<Boolean> {
    val resolver = LocalContext.current.contentResolver
    val enabled = remember { mutableStateOf(ValueAnimator.areAnimatorsEnabled()) }
    DisposableEffect(resolver) {
        val observer = object : ContentObserver(Handler(Looper.getMainLooper())) {
            override fun onChange(selfChange: Boolean) {
                enabled.value = ValueAnimator.areAnimatorsEnabled()
            }
        }
        resolver.registerContentObserver(Settings.Global.getUriFor(Settings.Global.ANIMATOR_DURATION_SCALE), false, observer)
        enabled.value = ValueAnimator.areAnimatorsEnabled()
        onDispose { resolver.unregisterContentObserver(observer) }
    }
    return enabled
}

fun Modifier.blackGlass(shape: Shape, prominent: Boolean = false): Modifier {
    val top = if (prominent) Color(0xED282A2E) else Color(0xDD1C1E21)
    val bottom = if (prominent) Color(0xF21A1B1E) else Color(0xF0101113)
    return clip(shape)
        .background(Brush.verticalGradient(listOf(top, bottom)))
        .border(1.dp, Brush.verticalGradient(listOf(Color(0x48E0E8F3), Color(0x0FFFFFFF), Color(0x24B0BFCC))), shape)
}

/** A low-contrast static layer behind the app surfaces. */
@Composable
fun GlassBackdrop(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        drawRect(Color(0xFF070809))
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        drawRect(Brush.radialGradient(
            0f to Color(0x233F5265),
            1f to Color.Transparent,
            center = Offset(size.width * 0.72f, size.height * 0.06f),
            radius = size.width * 0.94f,
        ))
        drawRect(Brush.radialGradient(
            0f to Color(0x18343D48),
            1f to Color.Transparent,
            center = Offset(size.width * 0.15f, size.height),
            radius = size.width * 0.91f,
        ))
    }
}

@Composable
fun LiquidRibbons(modifier: Modifier = Modifier) {
    val phase = if (LocalGlassMotion.current) {
        val transition = rememberInfiniteTransition(label = "liquid reflection")
        transition.animateFloat(
            initialValue = 0f,
            targetValue = (2 * PI).toFloat(),
            animationSpec = infiniteRepeatable(tween(12_000, easing = LinearEasing), RepeatMode.Restart),
            label = "reflection phase",
        )
    } else remember { mutableStateOf(0f) }

    Canvas(modifier) {
        if (size.width <= 0f || size.height <= 0f) return@Canvas
        val w = size.width
        val h = size.height
        val t = phase.value
        drawRect(Brush.radialGradient(
            0f to Color(0x503E5165),
            1f to Color.Transparent,
            center = Offset(w * (0.79f + 0.07f * sin(t)), h * 0.29f),
            radius = w * 0.8f,
        ))
        drawRect(Brush.radialGradient(
            0f to Color(0x38798EA4),
            0.52f to Color(0x1855697B),
            1f to Color.Transparent,
            center = Offset(w * 0.82f, h * (0.56f + 0.09f * sin(t + 0.9f))),
            radius = w * 0.43f,
        ))
        val body = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.42f to Color(0x143F647D),
            0.78f to Color(0x3EACC2D7),
            1f to Color(0x14384455),
        )
        val edge = Brush.horizontalGradient(
            0f to Color.Transparent,
            0.52f to Color(0x1F9BB6CB),
            0.81f to Color(0x86CBD9E8),
            1f to Color(0x18A5BCCF),
        )
        val path = Path()
        for (layer in 0..2) {
            val level = h * (0.36f + layer * 0.18f)
            val sway = h * 0.10f * sin(t + layer * 0.85f)
            path.apply {
                reset()
                moveTo(-w * 0.13f, level + h * 0.34f)
                cubicTo(w * 0.31f, level + h * 0.52f, w * 0.44f, level - h * 0.54f + sway, w * 0.79f, level - h * 0.37f)
                cubicTo(w * 0.91f, level - h * 0.31f, w * 1.02f, level - h * 0.1f + sway, w * 1.11f, level - h * 0.23f)
            }
            drawPath(path, body, style = Stroke(width = h * (0.17f - layer * 0.04f)))
            drawPath(path, edge, style = Stroke(width = (1.4f - layer * 0.2f).dp.toPx()))
        }
        drawRect(Brush.horizontalGradient(listOf(Color(0xC2090A0C), Color(0x78090A0C), Color.Transparent)))
    }
}
