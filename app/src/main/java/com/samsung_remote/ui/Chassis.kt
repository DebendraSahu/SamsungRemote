package com.samsung_remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** The slot an infra-red emitter sits behind on the real thing. Pure decoration, and it sells it. */
@Composable
internal fun IrWindow() {
    Box(
        Modifier
            .width(46.dp)
            .height(4.dp)
            .clip(RoundedCornerShape(2.dp))
            .background(
                Brush.verticalGradient(
                    listOf(Color.Black.copy(alpha = 0.85f), Color(0xFF2B2B33))
                )
            )
    )
}

/** The moulding line between two sections of the shell. */
@Composable
internal fun Seam() {
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(2.dp)
    ) {
        val mid = size.height / 2f
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f), Color.Transparent)
            ),
            start = Offset(0f, mid - 0.5f),
            end = Offset(size.width, mid - 0.5f),
            strokeWidth = 1f,
        )
        drawLine(
            brush = Brush.horizontalGradient(
                listOf(Color.Transparent, Color.White.copy(alpha = 0.08f), Color.Transparent)
            ),
            start = Offset(0f, mid + 0.5f),
            end = Offset(size.width, mid + 0.5f),
            strokeWidth = 1f,
        )
    }
}
