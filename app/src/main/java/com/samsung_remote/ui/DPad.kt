package com.samsung_remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung_remote.tv.Keys

internal const val NONE = 0
internal const val UP = 1
internal const val RIGHT = 2
internal const val DOWN = 3
internal const val LEFT = 4

/**
 * The navigation wheel: one raised disc sunk into a recess, with OK standing proud of it. The
 * arrows are drawn rather than typed so they stay crisp at whatever size the screen leaves, and
 * the disc lights under whichever one is being held.
 */
@Composable
internal fun DPad(modifier: Modifier, press: (String) -> Unit) {
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center,
    ) {
        val side = (if (maxWidth < maxHeight) maxWidth else maxHeight).coerceAtMost(286.dp)
        val target = (side * 0.30f).coerceIn(44.dp, 78.dp)
        var held by remember { mutableStateOf(NONE) }

        Box(
            modifier = Modifier
                .size(side)
                .recessed(CircleShape)
                .padding(7.dp),
            contentAlignment = Alignment.Center,
        ) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .shadow(7.dp, CircleShape, clip = false, spotColor = Color.Black)
                    .moulded(CircleShape),
            ) {
                Canvas(Modifier.fillMaxSize()) {
                    if (held != NONE) drawHeldGlow(held)
                    drawArrow(UP, held == UP)
                    drawArrow(RIGHT, held == RIGHT)
                    drawArrow(DOWN, held == DOWN)
                    drawArrow(LEFT, held == LEFT)
                }

                Target(Alignment.TopCenter, target, UP, "Up", { held = it }) { press(Keys.UP) }
                Target(Alignment.CenterEnd, target, RIGHT, "Right", { held = it }) { press(Keys.RIGHT) }
                Target(Alignment.BottomCenter, target, DOWN, "Down", { held = it }) { press(Keys.DOWN) }
                Target(Alignment.CenterStart, target, LEFT, "Left", { held = it }) { press(Keys.LEFT) }

                // Last child, so it takes the touch wherever it overlaps an arrow's reach.
                Key(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .size(side * 0.36f),
                    base = KeyHero,
                    shape = CircleShape,
                    elevation = 8.dp,
                    desc = "OK",
                    onClick = { press(Keys.ENTER) },
                ) {
                    Text(
                        text = "OK",
                        color = Label,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp,
                        style = LocalTextStyle.current.copy(shadow = LabelShadow),
                    )
                }
            }
        }
    }
}

@Composable
internal fun BoxScope.Target(
    align: Alignment,
    size: Dp,
    id: Int,
    desc: String,
    setHeld: (Int) -> Unit,
    onFire: () -> Unit,
) {
    Box(
        modifier = Modifier
            .align(align)
            .size(size)
            .semantics {
                role = Role.Button
                contentDescription = desc
            }
            .then(
                holdable(
                    repeatable = true,
                    onPressedChange = { setHeld(if (it) id else NONE) },
                    onFire = onFire,
                )
            )
    )
}

/** Where an arrow sits on the disc: out near the rim, the way they are printed on a real wheel. */
internal fun DrawScope.arrowAt(dir: Int): Offset {
    val d = size.minDimension * 0.395f
    return when (dir) {
        UP -> Offset(center.x, center.y - d)
        RIGHT -> Offset(center.x + d, center.y)
        DOWN -> Offset(center.x, center.y + d)
        else -> Offset(center.x - d, center.y)
    }
}

internal fun DrawScope.drawHeldGlow(dir: Int) {
    val at = arrowAt(dir)
    val r = size.minDimension * 0.30f
    drawCircle(
        brush = Brush.radialGradient(
            listOf(Color.White.copy(alpha = 0.14f), Color.Transparent),
            center = at,
            radius = r,
        ),
        radius = r,
        center = at,
    )
}

internal fun DrawScope.drawArrow(dir: Int, held: Boolean) {
    val degrees = when (dir) {
        UP -> 0f
        RIGHT -> 90f
        DOWN -> 180f
        else -> 270f
    }
    // Every arrow is the top one, turned a quarter at a time.
    rotate(degrees, center) {
        val at = arrowAt(UP)
        val w = size.minDimension * 0.058f
        val h = size.minDimension * 0.052f
        val path = Path().apply {
            moveTo(at.x, at.y - h / 2f)
            lineTo(at.x - w, at.y + h / 2f)
            lineTo(at.x + w, at.y + h / 2f)
            close()
        }
        // Printed on the disc, so it casts the same hairline of shade as every other symbol.
        translate(top = 0.8.dp.toPx()) { drawPath(path, Color.Black.copy(alpha = 0.5f)) }
        drawPath(path, Label.copy(alpha = if (held) 1f else 0.8f))
    }
}
