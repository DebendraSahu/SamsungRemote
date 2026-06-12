package com.samsung_remote.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.samsung_remote.tv.TvDevice

@Composable
internal fun PickerDialog(
    scanning: Boolean,
    devices: List<TvDevice>,
    onSelect: (TvDevice) -> Unit,
    onRescan: () -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = BodyMid,
        titleContentColor = Label,
        textContentColor = Muted,
        title = { Text("Choose your TV", fontWeight = FontWeight.SemiBold) },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                if (devices.isEmpty()) {
                    Text(
                        text = if (scanning) {
                            "Looking for TVs on your Wi-Fi..."
                        } else {
                            "Nothing answered. Check the TV is switched on and on this " +
                                    "same Wi-Fi network."
                        },
                        color = Muted,
                        fontSize = 14.sp,
                    )
                } else {
                    // Named TVs first; unidentified hosts are a fallback, not the headline.
                    devices.sortedByDescending { it.identified }.forEach { device ->
                        DeviceRow(device) { onSelect(device) }
                    }
                }
                if (scanning) {
                    Row(
                        modifier = Modifier.padding(top = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        CircularProgressIndicator(modifier = Modifier.size(16.dp), color = Muted)
                        Text(text = "Scanning", fontSize = 13.sp, color = Muted)
                    }
                }
            }
        },
        confirmButton = { TextButton(onClick = onDismiss) { Text("Close", color = Label) } },
        dismissButton = {
            if (!scanning) TextButton(onClick = onRescan) { Text("Scan again", color = Muted) }
        },
    )
}

@Composable
internal fun DeviceRow(device: TvDevice, onClick: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 8.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        Box(
            modifier = Modifier
                .size(38.dp)
                .moulded(RoundedCornerShape(10.dp)),
            contentAlignment = Alignment.Center,
        ) {
            ScreenGlyph(if (device.identified) Label else Muted, 20.dp)
        }
        Column(Modifier.weight(1f)) {
            Text(
                text = device.name,
                color = Label,
                fontSize = 16.sp,
                fontWeight = FontWeight.SemiBold,
            )
            Text(
                text = if (device.identified) {
                    listOfNotNull(device.model, device.ip).joinToString(" · ")
                } else {
                    "Didn't identify itself — tap to try anyway"
                },
                color = Muted,
                fontSize = 13.sp,
            )
            if (device.identified && !device.supported) {
                Text(
                    text = "Pre-2016 model — may not accept this app",
                    color = PowerRed,
                    fontSize = 12.sp,
                )
            }
        }
    }
}

@Composable
internal fun ScreenGlyph(color: Color, glyphSize: Dp) {
    Canvas(Modifier.size(glyphSize)) {
        val stroke = size.minDimension * 0.09f
        drawRoundRect(
            color = color,
            topLeft = Offset(0f, size.height * 0.08f),
            size = Size(size.width, size.height * 0.66f),
            cornerRadius = CornerRadius(size.minDimension * 0.12f),
            style = Stroke(width = stroke),
        )
        drawLine(
            color = color,
            start = Offset(size.width * 0.3f, size.height * 0.94f),
            end = Offset(size.width * 0.7f, size.height * 0.94f),
            strokeWidth = stroke,
            cap = StrokeCap.Round,
        )
    }
}
