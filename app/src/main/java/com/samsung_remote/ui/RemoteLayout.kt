package com.samsung_remote.ui

import com.samsung_remote.tv.TvApp
import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * The blocks of keys the remote is stacked from, in their factory order.
 *
 * The status line and power key are deliberately not here: they are the remote's identity, not
 * one of its sections, and a remote whose power key wanders is a worse remote.
 */
enum class Section(val label: String, val detail: String) {
    DPAD("Navigation pad", "Arrows and OK"),
    NAV("Back, Home, Menu, Source", "The round keys"),
    MEDIA("Playback", "Rewind, play/pause, forward"),
    ROCKERS("Volume and channel", "Both rockers, with mute between them"),
    APPS("App keys", "Your streaming shortcuts"),
}

/** How many app keys fit across before the strip starts sliding. */
const val APP_ROWS = 2

/**
 * What the viewer has rearranged: which apps sit at the front of the app strip, and the order
 * the key blocks are stacked in.
 *
 * Both persist, because a remote whose keys move between launches is worse than one that never
 * let you move them at all. Stored as enum names rather than ordinals, so reordering the
 * catalogue in code can never silently reshuffle somebody's saved layout.
 */
class RemoteLayout(context: Context) {

    private val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private val _sections = MutableStateFlow(readSections())
    val sections: StateFlow<List<Section>> = _sections.asStateFlow()

    private val _favourites = MutableStateFlow(readFavourites())
    val favourites: StateFlow<List<TvApp>> = _favourites.asStateFlow()

    fun moveSection(section: Section, delta: Int) {
        val moved = _sections.value.shifted(section, delta) ?: return
        _sections.value = moved
        store(KEY_SECTIONS, moved.map(Section::name))
    }

    fun moveFavourite(app: TvApp, delta: Int) {
        val moved = _favourites.value.shifted(app, delta) ?: return
        _favourites.value = moved
        store(KEY_FAVOURITES, moved.map(TvApp::name))
    }

    /** Starring an app appends it; a new favourite goes behind the ones already chosen. */
    fun toggleFavourite(app: TvApp) {
        val current = _favourites.value
        val next = if (app in current) current - app else current + app
        _favourites.value = next
        store(KEY_FAVOURITES, next.map(TvApp::name))
    }

    /**
     * The two resets are deliberately separate. Restoring the order of the key blocks and
     * throwing away a carefully starred list of apps are unrelated regrets, and one reset that
     * did both would make the safe one dangerous.
     */
    fun resetSections() {
        _sections.value = Section.entries.toList()
        prefs.edit().remove(KEY_SECTIONS).apply()
    }

    fun resetFavourites() {
        _favourites.value = emptyList()
        prefs.edit().remove(KEY_FAVOURITES).apply()
    }

    private fun readSections(): List<Section> {
        val stored = prefs.getString(KEY_SECTIONS, null).orEmpty()
            .split(SEPARATOR)
            .mapNotNull { name -> Section.entries.firstOrNull { it.name == name } }
        // A section added to the enum later lands at the bottom rather than disappearing for
        // everybody who already has a saved order.
        return stored + Section.entries.filter { it !in stored }
    }

    private fun readFavourites(): List<TvApp> =
        prefs.getString(KEY_FAVOURITES, null).orEmpty()
            .split(SEPARATOR)
            .mapNotNull { name -> TvApp.entries.firstOrNull { it.name == name } }

    private fun store(key: String, names: List<String>) {
        prefs.edit().putString(key, names.joinToString(SEPARATOR)).apply()
    }

    companion object {
        private const val PREFS = "remote_layout"
        private const val KEY_SECTIONS = "sections"
        private const val KEY_FAVOURITES = "favourites"
        private const val SEPARATOR = ","

        /**
         * The app strip's running order: the viewer's favourites exactly as they arranged
         * them, then everything else with whatever the TV confirms it has brought forward, so
         * the keys most likely to be wanted are the ones reachable without sliding.
         */
        fun appOrder(favourites: List<TvApp>, available: Set<TvApp>): List<TvApp> =
            favourites + TvApp.entries
                .filter { it !in favourites }
                .sortedByDescending { it in available }
    }
}

/** The list with [item] moved [delta] places, or null if it cannot go that way. */
internal fun <T> List<T>.shifted(item: T, delta: Int): List<T>? {
    val from = indexOf(item)
    if (from < 0) return null
    val to = from + delta
    if (to !in indices) return null
    return toMutableList().apply { add(to, removeAt(from)) }
}
