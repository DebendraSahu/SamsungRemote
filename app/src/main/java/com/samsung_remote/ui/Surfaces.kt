package com.samsung_remote.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Canvas
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.ImageShader
import androidx.compose.ui.graphics.Paint
import androidx.compose.ui.graphics.ShaderBrush
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.TileMode
import androidx.compose.ui.unit.dp
import kotlin.random.Random

// How the remote's plastic catches light. There are three surfaces in the whole app -- the shell
// itself, a key standing proud of it, and a recess cut into it -- so they live here as modifiers
// rather than being spelled out as a clip/background/border trio at every call site.
//
// One light, from above and slightly in front. Everything follows from that: convex things are
// bright at the top and shaded at the foot, concave things are dark under their top lip, and a
// pressed key swaps one for the other.

/** The shell's own gradient, lit from the top edge. */
internal fun bodyGradient(): Brush =
    Brush.verticalGradient(listOf(BodyTop, BodyMid, BodyBottom))

/**
 * The shell: its gradient, the fall-off towards the edges where the body curves away from the
 * light, and a fine grain so the plastic reads as a material rather than a flat fill.
 */
internal fun Modifier.shell(): Modifier = this
    .background(bodyGradient())
    .drawWithCache {
        val vignette = Brush.radialGradient(
            0.5f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.5f),
            center = Offset(size.width / 2f, size.height * 0.4f),
            radius = size.maxDimension * 0.72f,
        )
        // The grain is one small tile, repeated. A pixel of texture is a device pixel, so
        // the tile is built at this screen's density rather than once for all.
        val grain = ShaderBrush(ImageShader(grainTile(density), TileMode.Repeated, TileMode.Repeated))
        onDrawBehind {
            drawRect(vignette)
            drawRect(grain)
        }
    }

/**
 * A moulded key standing proud of the shell.
 *
 * Unpressed, its face is a shallow dome: a highlight where the light lands near the top, shade
 * gathering at the foot, a bright rim along the top edge and a dark one along the bottom.
 * [pressed] sinks it: the face darkens, the lip above throws a shadow across it, and the rim
 * flips, which is what reads as the key going in rather than merely changing colour.
 */
internal fun Modifier.moulded(
    shape: Shape,
    base: Color = KeyPlastic,
    pressed: Boolean = false,
): Modifier = this
    .clip(shape)
    .background(keyFace(base, pressed))
    .drawBehind {
        if (pressed) {
            drawRect(
                Brush.verticalGradient(
                    0f to Color.Black.copy(alpha = 0.45f),
                    0.6f to Color.Transparent,
                )
            )
        } else {
            drawRect(
                Brush.radialGradient(
                    0f to Color.White.copy(alpha = 0.17f),
                    1f to Color.Transparent,
                    center = Offset(size.width / 2f, size.height * 0.05f),
                    radius = size.maxDimension * 0.8f,
                )
            )
            drawRect(
                Brush.verticalGradient(
                    0.55f to Color.Transparent,
                    1f to Color.Black.copy(alpha = 0.32f),
                )
            )
        }
    }
    .border(1.dp, keyRim(pressed), shape)

/**
 * A recess cut into the shell: the lip overhangs, so shadow pools under the top edge, and the
 * far wall at the bottom catches a sliver of light.
 */
internal fun Modifier.recessed(shape: Shape): Modifier = this
    .clip(shape)
    .background(wellFace())
    .drawBehind {
        drawRect(
            Brush.verticalGradient(
                0f to Color.Black.copy(alpha = 0.6f),
                0.4f to Color.Transparent,
            )
        )
        drawRect(
            Brush.verticalGradient(
                0.8f to Color.Transparent,
                1f to Color.White.copy(alpha = 0.06f),
            )
        )
    }
    .border(1.dp, wellRim(), shape)

private fun keyFace(base: Color, pressed: Boolean): Brush =
    if (pressed) {
        Brush.verticalGradient(listOf(base.darken(0.34f), base.darken(0.08f)))
    } else {
        Brush.verticalGradient(listOf(base.lighten(0.20f), base.darken(0.30f)))
    }

/** Bright along a key's top edge, dark along the bottom -- the strongest cue that it stands proud. */
private fun keyRim(pressed: Boolean): Brush = Brush.verticalGradient(
    if (pressed) {
        listOf(Color.Black.copy(alpha = 0.6f), Color.White.copy(alpha = 0.06f))
    } else {
        listOf(Color.White.copy(alpha = 0.24f), Color.Black.copy(alpha = 0.4f))
    }
)

private fun wellFace(): Brush =
    Brush.verticalGradient(listOf(Well.darken(0.4f), Well.lighten(0.05f)))

private fun wellRim(): Brush = Brush.verticalGradient(
    listOf(Color.Black.copy(alpha = 0.7f), Color.White.copy(alpha = 0.08f))
)

/**
 * A tile of plastic grain: scattered specks a device pixel or so across, half a touch lighter
 * than the shell and half a touch darker, faint enough to be felt rather than seen.
 */
private fun grainTile(density: Float): ImageBitmap {
    val side = (160 * density).toInt().coerceAtLeast(64)
    // Just over a device pixel: any larger and it reads as noise rather than grain.
    val speck = (density * 0.55f).coerceAtLeast(1f)
    val bitmap = ImageBitmap(side, side)
    val canvas = Canvas(bitmap)
    val light = Paint().apply { color = Color.White.copy(alpha = 0.035f) }
    val dark = Paint().apply { color = Color.Black.copy(alpha = 0.06f) }
    val random = Random(7)
    repeat((side * side / (speck * speck) / 6f).toInt()) {
        val x = random.nextFloat() * side
        val y = random.nextFloat() * side
        canvas.drawRect(Rect(x, y, x + speck, y + speck), if (random.nextBoolean()) light else dark)
    }
    return bitmap
}
