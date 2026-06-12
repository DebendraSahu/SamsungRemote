package com.samsung_remote.ui

import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung_remote.tv.TvApp

/**
 * The strip of app keys, sunk into the shell like the printed shortcuts along the bottom of a
 * real remote -- except the catalogue runs to two dozen, so the tray slides sideways.
 *
 * Once the TV has said which apps it actually has, those keys come to the front; the rest stay
 * reachable behind them, greyed but still worth a try, since a TV that refuses to enumerate its
 * apps can still be holding one.
 */
@Composable
internal fun AppTray(
    order: List<TvApp>,
    available: Set<TvApp>,
    connected: Boolean,
    onOpen: (TvApp) -> Unit,
) {
    val shape = RoundedCornerShape(20.dp)
    // Both rows ride one scroll state, so they slide together and stay in column with each
    // other. Filling down each column before moving right means the keys the viewer put first
    // are the ones on screen before anything is dragged.
    val scroll = rememberScrollState()
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .recessed(shape)
            .padding(6.dp)
    ) {
        Row(
            modifier = Modifier.horizontalScroll(scroll),
            horizontalArrangement = Arrangement.spacedBy(7.dp),
        ) {
            order.chunked(APP_ROWS).forEach { column ->
                Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                    column.forEach { app ->
                        AppKey(
                            app = app,
                            // Dim only once the TV has actually answered. An empty set means
                            // the probe is still running, and greying the whole strip for
                            // those few seconds made every key look dead at exactly the
                            // moment the remote came alive.
                            available = !connected || available.isEmpty() || app in available,
                            onClick = { onOpen(app) },
                        )
                    }
                    // A short last column would otherwise pull the row above out of line.
                    repeat(APP_ROWS - column.size) { Spacer(Modifier.height(APP_KEY_HEIGHT)) }
                }
            }
        }
    }
}

internal val APP_KEY_HEIGHT = 44.dp

@Composable
internal fun AppKey(
    app: TvApp,
    available: Boolean,
    onClick: () -> Unit,
) {
    // An unconfirmed app keeps its shape but loses its colour, so no key looks dead.
    val base = if (available) app.tint else lerp(app.tint, KeyPlastic, 0.82f)
    Key(
        modifier = Modifier
            .width(78.dp)
            .height(APP_KEY_HEIGHT),
        base = base,
        shape = RoundedCornerShape(12.dp),
        elevation = 5.dp,
        desc = app.label,
        onClick = onClick,
    ) {
        Text(
            text = app.label.uppercase(),
            color = if (available) Color.White else Muted,
            fontSize = 9.5.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.5.sp,
            maxLines = 1,
            textAlign = TextAlign.Center,
            style = LocalTextStyle.current.copy(shadow = LabelShadow),
            modifier = Modifier.padding(horizontal = 4.dp),
        )
    }
}
