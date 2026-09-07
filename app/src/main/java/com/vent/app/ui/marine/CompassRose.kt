package com.vent.app.ui.marine

import android.provider.Settings
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.vent.app.data.model.WindRose
import com.vent.app.haptic.HapticController
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.min
import kotlin.math.roundToInt
import kotlin.math.sin

/**
 * Large canvas compass rose. The needle points INTO the wind — i.e. toward the
 * direction the wind is coming FROM (weather-vane convention, north up), which
 * is what a sailor reads off the water: "the wind is out of the NE".
 */
@Composable
fun CompassRose(
    windDirectionDeg: Int,
    speedKnots: Double,
    gustKnots: Double,
    modifier: Modifier = Modifier,
    haptics: HapticController? = null,
) {
    val context = LocalContext.current
    val reduceMotion = remember {
        val scale = runCatching {
            Settings.Global.getFloat(context.contentResolver, Settings.Global.ANIMATOR_DURATION_SCALE, 1f)
        }.getOrDefault(1f)
        scale == 0f
    }
    val container = MaterialTheme.colorScheme.surfaceContainerHighest
    val primary = MaterialTheme.colorScheme.primary
    val onSurface = MaterialTheme.colorScheme.onSurface
    val outline = MaterialTheme.colorScheme.outline
    val needle = MaterialTheme.colorScheme.tertiary

    val needleDeg = remember { Animatable(windDirectionDeg.toFloat()) }

    LaunchedEffect(windDirectionDeg) {
        if (reduceMotion) {
            needleDeg.snapTo(windDirectionDeg.toFloat())
        } else {
            needleDeg.animateTo(
                targetValue = windDirectionDeg.toFloat(),
                animationSpec = spring(stiffness = Spring.StiffnessMediumLow, dampingRatio = Spring.DampingRatioMediumBouncy),
            )
        }
    }

    // Faint haptic tick as the needle crosses each 22.5° cardinal boundary.
    LaunchedEffect(Unit) {
        snapshotFlow { needleDeg.value }
            .map { (it / 22.5f).toInt() }
            .distinctUntilChanged()
            .collect { haptics?.cardinalTick() }
    }

    Box(modifier = modifier) {
        Surface(
            color = container,
            shape = MaterialTheme.shapes.extraLarge,
            modifier = Modifier.fillMaxSize(),
        ) {
            Canvas(Modifier.fillMaxSize().padding(12.dp)) {
                val center = Offset(size.width / 2f, size.height / 2f)
                val radius = min(size.width, size.height) / 2f - 12.dp.toPx()
                val tickIn = radius * 0.86f

                drawCircle(color = outline.copy(alpha = 0.5f), radius = radius, center = center, style = Stroke(width = 1.5.dp.toPx()))

                // 24 ticks; cardinal ticks longer and heavier.
                for (i in 0 until 24) {
                    val deg = i * 15
                    val cardinal = deg % 90 == 0
                    val outerR = if (cardinal) radius else radius * 0.93f
                    val rad = deg * (PI.toFloat() / 180f)
                    val outer = Offset(center.x + outerR * sin(rad), center.y - outerR * cos(rad))
                    val inner = Offset(center.x + tickIn * sin(rad), center.y - tickIn * cos(rad))
                    drawLine(
                        color = if (cardinal) onSurface else outline.copy(alpha = 0.7f),
                        start = outer,
                        end = inner,
                        strokeWidth = if (cardinal) 2.5.dp.toPx() else 1.dp.toPx(),
                        cap = StrokeCap.Round,
                    )
                }

                // Needle: tapered triangle pointing at the wind origin (from-direction).
                rotate(degrees = needleDeg.value, pivot = center) {
                    val needleLen = radius * 0.78f
                    val baseHalf = radius * 0.13f
                    val path = Path().apply {
                        moveTo(center.x, center.y - needleLen)
                        lineTo(center.x + baseHalf, center.y)
                        lineTo(center.x - baseHalf, center.y)
                        close()
                    }
                    drawPath(path = path, color = needle)
                }
                drawCircle(color = primary, radius = radius * 0.07f, center = center)
            }
        }
        cardinalLetter("N", Modifier.align(Alignment.TopCenter).padding(top = 6.dp), onSurface)
        cardinalLetter("E", Modifier.align(Alignment.CenterEnd).padding(end = 10.dp), onSurface)
        cardinalLetter("S", Modifier.align(Alignment.BottomCenter).padding(bottom = 6.dp), onSurface)
        cardinalLetter("W", Modifier.align(Alignment.CenterStart).padding(start = 10.dp), onSurface)
    }

    Text(
        text = "${WindRose.cardinal(windDirectionDeg)} · ${speedKnots.roundToInt()} kn, gusts ${gustKnots.roundToInt()} kn",
        style = MaterialTheme.typography.titleMedium,
        fontWeight = FontWeight.SemiBold,
    )
}

@Composable
private fun cardinalLetter(label: String, modifier: Modifier, color: Color) {
    Text(
        text = label,
        modifier = modifier,
        style = MaterialTheme.typography.labelLarge,
        fontWeight = FontWeight.Bold,
        color = color,
    )
}