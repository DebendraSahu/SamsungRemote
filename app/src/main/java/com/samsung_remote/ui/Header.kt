package com.samsung_remote.ui

import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung_remote.tv.Status

@Composable
internal fun Header(
    name: String,
    ipKnown: Boolean,
    status: Status,
    onPower: () -> Unit,
    onConnect: () -> Unit,
    onDisconnect: () -> Unit,
    onFindOther: () -> Unit,
    onForget: () -> Unit,
    onCustomise: () -> Unit,
) {
    val (lamp, label) = when (status) {
        Status.CONNECTED -> Online to "Connected"
        Status.CONNECTING -> Pending to "Connecting"
        Status.WAITING_APPROVAL -> Pending to "Allow on your TV"
        Status.DISCONNECTED -> Offline to if (ipKnown) "Disconnected" else "No TV paired"
    }
    val live = status == Status.CONNECTED
    var menuOpen by remember { mutableStateOf(false) }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(54.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
    ) {
        StatusLamp(
            colour = lamp,
            pulsing = status == Status.CONNECTING || status == Status.WAITING_APPROVAL,
        )

        Column(Modifier.weight(1f)) {
            Text(
                text = name.ifBlank { "Samsung Remote" },
                color = Label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
            )
            Text(
                text = label,
                color = Muted,
                fontSize = 12.sp,
                maxLines = 1,
            )
        }

        Box(contentAlignment = Alignment.Center) {
            // A live TV makes the key glow faintly, the way a lit key bleeds through plastic.
            if (live) {
                Canvas(Modifier.size(72.dp)) {
                    drawCircle(
                        Brush.radialGradient(
                            listOf(PowerRed.copy(alpha = 0.30f), Color.Transparent),
                            center = center,
                            radius = size.minDimension / 2f,
                        )
                    )
                }
            }
            Key(
                modifier = Modifier.size(50.dp),
                base = PowerRed,
                shape = CircleShape,
                elevation = 8.dp,
                desc = "Power",
                onClick = onPower,
            ) { PowerGlyph(Color.White, 20.dp) }
        }

        Box {
            Key(
                modifier = Modifier.size(44.dp),
                shape = CircleShape,
                elevation = 5.dp,
                desc = "More options",
                onClick = { menuOpen = true },
            ) {
                MoreGlyph(Label, 18.dp)
            }
            DropdownMenu(
                expanded = menuOpen,
                onDismissRequest = { menuOpen = false },
                modifier = Modifier.background(BodyTop),
            ) {
                if (status == Status.CONNECTED) {
                    DropdownMenuItem(
                        text = { Text("Disconnect") },
                        onClick = { menuOpen = false; onDisconnect() },
                    )
                } else if (ipKnown) {
                    DropdownMenuItem(
                        text = { Text("Connect") },
                        onClick = { menuOpen = false; onConnect() },
                    )
                }
                DropdownMenuItem(
                    text = { Text("Customise remote") },
                    onClick = { menuOpen = false; onCustomise() },
                )
                DropdownMenuItem(
                    text = { Text(if (ipKnown) "Connect to another TV" else "Find my TV") },
                    onClick = { menuOpen = false; onFindOther() },
                )
                if (ipKnown) {
                    DropdownMenuItem(
                        text = { Text("Forget this TV") },
                        onClick = { menuOpen = false; onForget() },
                    )
                }
            }
        }
    }
}

/** A pinhole lamp behind the shell: a bright core and the halo it throws onto the plastic. */
@Composable
internal fun StatusLamp(colour: Color, pulsing: Boolean) {
    val transition = rememberInfiniteTransition(label = "lamp")
    val breath by transition.animateFloat(
        initialValue = 0.30f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(850), RepeatMode.Reverse),
        label = "breath",
    )
    val glow = if (pulsing) breath else 1f
    Canvas(Modifier.size(16.dp)) {
        drawCircle(
            brush = Brush.radialGradient(
                listOf(colour.copy(alpha = 0.40f * glow), Color.Transparent),
                center = center,
                radius = size.minDimension / 2f,
            )
        )
        drawCircle(color = colour.copy(alpha = glow), radius = size.minDimension * 0.20f)
    }
}
