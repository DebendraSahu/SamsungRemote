package com.samsung_remote.tv

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.net.Inet4Address
import java.net.InetAddress

class SubnetTest {

    private fun ip(dotted: String): Int = (InetAddress.getByName(dotted) as Inet4Address).toIpInt()

    @Test
    fun `a slash 24 yields the other 253 hosts`() {
        val hosts = subnetHosts(ip("192.168.1.37"), 24)
        assertEquals(253, hosts.size)
        assertTrue("192.168.1.1" in hosts)
        assertTrue("192.168.1.254" in hosts)
        assertFalse("network address must be skipped", "192.168.1.0" in hosts)
        assertFalse("broadcast address must be skipped", "192.168.1.255" in hosts)
        assertFalse("the phone itself must be skipped", "192.168.1.37" in hosts)
    }

    @Test
    fun `wider networks are clamped to the containing slash 24`() {
        val hosts = subnetHosts(ip("10.0.5.9"), 16)
        assertEquals(253, hosts.size)
        assertTrue(hosts.all { it.startsWith("10.0.5.") })
    }

    @Test
    fun `a slash 25 sweeps only its own half`() {
        val lower = subnetHosts(ip("192.168.1.10"), 25)
        assertEquals(125, lower.size)
        assertTrue("192.168.1.126" in lower)
        assertFalse("192.168.1.127" in lower)
        assertFalse("192.168.1.128" in lower)

        val upper = subnetHosts(ip("192.168.1.200"), 25)
        assertEquals(125, upper.size)
        assertTrue("192.168.1.129" in upper)
        assertTrue("192.168.1.254" in upper)
        assertFalse("192.168.1.128" in upper)
        assertFalse("192.168.1.255" in upper)
    }

    @Test
    fun `addresses above 127 in the first octet survive the signed int round trip`() {
        assertEquals("192.168.1.37", ip("192.168.1.37").toIpString())
        assertEquals("255.255.255.254", ip("255.255.255.254").toIpString())
        assertEquals("10.0.0.1", ip("10.0.0.1").toIpString())
    }

    @Test
    fun `xml entities in TV names are unescaped`() {
        assertEquals("50\" Smart TV", "50&quot; Smart TV".unescapeXml())
        assertEquals("Tom & Jerry's <TV>", "Tom &amp; Jerry&apos;s &lt;TV&gt;".unescapeXml())
        assertEquals("43\" UHD", "43&#34; UHD".unescapeXml())
    }

    @Test
    fun `ampersand is decoded last so double-escaped text is not decoded twice`() {
        assertEquals("&quot;", "&amp;quot;".unescapeXml())
    }
}
