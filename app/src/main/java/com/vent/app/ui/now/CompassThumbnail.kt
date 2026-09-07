package com.vent.app.ui.now

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * Compact compass rose used in the hero card corner: a small ring with a
 * north marker and a needle pointing into the wind (from-direction).
 */
@Composable
fun CompassThumbnail(
    windDirectionDeg: Int,
    modifier: Modifier = Modifier,
    size: Dp = 56.dp,
    ringColor: Color,
    needleColor: Color,
) {
    Canvas(modifier = modifier.size(size)) {
        val stroke = Stroke(width = size.toPx() * 0.06f)
        // Outer ring
        drawCircle(
            color = ringColor.copy(alpha = 0.35f),
            radius = size.toPx() / 2 - stroke.width,
            style = stroke,
        )
        // North tick
        drawArc(
            color = ringColor.copy(alpha = 0.7f),
            startAngle = -96f,
            sweepAngle = 12f,
            useCenter = false,
            topLeft = Offset(size.toPx() * 0.08f, size.toPx() * 0.08f),
            size = Size(size.toPx() * 0.84f, size.toPx() * 0.84f),
            style = stroke,
        )
        // Needle pointing FROM the wind direction (weather vane convention).
        rotate(degrees = windDirectionDeg.toFloat()) {
            val center = Offset(size.toPx() / 2, size.toPx() / 2)
            val len = size.toPx() * 0.32f
            drawLine(
                color = needleColor,
                start = center - Offset(len * 0.2f, 0f),
                end = center + Offset(len, 0f),
                strokeWidth = size.toPx() * 0.07f,
                cap = StrokeCap.Round,
            )
        }
        // Hub
        drawCircle(
            color = needleColor,
            radius = size.toPx() * 0.05f,
            center = Offset(size.toPx() / 2, size.toPx() / 2),
        )
    }
}