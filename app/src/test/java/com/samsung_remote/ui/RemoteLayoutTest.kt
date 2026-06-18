package com.samsung_remote.ui

import com.samsung_remote.tv.TvApp
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class RemoteLayoutTest {

    @Test
    fun `shifted moves an item down and up`() {
        val list = listOf("a", "b", "c", "d")
        assertEquals(listOf("a", "c", "b", "d"), list.shifted("b", 1))
        assertEquals(listOf("a", "c", "b", "d"), list.shifted("c", -1))
        assertEquals(listOf("b", "c", "d", "a"), list.shifted("a", 3))
    }

    @Test
    fun `shifted refuses to move past either end`() {
        val list = listOf("a", "b", "c")
        assertNull(list.shifted("a", -1))
        assertNull(list.shifted("c", 1))
        assertNull(list.shifted("b", 5))
    }

    @Test
    fun `shifted returns null for an item that is not in the list`() {
        assertNull(listOf("a", "b").shifted("z", 1))
    }

    @Test
    fun `favourites lead the strip in the order they were arranged`() {
        val favourites = listOf(TvApp.SPOTIFY, TvApp.NETFLIX)
        val order = RemoteLayout.appOrder(favourites, emptySet())
        assertEquals(favourites, order.take(2))
    }

    @Test
    fun `apps the TV confirmed come before the rest, each group in catalogue order`() {
        val available = setOf(TvApp.DISNEY, TvApp.YOUTUBE)
        val order = RemoteLayout.appOrder(emptyList(), available)
        // Catalogue order within the confirmed group: YouTube is declared before Disney+.
        assertEquals(listOf(TvApp.YOUTUBE, TvApp.DISNEY), order.take(2))
        // The rest keep catalogue order too.
        val rest = TvApp.entries.filter { it !in available }
        assertEquals(rest, order.drop(2))
    }

    @Test
    fun `a favourite is never listed twice`() {
        val order = RemoteLayout.appOrder(listOf(TvApp.NETFLIX), setOf(TvApp.NETFLIX))
        assertEquals(TvApp.entries.size, order.size)
        assertEquals(TvApp.entries.toSet(), order.toSet())
        assertEquals(1, order.count { it == TvApp.NETFLIX })
    }
}
