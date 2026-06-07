package com.samsung_remote.tv

import android.annotation.SuppressLint
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Base64
import java.io.IOException
import java.net.ConnectException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.SSLException
import javax.net.ssl.SSLSocketFactory
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import okhttp3.Call
import okhttp3.Callback
import okhttp3.ConnectionSpec
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import org.json.JSONObject

private data class InstalledApp(val id: String, val name: String, val type: Int)

/**
 * Talks to a Samsung Tizen TV (2016 and newer) over its WebSocket remote-control channel.
 *
 * Exactly one TV is remembered. The first connection makes the TV show an "Allow" popup; when
 * the viewer accepts, the TV replies with `ms.channel.connect` carrying a token which is
 * persisted so later launches connect silently. Pointing the app at a different TV discards
 * the previous pairing entirely.
 */
class SamsungTv(context: Context, private val scanner: TvScanner) {

    private val appContext = context.applicationContext
    private val prefs = appContext.getSharedPreferences("samsung_remote", Context.MODE_PRIVATE)

    private val _status = MutableStateFlow(Status.DISCONNECTED)
    val status: StateFlow<Status> = _status.asStateFlow()

    /** Friendly name of the remembered TV, e.g. "Living Room". Blank until one is known. */
    private val _name = MutableStateFlow(prefs.getString(PREF_NAME, "").orEmpty())
    val name: StateFlow<String> = _name.asStateFlow()

    /** Address of the remembered TV, blank when none is paired. */
    private val _ip = MutableStateFlow(prefs.getString(PREF_IP, "").orEmpty())
    val ip: StateFlow<String> = _ip.asStateFlow()

    /** User-facing problems, surfaced as snackbars. */
    private val _messages = MutableSharedFlow<String>(
        extraBufferCapacity = 8,
        onBufferOverflow = BufferOverflow.DROP_OLDEST
    )
    val messages: SharedFlow<String> = _messages.asSharedFlow()

    private val client = buildClient()
    private var socket: WebSocket? = null

    @Volatile
    private var installed: List<InstalledApp> = emptyList()

    /**
     * App ids confirmed present on this TV, from probing its :8001 endpoint.
     *
     * 2024 firmware ignores the `ed.installedApp.get` WebSocket event entirely, so HTTP probing
     * is the only enumeration that still works and this map is the primary source.
     */
    @Volatile
    private var resolved: Map<TvApp, String> = emptyMap()

    /** Which shortcuts this TV can actually open, so the UI never offers a dead key. */
    private val _availableApps = MutableStateFlow<Set<TvApp>>(emptySet())
    val availableApps: StateFlow<Set<TvApp>> = _availableApps.asStateFlow()

    /** Plain HTTP for the TV's :8001 REST API — separate from the trust-all wss client. */
    private val rest = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(6, TimeUnit.SECONDS)
        .build()

    /**
     * Bumped on every (re)connect. A listener whose generation no longer matches belongs to a
     * socket we've moved on from, so its late callbacks must not clobber the current status.
     */
    private var generation = 0

    val savedIp: String get() = _ip.value

    init {
        // An older build kept one token per IP. Only a single TV is remembered now, so clear
        // any strays rather than leaving dead keys behind.
        val legacy = prefs.all.keys.filter { it.startsWith("token_") }
        if (legacy.isNotEmpty()) {
            prefs.edit().apply { legacy.forEach { remove(it) } }.apply()
        }
    }

