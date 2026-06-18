package com.samsung_remote.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TvAppTest {

    @Test
    fun `matching is case-insensitive`() {
        assertTrue(TvApp.NETFLIX.matches("Netflix"))
        assertTrue(TvApp.NETFLIX.matches("NETFLIX"))
        assertTrue(TvApp.NETFLIX.matches("netflix"))
    }

    @Test
    fun `a keyword can sit inside a longer name`() {
        assertTrue(TvApp.PRIME.matches("Amazon Prime Video"))
        assertTrue(TvApp.MAX.matches("HBO Max"))
        assertTrue(TvApp.BROWSER.matches("Internet"))
        assertTrue(TvApp.TV_PLUS.matches("Samsung TV Plus"))
    }

    @Test
    fun `unrelated names do not match`() {
        assertFalse(TvApp.NETFLIX.matches("Spotify"))
        assertFalse(TvApp.SPOTIFY.matches("Apple Music"))
        assertFalse(TvApp.APPLE_TV.matches("Apple Music"))
    }

    @Test
    fun `every app has a label and at least one id to try`() {
        TvApp.entries.forEach { app ->
            assertTrue("${app.name} has no label", app.label.isNotBlank())
            assertTrue("${app.name} has no candidate ids", app.candidateIds.isNotEmpty())
            assertTrue("${app.name} has a blank id", app.candidateIds.all { it.isNotBlank() })
        }
    }

    @Test
    fun `no id is claimed by two different apps`() {
        val owners = mutableMapOf<String, TvApp>()
        TvApp.entries.forEach { app ->
            app.candidateIds.forEach { id ->
                val other = owners.put(id, app)
                assertEquals("id $id is listed under both $other and $app", null, other)
            }
        }
    }
}
