package com.samsung_remote.ui

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung_remote.tv.Keys

@Composable
internal fun NavRow(press: (String) -> Unit) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(14.dp, Alignment.CenterHorizontally),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Key(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            desc = "Back",
            onClick = { press(Keys.RETURN) },
        ) { BackGlyph(Label, 19.dp) }
        Key(
            modifier = Modifier.size(58.dp),
            base = KeyHero,
            shape = CircleShape,
            elevation = 7.dp,
            desc = "Home",
            onClick = { press(Keys.HOME) },
        ) { HomeGlyph(Label, 21.dp) }
        Key(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            desc = "Menu",
            onClick = { press(Keys.MENU) },
        ) { MenuGlyph(Label, 18.dp) }
        Key(
            modifier = Modifier.size(52.dp),
            shape = CircleShape,
            desc = "Source",
            onClick = { press(Keys.SOURCE) },
        ) { SourceGlyph(Label, 19.dp) }
    }
}

@Composable
internal fun MediaRow(press: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(50.dp),
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        Key(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            desc = "Rewind",
            repeatable = true,
            onClick = { press(Keys.REWIND) },
        ) { SkipGlyph(Label, 20.dp, forward = false) }
        Key(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            base = KeyHero,
            elevation = 7.dp,
            desc = "Play or pause",
            onClick = { press(Keys.PLAY_PAUSE) },
        ) { PlayPauseGlyph(Label, 19.dp) }
        Key(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            desc = "Fast forward",
            repeatable = true,
            onClick = { press(Keys.FAST_FORWARD) },
        ) { SkipGlyph(Label, 20.dp, forward = true) }
    }
}

@Composable
internal fun RockerRow(press: (String) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(124.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Rocker("VOL", Modifier.weight(1f), { press(Keys.VOL_UP) }, { press(Keys.VOL_DOWN) })
        Key(
            modifier = Modifier.size(58.dp),
            shape = CircleShape,
            desc = "Mute",
            onClick = { press(Keys.MUTE) },
        ) { MuteGlyph(Label, 21.dp) }
        Rocker("CH", Modifier.weight(1f), { press(Keys.CH_UP) }, { press(Keys.CH_DOWN) })
    }
}

/**
 * A real remote's rocker: one moulded key with a plus end, a minus end and a label between.
 * Pressing an end tips the whole body about its middle rather than squashing it, because the
 * rocker is the one control on a remote that pivots instead of sinking.
 */
@Composable
internal fun Rocker(
    label: String,
    modifier: Modifier = Modifier,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    val shape = RoundedCornerShape(32.dp)
    var tipped by remember { mutableStateOf(0) }
    val tilt by animateFloatAsState(
        targetValue = tipped * 9f,
        animationSpec = spring(dampingRatio = 0.6f, stiffness = Spring.StiffnessMediumLow),
        label = "tilt",
    )
    val lift by animateDpAsState(
        targetValue = if (tipped == 0) 7.dp else 2.dp,
        animationSpec = spring(stiffness = Spring.StiffnessHigh),
        label = "rockerLift",
    )

    Column(
        modifier = modifier
            .fillMaxHeight()
            .graphicsLayer {
                rotationX = tilt
                cameraDistance = 9f * density
            }
            .shadow(lift, shape, clip = false, spotColor = Color.Black)
            .moulded(shape),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        RockerEnd(
            modifier = Modifier.weight(1f),
            desc = "$label up",
            shaded = tipped == 1,
            onPressedChange = { tipped = if (it) 1 else 0 },
            onFire = onUp,
        ) {
            Text(
                text = "+",
                color = Label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                style = LocalTextStyle.current.copy(shadow = LabelShadow),
            )
        }
        // The groove moulded across the middle, where the two ends meet.
        Canvas(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
        ) {
            drawLine(
                brush = Brush.horizontalGradient(
                    listOf(Color.Transparent, Color.Black.copy(alpha = 0.5f), Color.Transparent)
                ),
                start = Offset(0f, 0f),
                end = Offset(size.width, 0f),
                strokeWidth = 1f,
            )
        }
        Text(
            text = label,
            color = Muted,
            fontSize = 10.sp,
            fontWeight = FontWeight.Medium,
            letterSpacing = 1.sp,
            style = LocalTextStyle.current.copy(shadow = LabelShadow),
        )
        RockerEnd(
            modifier = Modifier.weight(1f),
            desc = "$label down",
            shaded = tipped == -1,
            onPressedChange = { tipped = if (it) -1 else 0 },
            onFire = onDown,
        ) {
            Text(
                text = "−",
                color = Label,
                fontSize = 24.sp,
                fontWeight = FontWeight.Medium,
                style = LocalTextStyle.current.copy(shadow = LabelShadow),
            )
        }
    }
}

/** One end of a rocker: no shape of its own, just the shadow that falls across the end going in. */
@Composable
internal fun RockerEnd(
    modifier: Modifier,
    desc: String,
    shaded: Boolean,
    onPressedChange: (Boolean) -> Unit,
    onFire: () -> Unit,
    content: @Composable BoxScope.() -> Unit,
) {
    Box(
        modifier = modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    if (shaded) {
                        listOf(Color.Black.copy(alpha = 0.40f), Color.Black.copy(alpha = 0.04f))
                    } else {
                        listOf(Color.Transparent, Color.Transparent)
                    }
                )
            )
            .semantics {
                role = Role.Button
                contentDescription = desc
            }
            .then(holdable(repeatable = true, onPressedChange = onPressedChange, onFire = onFire)),
        contentAlignment = Alignment.Center,
        content = content,
    )
}
