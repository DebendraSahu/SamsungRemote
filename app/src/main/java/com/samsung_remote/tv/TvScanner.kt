package com.samsung_remote.tv

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request
import org.json.JSONObject
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import java.util.concurrent.TimeUnit

/** Something on the local network that answers on a Samsung TV port. */
data class TvDevice(
    val ip: String,
    /** The TV's friendly name, or the bare IP when the host wouldn't identify itself. */
    val name: String,
    val model: String?,
    /**
     * The TV's own stable id (`uuid:…`), which survives DHCP moving its address. Blank when
     * the host wouldn't identify itself. This, not the IP, is what identifies a TV.
     */
    val uuid: String,
    /** The host confirmed itself a Samsung TV via the :8001 info endpoint. */
    val identified: Boolean,
    /** 2016-and-newer sets advertise token auth; older ones can't speak this app's protocol. */
    val supported: Boolean,
)

/**
 * Finds candidate TVs by sweeping the phone's own subnet.
 *
 * Any host with :8001 or :8002 open is listed, because that pair is the Samsung remote-control
 * signature. Hosts that also answer the unauthenticated `:8001/api/v2/` info endpoint are shown
 * by name and model; the rest are still offered so an unusual set can be tried by hand rather
 * than being hidden by an over-strict filter.
 *
 * Nothing here needs a runtime permission: it is ordinary TCP, not a Wi-Fi scan.
 */
class TvScanner(context: Context) {

    private val appContext = context.applicationContext

    // Plain HTTP on :8001 — deliberately NOT the trust-all client SamsungTv uses for wss on :8002.
    // Cleartext to the LAN is allowed by res/xml/network_security_config.xml.
    private val http = OkHttpClient.Builder()
        .connectTimeout(HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .readTimeout(HTTP_TIMEOUT_MS, TimeUnit.MILLISECONDS)
        .build()

    /** Emits each candidate as it is found; completes when the whole subnet has been tried. */
    fun scan(): Flow<TvDevice> = channelFlow {
        val gate = Semaphore(MAX_PARALLEL)
        for (ip in subnetHosts()) {
            launch(Dispatchers.IO) {
                gate.withPermit {
                    if (PORTS.none { listening(ip, it) }) return@withPermit
                    send(identify(ip) ?: unidentified(ip))
                }
            }
        }
    }

    private fun listening(ip: String, port: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), CONNECT_TIMEOUT_MS)
            true
        }
    } catch (e: Exception) {
        false
    }

    /** Asks a single host to describe itself, for filling in the name of a hand-typed IP. */
    suspend fun lookup(ip: String): TvDevice? = withContext(Dispatchers.IO) { identify(ip) }

    private fun identify(ip: String): TvDevice? = try {
        val request = Request.Builder().url("http://$ip:$INFO_PORT/api/v2/").build()
        http.newCall(request).execute().use { response ->
            if (response.isSuccessful) response.body?.string()?.let { describe(ip, it) } else null
        }
    } catch (e: Exception) {
        null
    }

    private fun describe(ip: String, body: String): TvDevice? {
        val root = try {
            JSONObject(body)
        } catch (e: Exception) {
            return null
        }
        val device = root.optJSONObject("device")
        val kind = root.optString("type").ifBlank { device?.optString("type").orEmpty() }
        if (!kind.contains("Samsung", ignoreCase = true)) return null

        val advertised = root.optString("name").ifBlank { device?.optString("name").orEmpty() }
        val id = listOfNotNull(device?.optString("duid"), device?.optString("id"), root.optString("id"))
            .firstOrNull { it.isNotBlank() }
            .orEmpty()
        return TvDevice(
            ip = ip,
            // Sets announce themselves as e.g. "[TV] Living Room" or "50&quot; Smart TV".
            name = advertised.unescapeXml().removePrefix("[TV] ").trim().ifBlank { "Samsung TV" },
            model = device?.optString("modelName")?.takeIf { it.isNotBlank() },
            uuid = id,
            identified = true,
            supported = device?.optString("TokenAuthSupport").equals("true", ignoreCase = true),
        )
    }

    private fun unidentified(ip: String) = TvDevice(
        ip = ip,
        name = ip,
        model = null,
        uuid = "",
        identified = false,
        supported = false,
    )

    /**
     * Finds the TV with this stable id, wherever DHCP has put it.
     *
     * Addresses move between sessions; the TV's own uuid does not. Looking it up again is
     * what lets a reconnect keep using the saved access token instead of asking the viewer
     * to approve the app all over again.
     */
    suspend fun findByUuid(uuid: String): TvDevice? {
        if (uuid.isBlank()) return null
        return scan().firstOrNull { it.identified && it.uuid == uuid }
    }

    /** Every address on the phone's subnet except its own. */
    private fun subnetHosts(): List<String> {
        val (address, prefixLength) = localAddress() ?: return emptyList()
        return subnetHosts(address.toIpInt(), prefixLength)
    }

    /**
     * The Wi-Fi address and prefix length. Preferring Wi-Fi over [ConnectivityManager.activeNetwork]
     * matters on a phone with mobile data up: the cellular link is a /27 around 192.0.0.2, and
     * sweeping that instead of the real LAN finds nothing.
     */
    private fun localAddress(): Pair<Inet4Address, Int>? {
        wifiAddress()?.let { return it }

        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
        val active = cm?.activeNetwork?.let { cm.getLinkProperties(it) }
        active?.linkAddresses?.forEach { link ->
            val addr = link.address
            if (addr is Inet4Address && !addr.isLoopbackAddress) return addr to link.prefixLength
        }
        return null
    }

    private fun wifiAddress(): Pair<Inet4Address, Int>? = try {
        NetworkInterface.getNetworkInterfaces()?.toList()
            ?.filter { it.isUp && !it.isLoopback && it.name.startsWith("wlan") }
            ?.firstNotNullOfOrNull { nif ->
                nif.interfaceAddresses.firstNotNullOfOrNull { ia ->
                    val addr = ia.address
                    if (addr is Inet4Address && addr.isSiteLocalAddress) {
                        addr to ia.networkPrefixLength.toInt()
                    } else {
                        null
                    }
                }
            }
    } catch (e: Exception) {
        null
    }

    private companion object {
        const val INFO_PORT = 8001
        val PORTS = intArrayOf(8001, 8002)
        const val MAX_PARALLEL = 48
        const val CONNECT_TIMEOUT_MS = 400
        const val HTTP_TIMEOUT_MS = 2000L
    }
}

