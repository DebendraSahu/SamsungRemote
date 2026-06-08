package com.samsung_remote.ui

import androidx.compose.material3.darkColorScheme
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.lerp
import com.samsung_remote.tv.TvApp

// The remote is one piece of dark moulded plastic lit from above: every key derives its top
// highlight, its face gradient and its shadow from a single base colour, so the whole thing
// catches the same light. Colour appears only where a real remote prints it -- the red power
// key, the status lamp, and the branded app keys along the bottom.
internal val Ink = Color(0xFF000000)
internal val BodyTop = Color(0xFF26262D)
internal val BodyMid = Color(0xFF1C1C22)
internal val BodyBottom = Color(0xFF131318)
internal val Well = Color(0xFF131317)
internal val KeyPlastic = Color(0xFF26262D)
internal val KeyHero = Color(0xFF34343D)
internal val Label = Color(0xFFEDEDF1)
internal val Muted = Color(0xFF7C7C87)
internal val PowerRed = Color(0xFFC92F37)
internal val Online = Color(0xFF3ED16A)
internal val Pending = Color(0xFFE0A93A)
internal val Offline = Color(0xFF45454D)

internal val DarkColors = darkColorScheme(
    primary = Label,
    onPrimary = Ink,
    background = Ink,
    onBackground = Label,
    surface = BodyMid,
    onSurface = Label,
    surfaceVariant = KeyPlastic,
    onSurfaceVariant = Muted,
    error = PowerRed,
)

/**
 * The colour each app key is printed in. Taken from each service's own brand and knocked back,
 * because these are moulded keys catching the same dim light as the rest of the remote, not
 * backlit logos.
 */
internal val TvApp.tint: Color
    get() = when (this) {
        TvApp.NETFLIX -> Color(0xFFC4161C)
        TvApp.YOUTUBE -> Color(0xFFE2362F)
        TvApp.PRIME -> Color(0xFF0C7FAF)
        TvApp.HOTSTAR -> Color(0xFF2A2F6E)
        TvApp.SONYLIV -> Color(0xFF5B2A86)
        TvApp.DISNEY -> Color(0xFF14295C)
        TvApp.APPLE_TV -> Color(0xFF3A3A3F)
        TvApp.SPOTIFY -> Color(0xFF1A8F4C)
        TvApp.TWITCH -> Color(0xFF6441A5)
        TvApp.PLEX -> Color(0xFFB3800F)
        TvApp.MAX -> Color(0xFF1B2FB0)
        TvApp.HULU -> Color(0xFF17A85C)
        TvApp.PEACOCK -> Color(0xFF2D2D3A)
        TvApp.PARAMOUNT -> Color(0xFF0057B8)
        TvApp.DISCOVERY -> Color(0xFF1B5CAF)
        TvApp.IPLAYER -> Color(0xFFC23A74)
        TvApp.ITVX -> Color(0xFFBE9316)
        TvApp.TUBI -> Color(0xFF6A1BBF)
        TvApp.PLUTO -> Color(0xFF1A2060)
        TvApp.RAKUTEN -> Color(0xFFB0132A)
        TvApp.APPLE_MUSIC -> Color(0xFFBC2F45)
        TvApp.TIDAL -> Color(0xFF2B2B33)
        TvApp.DEEZER -> Color(0xFF8A2BC4)
        TvApp.YT_KIDS -> Color(0xFFD0392E)
        TvApp.STEAM_LINK -> Color(0xFF24394F)
        TvApp.TV_PLUS -> Color(0xFF1428A0)
        TvApp.BROWSER -> Color(0xFF3B3B45)
    }

/**
 * The hairline of shade under a printed label. Drawn glyphs get the same from [Glyph]; this is
 * for the few labels that are typed -- OK, the rocker ends, the app names.
 */
internal val LabelShadow = Shadow(
    color = Color.Black.copy(alpha = 0.6f),
    offset = Offset(0f, 2f),
    blurRadius = 2f,
)

internal fun Color.lighten(f: Float) = lerp(this, Color.White, f)
internal fun Color.darken(f: Float) = lerp(this, Color.Black, f)
