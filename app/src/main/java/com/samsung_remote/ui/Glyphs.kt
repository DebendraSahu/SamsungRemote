package com.samsung_remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * A symbol printed on a key. [draw] is run twice: once a hair lower in near-black, then in
 * [color] on top. That hairline of shade under the ink is what makes it sit on the plastic
 * instead of floating in front of it.
 */
@Composable
internal fun Glyph(glyphSize: Dp, color: Color, draw: DrawScope.(ink: Color) -> Unit) {
    Canvas(Modifier.size(glyphSize)) {
        translate(top = 0.8.dp.toPx()) { draw(Color.Black.copy(alpha = 0.55f)) }
        draw(color)
    }
}

@Composable
internal fun PowerGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val stroke = size.minDimension * 0.13f
        val inset = stroke / 2f
        drawArc(
            color = ink,
            startAngle = -62f,
            sweepAngle = 304f,
            useCenter = false,
            topLeft = Offset(inset, inset),
            size = Size(size.width - stroke, size.height - stroke),
            style = Stroke(width = stroke, cap = StrokeCap.Round),
        )
        drawLine(
            color = ink,
            start = Offset(size.width / 2f, 0f),
            end = Offset(size.width / 2f, size.height * 0.46f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}

@Composable
internal fun PlayPauseGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val barWidth = size.width * 0.13f
        val gap = size.width * 0.11f
        // Play triangle on the left.
        val path = Path().apply {
            moveTo(0f, 0f)
            lineTo(size.width * 0.42f, size.height / 2f)
            lineTo(0f, size.height)
            close()
        }
        drawPath(path, ink)
        // Pause bars on the right.
        drawRect(
            color = ink,
            topLeft = Offset(size.width * 0.42f + gap, 0f),
            size = Size(barWidth, size.height),
        )
        drawRect(
            color = ink,
            topLeft = Offset(size.width * 0.42f + gap + barWidth + gap * 0.7f, 0f),
            size = Size(barWidth, size.height),
        )
    }
}

/** The double wedge printed on a rewind or fast-forward key. */
@Composable
internal fun SkipGlyph(color: Color, glyphSize: Dp, forward: Boolean) {
    Glyph(glyphSize, color) { ink ->
        val w = size.width
        val h = size.height
        val half = w * 0.46f
        fun wedge(left: Float) = Path().apply {
            if (forward) {
                moveTo(left, 0f)
                lineTo(left + half, h / 2f)
                lineTo(left, h)
            } else {
                moveTo(left + half, 0f)
                lineTo(left, h / 2f)
                lineTo(left + half, h)
            }
            close()
        }
        drawPath(wedge(0f), ink)
        drawPath(wedge(w - half), ink)
    }
}

@Composable
internal fun BackGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val stroke = size.minDimension * 0.12f
        drawLine(
            color = ink,
            start = Offset(size.width * 0.94f, size.height / 2f),
            end = Offset(size.width * 0.22f, size.height / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val head = Path().apply {
            moveTo(size.width * 0.56f, size.height * 0.16f)
            lineTo(size.width * 0.14f, size.height / 2f)
            lineTo(size.width * 0.56f, size.height * 0.84f)
        }
        drawPath(head, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

/** Outlined, like the back and menu keys -- filled in, the roof and body read as one arrowhead. */
@Composable
internal fun HomeGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.12f
        val eaves = h * 0.44f
        val roof = Path().apply {
            moveTo(w * 0.02f, eaves)
            lineTo(w / 2f, h * 0.04f)
            lineTo(w * 0.98f, eaves)
        }
        drawPath(roof, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
        val body = Path().apply {
            moveTo(w * 0.17f, eaves)
            lineTo(w * 0.17f, h * 0.96f)
            lineTo(w * 0.83f, h * 0.96f)
            lineTo(w * 0.83f, eaves)
        }
        drawPath(body, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
internal fun MenuGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val stroke = size.minDimension * 0.14f
        listOf(0.16f, 0.5f, 0.84f).forEach { y ->
            drawLine(
                color = ink,
                start = Offset(0f, size.height * y),
                end = Offset(size.width, size.height * y),
                strokeWidth = stroke,
                cap = StrokeCap.Round,
            )
        }
    }
}

/** Three dots in a column: the overflow menu. */
@Composable
internal fun MoreGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val r = size.minDimension * 0.09f
        listOf(0.2f, 0.5f, 0.8f).forEach { y ->
            drawCircle(ink, radius = r, center = Offset(size.width / 2f, size.height * y))
        }
    }
}

/** A signal arriving at a screen -- the TV's input list, which is where a USB stick shows up. */
@Composable
internal fun SourceGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.11f
        drawRoundRect(
            color = ink,
            topLeft = Offset(stroke / 2f, h * 0.14f),
            size = Size(w - stroke, h * 0.72f),
            cornerRadius = CornerRadius(size.minDimension * 0.14f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = ink,
            start = Offset(w * 0.24f, h / 2f),
            end = Offset(w * 0.70f, h / 2f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        val head = Path().apply {
            moveTo(w * 0.52f, h * 0.30f)
            lineTo(w * 0.74f, h / 2f)
            lineTo(w * 0.52f, h * 0.70f)
        }
        drawPath(head, ink, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
internal fun MuteGlyph(color: Color, glyphSize: Dp) {
    Glyph(glyphSize, color) { ink ->
        val w = size.width
        val h = size.height
        val stroke = w * 0.11f
        val speaker = Path().apply {
            moveTo(0f, h * 0.33f)
            lineTo(w * 0.18f, h * 0.33f)
            lineTo(w * 0.42f, h * 0.10f)
            lineTo(w * 0.42f, h * 0.90f)
            lineTo(w * 0.18f, h * 0.67f)
            lineTo(0f, h * 0.67f)
            close()
        }
        drawPath(speaker, ink)
        // The cross that marks it silenced.
        drawLine(
            color = ink,
            start = Offset(w * 0.58f, h * 0.30f),
            end = Offset(w * 0.95f, h * 0.70f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
        drawLine(
            color = ink,
            start = Offset(w * 0.95f, h * 0.30f),
            end = Offset(w * 0.58f, h * 0.70f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