// The pure arithmetic behind the sweep, kept free of Android types so it can be unit-tested.

/**
 * Every host address in the subnet around [self] (an IPv4 address packed into an int), as
 * dotted strings, minus the network address, the broadcast address and [self] itself.
 */
internal fun subnetHosts(self: Int, prefixLength: Int): List<String> {
    // A /16 would be 65k probes, so never sweep wider than the /24 we sit in.
    val prefix = maxOf(prefixLength, 24)
    val mask = -1 shl (32 - prefix)
    val network = self and mask
    val broadcast = network or mask.inv()
    return ((network + 1) until broadcast)
        .filter { it != self }
        .map { it.toIpString() }
}

internal fun Inet4Address.toIpInt(): Int =
    address.fold(0) { acc, byte -> (acc shl 8) or (byte.toInt() and 0xFF) }

internal fun Int.toIpString(): String =
    "${(this ushr 24) and 0xFF}.${(this ushr 16) and 0xFF}.${(this ushr 8) and 0xFF}.${this and 0xFF}"

/** The info endpoint XML-escapes its strings, so "43&quot; UHD" arrives with the entity. */
internal fun String.unescapeXml(): String = this
    .replace("&quot;", "\"")
    .replace("&#34;", "\"")
    .replace("&apos;", "'")
    .replace("&#39;", "'")
    .replace("&lt;", "<")
    .replace("&gt;", ">")
    .replace("&amp;", "&") // last, so "&amp;quot;" doesn't decode twice