    /** True when the phone is on Wi-Fi; the TV is only reachable over the local network. */
    fun isOnWifi(): Boolean {
        val cm = appContext.getSystemService(Context.CONNECTIVITY_SERVICE) as? ConnectivityManager
            ?: return false
        val caps = cm.getNetworkCapabilities(cm.activeNetwork) ?: return false
        return caps.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    /**
     * Pair with [rawIp] and remember it as *the* TV.
     *
     * A TV is identified by [tvUuid], never by its address. Routers hand out a different
     * address every few days, and treating that as "a different TV" threw away the access
     * token and made the set ask the viewer to approve the app all over again — which is
     * exactly the daily re-approval this avoids. The pairing is only discarded when the
     * uuid genuinely differs, i.e. it really is another TV.
     */
    fun connect(rawIp: String, tvName: String? = null, tvUuid: String? = null) {
        val host = rawIp.trim()
        if (host.isEmpty()) return

        val knownUuid = prefs.getString(PREF_UUID, "").orEmpty()
        val sameTv = when {
            !tvUuid.isNullOrBlank() && knownUuid.isNotBlank() -> tvUuid == knownUuid
            // No uuid to compare (a hand-typed address, or a set that wouldn't identify
            // itself): fall back to the address, which is the best signal available.
            else -> host == _ip.value
        }

        if (!sameTv) {
            prefs.edit().remove(PREF_TOKEN).remove(PREF_NAME).remove(PREF_UUID).apply()
            _name.value = ""
            installed = emptyList()
            resolved = emptyMap()
            _availableApps.value = emptySet()
        }

        prefs.edit().apply {
            putString(PREF_IP, host)
            if (!tvUuid.isNullOrBlank()) putString(PREF_UUID, tvUuid)
            apply()
        }
        _ip.value = host
        if (!tvName.isNullOrBlank()) rememberName(tvName)

        open(host)
    }

    /**
     * Connect to the remembered TV, finding it again if the router moved it.
     *
     * Checks the saved address still answers *as the same TV*; if it doesn't, scans the
     * network for the saved uuid and connects there instead. The token survives, so this
     * is silent — no popup on the TV.
     */
    suspend fun connectRemembered(announce: Boolean = false) {
        val host = _ip.value
        val uuid = prefs.getString(PREF_UUID, "").orEmpty()
        if (host.isEmpty()) {
            if (announce) emit("No TV paired yet")
            return
        }
        if (!isOnWifi()) {
            if (announce) emit("Join a Wi-Fi network to reach your TV")
            return
        }

        // Still there? (Also covers the case where we never learned the uuid.)
        val atSavedAddress = scanner.lookup(host)
        if (atSavedAddress != null && (uuid.isBlank() || atSavedAddress.uuid == uuid)) {
            if (uuid.isBlank() && atSavedAddress.uuid.isNotBlank()) {
                prefs.edit().putString(PREF_UUID, atSavedAddress.uuid).apply()
            }
            open(host)
            return
        }

        if (uuid.isBlank()) {
            // Nothing stable to search for; the address is all we have.
            open(host)
            return
        }

        _status.value = Status.CONNECTING
        val moved = scanner.findByUuid(uuid)
        if (moved == null) {
            _status.value = Status.DISCONNECTED
            // Only when the viewer asked. The silent reconnect on every resume would
            // otherwise nag about a TV that is simply switched off; the header already
            // says "Disconnected".
            if (announce) emit("Couldn't find your TV on this Wi-Fi")
            return
        }
        prefs.edit().putString(PREF_IP, moved.ip).apply()
        _ip.value = moved.ip
        if (moved.name.isNotBlank()) rememberName(moved.name)
        open(moved.ip)
    }

    fun disconnect() {
        generation++
        closeSocket()
        _status.value = Status.DISCONNECTED
    }

    /** Drop the remembered TV entirely, token included. */
    fun forget() {
        disconnect()
        prefs.edit().clear().apply()
        _name.value = ""
        _ip.value = ""
        installed = emptyList()
    }

    /** Fills in the name of a TV that was reached without one. */
    fun rememberName(tvName: String) {
        if (tvName.isBlank()) return
        prefs.edit().putString(PREF_NAME, tvName).apply()
        _name.value = tvName
    }

    fun sendKey(key: String) {
        val ws = connectedSocket() ?: return
        val payload = JSONObject()
            .put("method", "ms.remote.control")
            .put(
                "params", JSONObject()
                    .put("Cmd", "Click")
                    .put("DataOfCmd", key)
                    .put("Option", "false")
                    .put("TypeOfRemote", "SendRemoteKey")
            )
        if (!ws.send(payload.toString())) emit("Couldn't reach the TV")
    }

    /**
     * Works out which id each shortcut uses on this TV.
     *
     * `GET /applications/<id>` answers 200 for an installed app and 404 otherwise. That is the
     * only app enumeration current firmware still honours — the WebSocket `ed.installedApp.get`
     * event is ignored outright on 2024 sets, which just keep pinging instead of replying.
     */
    suspend fun resolveApps() = withContext(Dispatchers.IO) {
        val host = _ip.value
        if (host.isEmpty()) return@withContext
        // The catalogue is large, so probe apps in parallel -- but only a few at a time.
        // These sets accept roughly a dozen concurrent connections; past that they start
        // refusing, and a refusal is indistinguishable from "app not installed".
        val gate = Semaphore(PROBE_PARALLEL)
        val found = coroutineScope {
            TvApp.entries.map { app ->
                async {
                    val fromTv = installed.firstOrNull { app.matches(it.name) }?.id
                    val id = fromTv ?: app.candidateIds.firstOrNull {
                        gate.withPermit { appInstalled(host, it) }
                    }
                    id?.let { app to it }
                }
            }.awaitAll().filterNotNull().toMap()
        }
        resolved = found
        _availableApps.value = found.keys
    }

    /**
     * True when the TV confirms the app is installed.
     *
     * Only a real 404 means "absent" — a timeout is not an answer, so it is retried once
     * rather than quietly hiding an app the viewer actually has.
     */
    private fun appInstalled(host: String, appId: String): Boolean {
        repeat(2) {
            try {
                rest.newCall(Request.Builder().url(appUrl(host, appId)).build())
                    .execute().use { response ->
                        if (response.isSuccessful) return true
                        if (response.code == 404) return false
                    }
            } catch (e: Exception) {
                // Network hiccup rather than an answer: fall through and try again.
            }
        }
        return false
    }

    /**
     * Opens a streaming app.
     *
     * `POST /applications/<id>` is what current firmware acts on, and what Home Assistant and
     * samsungtvws use for modern sets. The older `ed.apps.launch` WebSocket event is accepted
     * silently and does nothing on a 2024 TV, so it is only a fallback for older models.
     */
    fun launch(app: TvApp) {
        val host = _ip.value
        if (host.isEmpty()) {
            emit("No TV paired yet")
            return
        }
        // A confirmed id if the probe found one; otherwise work through the candidates anyway.
        // An unconfirmed key is still worth pressing: probing can miss, and a set that refuses
        // to enumerate its apps may well be holding one it never admitted to.
        val known = resolved[app] ?: installed.firstOrNull { app.matches(it.name) }?.id
        attemptLaunch(app, host, known?.let { listOf(it) } ?: app.candidateIds, 0)
    }

    /** Tries each candidate in turn, stopping at the first the TV accepts. */
    private fun attemptLaunch(app: TvApp, host: String, ids: List<String>, index: Int) {
        if (index > ids.lastIndex) {
            emit("${app.label} isn't on this TV")
            return
        }
        val appId = ids[index]
        val request = Request.Builder()
            .url(appUrl(host, appId))
            .post(ByteArray(0).toRequestBody())
            .build()
        rest.newCall(request).enqueue(object : Callback {
            // Couldn't reach the TV at all, which is not the same as the app being absent —
            // the old control-channel route may still land on an older set.
            override fun onFailure(call: Call, e: IOException) = legacyLaunch(appId)

            override fun onResponse(call: Call, response: Response) {
                response.use {
                    when {
                        it.isSuccessful -> confirmApp(app, appId)
                        // A definite "no such app": try the next id this app is known by.
                        it.code == 404 -> attemptLaunch(app, host, ids, index + 1)
                        else -> legacyLaunch(appId)
                    }
                }
            }
        })
    }

    /** Learned by pressing rather than probing, so the key stops showing as unconfirmed. */
    private fun confirmApp(app: TvApp, appId: String) {
        resolved = resolved + (app to appId)
        _availableApps.value = _availableApps.value + app
    }

    /** Pre-2020 route: ask over the control channel instead. Ignored by newer firmware. */
    private fun legacyLaunch(appId: String) {
        val payload = JSONObject()
            .put("method", "ms.channel.emit")
            .put(
                "params", JSONObject()
                    .put("event", "ed.apps.launch")
                    .put("to", "host")
                    .put(
                        "data", JSONObject()
                            .put("appId", appId)
                            .put("action_type", "DEEP_LINK")
                    )
            )
        if (socket?.send(payload.toString()) != true) emit("Couldn't open that app")
    }

    private fun appUrl(host: String, appId: String) =
        "http://$host:$INFO_PORT/api/v2/applications/$appId"

    private fun connectedSocket(): WebSocket? {
        val ws = socket
        if (ws == null || _status.value != Status.CONNECTED) {
            emit(if (_ip.value.isEmpty()) "No TV paired yet" else "Not connected to your TV")
            return null
        }
        return ws
    }

    /** Asks the TV which apps it has, so shortcuts resolve to real ids. */
    private fun requestInstalledApps() {
        val payload = JSONObject()
            .put("method", "ms.channel.emit")
            .put(
                "params", JSONObject()
                    .put("event", "ed.installedApp.get")
                    .put("to", "host")
            )
        socket?.send(payload.toString())
    }

    private fun open(host: String) {
        closeSocket()
        val gen = ++generation
        val token = prefs.getString(PREF_TOKEN, null)
        _status.value = Status.CONNECTING

        val url = buildString {
            append("wss://").append(host).append(':').append(PORT)
            append("/api/v2/channels/samsung.remote.control")
            append("?name=").append(Base64.encodeToString(APP_NAME.toByteArray(), Base64.NO_WRAP))
            if (!token.isNullOrEmpty()) append("&token=").append(token)
        }

        val request = try {
            Request.Builder().url(url).build()
        } catch (e: IllegalArgumentException) {
            _status.value = Status.DISCONNECTED
            emit("\"$host\" isn't a valid address")
            return
        }
        socket = client.newWebSocket(request, Listener(gen, host, token != null))
    }

    private fun closeSocket() {
        socket?.close(NORMAL_CLOSURE, null)
        socket = null
    }

    private inner class Listener(
        private val gen: Int,
        private val host: String,
        private val hadToken: Boolean
    ) : WebSocketListener() {

        private val stale: Boolean get() = gen != generation

        override fun onOpen(webSocket: WebSocket, response: Response) {
            if (stale) return
            // The TV stays quiet until it accepts us. With no saved token, that silence means
            // the "Allow" popup is on screen waiting for the viewer.
            _status.value = if (hadToken) Status.CONNECTING else Status.WAITING_APPROVAL
        }

        override fun onMessage(webSocket: WebSocket, text: String) {
            if (stale) return
            val json = try {
                JSONObject(text)
            } catch (e: Exception) {
                return
            }
            when (json.optString("event")) {
                "ms.channel.connect" -> {
                    json.optJSONObject("data")
                        ?.optString("token")
                        ?.takeIf { it.isNotEmpty() && it != "null" }
                        ?.let { prefs.edit().putString(PREF_TOKEN, it).apply() }
                    _status.value = Status.CONNECTED
                    requestInstalledApps()
                }

                "ed.installedApp.get" -> {
                    val apps = json.optJSONObject("data")?.optJSONArray("data") ?: return
                    installed = (0 until apps.length()).mapNotNull { i ->
                        val entry = apps.optJSONObject(i) ?: return@mapNotNull null
                        val id = entry.optString("appId")
                        if (id.isBlank()) null
                        else InstalledApp(id, entry.optString("name"), entry.optInt("app_type", 2))
                    }
                }

                "ms.channel.unauthorized" -> {
                    prefs.edit().remove(PREF_TOKEN).apply()
                    _status.value = Status.DISCONNECTED
                    emit("The TV refused the connection. Allow \"$APP_NAME\" on the TV, then reconnect.")
                }

                "ms.channel.timeOut" -> {
                    _status.value = Status.DISCONNECTED
                    emit("The TV timed out waiting for approval.")
                }
            }
        }

        override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
            if (stale) return
            val deniedOnTv = _status.value == Status.WAITING_APPROVAL
            _status.value = Status.DISCONNECTED
            if (deniedOnTv) emit("The TV closed the connection — was \"$APP_NAME\" denied?")
        }

        override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
            if (stale) return
            _status.value = Status.DISCONNECTED
            emit(explain(t, host))
        }
    }

    private fun explain(t: Throwable, host: String): String = when (t) {
        is SocketTimeoutException -> "Timed out reaching the TV. Is it switched on?"
        is ConnectException -> "Can't reach $host. Check the TV is on and on this Wi-Fi."
        is UnknownHostException -> "\"$host\" couldn't be resolved."
        is SSLException -> "Secure handshake failed. Is this a 2016-or-newer Tizen TV?"
        else -> t.message?.takeIf { it.isNotBlank() } ?: "Connection to the TV was lost."
    }

    private fun emit(message: String) {
        _messages.tryEmit(message)
    }

    /**
     * Tizen TVs present a self-signed certificate on :8002, so this client accepts any
     * certificate. It is deliberately confined to this class — the LAN WebSocket above is the
     * only traffic that ever goes through it.
     */
    @SuppressLint("CustomX509TrustManager", "TrustAllX509TrustManager")
    private fun buildClient(): OkHttpClient {
        val trustManager = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) = Unit
            override fun getAcceptedIssuers(): Array<X509Certificate> = emptyArray()
        }
        val ssl: SSLSocketFactory = SSLContext.getInstance("TLS").apply {
            init(null, arrayOf<TrustManager>(trustManager), SecureRandom())
        }.socketFactory

        return OkHttpClient.Builder()
            .sslSocketFactory(ssl, trustManager)
            .hostnameVerifier { _, _ -> true }
            .connectionSpecs(listOf(ConnectionSpec.MODERN_TLS, ConnectionSpec.COMPATIBLE_TLS))
            .connectTimeout(10, TimeUnit.SECONDS)
            .readTimeout(0, TimeUnit.MILLISECONDS) // a remote session is idle most of the time
            .pingInterval(30, TimeUnit.SECONDS)
            .build()
    }

    private companion object {
        const val APP_NAME = "Samsung Remote"
        const val PORT = 8002
        const val INFO_PORT = 8001
        const val PROBE_PARALLEL = 8
        const val NORMAL_CLOSURE = 1000
        const val PREF_IP = "tv_ip"
        const val PREF_NAME = "tv_name"
        const val PREF_UUID = "tv_uuid"
        const val PREF_TOKEN = "tv_token"
    }
}
