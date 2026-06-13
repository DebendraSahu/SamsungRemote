package com.samsung_remote.ui

import com.samsung_remote.tv.TvApp
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin

private enum class Tab(val label: String) { APPS("App keys"), LAYOUT("Layout") }

/**
 * The screen where the remote gets rearranged: which app keys sit at the front of the strip,
 * and the order the blocks of keys are stacked in.
 *
 * Reordering is done with a pair of arrows per row rather than by dragging. Dragging reads
 * better in a demo, but this list is the one place in the app where a slip costs the viewer
 * their arrangement, and arrows cannot be slipped.
 */
@Composable
fun CustomiseScreen(layout: RemoteLayout, onDone: () -> Unit) {
    val favourites by layout.favourites.collectAsState()
    val sections by layout.sections.collectAsState()
    var tab by remember { mutableStateOf(Tab.APPS) }
    var confirmingReset by remember { mutableStateOf(false) }

    Column(Modifier.fillMaxSize()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 4.dp),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text(
                text = "Customise",
                color = Label,
                fontSize = 20.sp,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.weight(1f),
            )
            TextButton(onClick = { confirmingReset = true }) {
                Text("Reset", color = Muted, fontSize = 14.sp)
            }
            TextButton(onClick = onDone) {
                Text("Done", color = Label, fontSize = 15.sp, fontWeight = FontWeight.SemiBold)
            }
        }

        TabStrip(tab) { tab = it }

        Column(
            modifier = Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(top = 12.dp, bottom = 16.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            when (tab) {
                Tab.APPS -> AppsTab(layout, favourites)
                Tab.LAYOUT -> LayoutTab(layout, sections)
            }
        }
    }

    if (confirmingReset) {
        ResetDialog(
            tab = tab,
            nothingToUndo = when (tab) {
                Tab.APPS -> favourites.isEmpty()
                Tab.LAYOUT -> sections == Section.entries.toList()
            },
            onConfirm = {
                when (tab) {
                    Tab.APPS -> layout.resetFavourites()
                    Tab.LAYOUT -> layout.resetSections()
                }
                confirmingReset = false
            },
            onDismiss = { confirmingReset = false },
        )
    }
}

/**
 * Resetting cannot be undone and a starred list is slow to rebuild, so this says in plain words
 * which half is going and which half is staying.
 */
@Composable
private fun ResetDialog(
    tab: Tab,
    nothingToUndo: Boolean,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit,
) {
    val apps = tab == Tab.APPS
    AlertDialog(
        onDismissRequest = onDismiss,
        shape = RoundedCornerShape(24.dp),
        containerColor = BodyMid,
        titleContentColor = Label,
        textContentColor = Muted,
        title = { Text(if (apps) "Reset app keys?" else "Reset layout?") },
        text = {
            Text(
                text = when {
                    nothingToUndo && apps ->
                        "No apps are starred, so the strip is already in its default order. " +
                            "Your key layout is untouched either way."
                    nothingToUndo ->
                        "The keys are already in their factory order. Your app keys are " +
                            "untouched either way."
                    apps ->
                        "Unstars every app. The strip goes back to leading with whatever your " +
                            "TV says it has. Your key layout is left alone."
                    else ->
                        "Puts the blocks of keys back in their factory order. Your starred " +
                            "apps are left alone."
                },
                fontSize = 14.sp,
            )
        },
        confirmButton = {
            TextButton(onClick = onConfirm, enabled = !nothingToUndo) {
                Text("Reset", color = if (nothingToUndo) Offline else PowerRed)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (nothingToUndo) "Close" else "Cancel", color = Label)
            }
        },
    )
}

@Composable
private fun TabStrip(selected: Tab, onSelect: (Tab) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .recessed(RoundedCornerShape(14.dp))
            .padding(4.dp),
        horizontalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Tab.entries.forEach { entry ->
            val active = entry == selected
            val tabShape = RoundedCornerShape(11.dp)
            Box(
                modifier = Modifier
                    .weight(1f)
                    .height(36.dp)
                    // The selected tab is a key resting in the strip; the others are just the
                    // recess it sits in, so only it gets a face.
                    .then(if (active) Modifier.moulded(tabShape) else Modifier.clip(tabShape))
                    .clickable { onSelect(entry) },
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = entry.label,
                    color = if (active) Label else Muted,
                    fontSize = 13.sp,
                    fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
                )
            }
        }
    }
}

@Composable
private fun AppsTab(layout: RemoteLayout, favourites: List<TvApp>) {
    GroupHeading(
        title = "On the remote first",
        detail = if (favourites.isEmpty()) {
            "Star an app below and it moves to the front of the strip."
        } else {
            "These fill the ${APP_ROWS} rows from the left, in this order."
        },
    )
    favourites.forEachIndexed { index, app ->
        AppRow(
            app = app,
            starred = true,
            canGoUp = index > 0,
            canGoDown = index < favourites.lastIndex,
            onUp = { layout.moveFavourite(app, -1) },
            onDown = { layout.moveFavourite(app, 1) },
            onStar = { layout.toggleFavourite(app) },
        )
    }

    val rest = TvApp.entries.filter { it !in favourites }
    if (rest.isNotEmpty()) {
        Spacer(Modifier.height(10.dp))
        GroupHeading(
            title = "Everything else",
            detail = "Still on the remote, further along the strip.",
        )
        rest.forEach { app ->
            AppRow(
                app = app,
                starred = false,
                canGoUp = false,
                canGoDown = false,
                onUp = {},
                onDown = {},
                onStar = { layout.toggleFavourite(app) },
            )
        }
    }
}

@Composable
private fun LayoutTab(layout: RemoteLayout, sections: List<Section>) {
    GroupHeading(
        title = "Order of the keys",
        detail = "Top to bottom, as they sit on the remote. The navigation pad takes " +
                "whatever height the others leave.",
    )
    sections.forEachIndexed { index, section ->
        ReorderRow(
            title = section.label,
            detail = section.detail,
            canGoUp = index > 0,
            canGoDown = index < sections.lastIndex,
            onUp = { layout.moveSection(section, -1) },
            onDown = { layout.moveSection(section, 1) },
        )
    }
}

@Composable
private fun GroupHeading(title: String, detail: String) {
    Column(Modifier.padding(top = 6.dp, bottom = 4.dp)) {
        Text(
            text = title.uppercase(),
            color = Muted,
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
        )
        Text(text = detail, color = Muted.copy(alpha = 0.75f), fontSize = 12.5.sp)
    }
}

@Composable
private fun AppRow(
    app: TvApp,
    starred: Boolean,
    canGoUp: Boolean,
    canGoDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
    onStar: () -> Unit,
) {
    RowShell {
        // The same key, at the same size it has on the remote, so the choice is made against
        // what it will actually look like.
        Box(
            modifier = Modifier
                .width(64.dp)
                .height(34.dp)
                .moulded(RoundedCornerShape(10.dp), app.tint),
            contentAlignment = Alignment.Center,
        ) {
            Text(
                text = app.label.uppercase(),
                color = Color.White,
                fontSize = 8.sp,
                fontWeight = FontWeight.Bold,
                maxLines = 1,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = 3.dp),
            )
        }
        Text(
            text = app.label,
            color = Label,
            fontSize = 15.sp,
            fontWeight = FontWeight.Medium,
            maxLines = 1,
            modifier = Modifier.weight(1f),
        )
        if (starred) {
            IconKey("Move ${app.label} up", canGoUp, onUp) { c, s -> ChevronGlyph(c, s, up = true) }
            IconKey("Move ${app.label} down", canGoDown, onDown) { c, s -> ChevronGlyph(c, s, up = false) }
        }
        IconKey(
            desc = if (starred) "Remove ${app.label} from the front" else "Move ${app.label} to the front",
            enabled = true,
            onClick = onStar,
        ) { _, s -> StarGlyph(if (starred) Pending else Offline, s, filled = starred) }
    }
}

@Composable
private fun ReorderRow(
    title: String,
    detail: String,
    canGoUp: Boolean,
    canGoDown: Boolean,
    onUp: () -> Unit,
    onDown: () -> Unit,
) {
    RowShell {
        Column(Modifier.weight(1f)) {
            Text(text = title, color = Label, fontSize = 15.sp, fontWeight = FontWeight.Medium)
            Text(text = detail, color = Muted, fontSize = 12.sp)
        }
        IconKey("Move $title up", canGoUp, onUp) { c, s -> ChevronGlyph(c, s, up = true) }
        IconKey("Move $title down", canGoDown, onDown) { c, s -> ChevronGlyph(c, s, up = false) }
    }
}

@Composable
private fun RowShell(content: @Composable RowScope.() -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(Color.White.copy(alpha = 0.03f))
            .padding(horizontal = 10.dp, vertical = 9.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp),
        content = content,
    )
}

/** A small round key, so the controls in here are made of the same plastic as the remote. */
@Composable
private fun IconKey(
    desc: String,
    enabled: Boolean,
    onClick: () -> Unit,
    glyph: @Composable (Color, Dp) -> Unit,
) {
    if (!enabled) {
        // Kept in the layout so rows never jump sideways as things move to the ends.
        Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
            glyph(Offline.copy(alpha = 0.45f), 15.dp)
        }
        return
    }
    Key(
        modifier = Modifier.size(38.dp),
        shape = CircleShape,
        elevation = 4.dp,
        desc = desc,
        onClick = onClick,
    ) { glyph(Label, 15.dp) }
}

@Composable
private fun ChevronGlyph(color: Color, glyphSize: Dp, up: Boolean) {
    Canvas(Modifier.size(glyphSize)) {
        val w = size.width
        val h = size.height
        val stroke = size.minDimension * 0.15f
        val path = Path().apply {
            if (up) {
                moveTo(w * 0.1f, h * 0.68f)
                lineTo(w / 2f, h * 0.3f)
                lineTo(w * 0.9f, h * 0.68f)
            } else {
                moveTo(w * 0.1f, h * 0.32f)
                lineTo(w / 2f, h * 0.7f)
                lineTo(w * 0.9f, h * 0.32f)
            }
        }
        drawPath(path, color, style = Stroke(width = stroke, cap = StrokeCap.Round))
    }
}

@Composable
private fun StarGlyph(color: Color, glyphSize: Dp, filled: Boolean) {
    Canvas(Modifier.size(glyphSize)) {
        val outer = size.minDimension / 2f
        val inner = outer * 0.44f
        val path = Path()
        repeat(10) { i ->
            val radius = if (i % 2 == 0) outer else inner
            val angle = -PI / 2 + i * PI / 5
            val x = center.x + radius * cos(angle).toFloat()
            val y = center.y + radius * sin(angle).toFloat()
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        path.close()
        if (filled) {
            drawPath(path, color)
        } else {
            drawPath(path, color, style = Stroke(width = size.minDimension * 0.11f))
        }
    }
}
