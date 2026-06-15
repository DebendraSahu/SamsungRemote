package com.samsung_remote.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.samsung_remote.tv.Keys
import com.samsung_remote.tv.SamsungTv
import com.samsung_remote.tv.Status
import com.samsung_remote.tv.TvDevice
import com.samsung_remote.tv.TvScanner
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch

@Composable
internal fun RemoteScreen(tv: SamsungTv, scanner: TvScanner, layout: RemoteLayout) {
    val status by tv.status.collectAsState()
    val tvName by tv.name.collectAsState()
    val tvIp by tv.ip.collectAsState()
    val availableApps by tv.availableApps.collectAsState()
    val favourites by layout.favourites.collectAsState()
    val sections by layout.sections.collectAsState()
    val snackbars = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    var picking by remember { mutableStateOf(false) }
    var customising by remember { mutableStateOf(false) }
    var scanning by remember { mutableStateOf(false) }
    var devices by remember { mutableStateOf(emptyList<TvDevice>()) }
    var scanJob by remember { mutableStateOf<Job?>(null) }

    LaunchedEffect(tv) {
        tv.messages.collect { snackbars.showSnackbar(it) }
    }

    // A TV reached without a name asks for one once connected, so the header is never a bare IP.
    LaunchedEffect(status, tvName) {
        if (status == Status.CONNECTED && tvName.isBlank() && tvIp.isNotEmpty()) {
            scanner.lookup(tvIp)?.let { if (it.identified) tv.rememberName(it.name) }
        }
    }

    // Work out this TV's own ids for the streaming shortcuts.
    LaunchedEffect(status, tvIp) {
        if (status == Status.CONNECTED && tvIp.isNotEmpty()) tv.resolveApps()
    }

    // Keys talk straight to the TV; the haptic tick belongs to the key that was pressed.
    val press: (String) -> Unit = { key -> tv.sendKey(key) }
    val startScan = {
        scanJob?.cancel()
        devices = emptyList()
        scanning = true
        picking = true
        scanJob = scope.launch {
            try {
                scanner.scan().collect { found ->
                    if (devices.none { it.ip == found.ip }) devices = devices + found
                }
            } finally {
                scanning = false
            }
        }
    }
    val stopScan = {
        scanJob?.cancel()
        scanning = false
        picking = false
    }

    // Back should leave the customise screen rather than the app.
    BackHandler(enabled = customising) { customising = false }

    // Nothing paired yet: go straight to finding a TV rather than showing a dead remote.
    LaunchedEffect(Unit) {
        if (tvIp.isEmpty()) startScan()
    }

    Scaffold(
        snackbarHost = {
            SnackbarHost(snackbars) { data ->
                Snackbar(
                    snackbarData = data,
                    containerColor = KeyPlastic.lighten(0.08f),
                    contentColor = Label,
                    actionContentColor = Label,
                    shape = RoundedCornerShape(14.dp),
                )
            }
        },
        containerColor = Color.Transparent,
        modifier = Modifier
            .fillMaxSize()
            .shell(),
    ) { insets ->
        // The customise screen takes the whole frame rather than floating over the remote:
        // rearranging keys is a job you finish, not something you peek at.
        if (customising) {
            Box(
                Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .padding(horizontal = 16.dp)
                    .padding(top = 2.dp, bottom = 14.dp)
            ) {
                CustomiseScreen(layout) { customising = false }
            }
        } else {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(insets)
                    .padding(horizontal = 16.dp)
                    // Extra room under the last row so the app keys clear the gesture bar.
                    .padding(top = 2.dp, bottom = 14.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                IrWindow()

                Header(
                    name = tvName,
                    ipKnown = tvIp.isNotEmpty(),
                    status = status,
                    onPower = { press(Keys.POWER) },
                    onConnect = { scope.launch { tv.connectRemembered(announce = true) } },
                    onDisconnect = { tv.disconnect() },
                    onFindOther = startScan,
                    onForget = { tv.forget() },
                    onCustomise = { customising = true },
                )

                Seam()

                // The blocks are stacked in whatever order the viewer arranged them. The pad keeps
                // the weight wherever it lands, so it still absorbs the leftover height.
                sections.forEach { section ->
                    when (section) {
                        Section.DPAD -> DPad(Modifier.weight(1f), press)
                        Section.NAV -> NavRow(press)
                        Section.MEDIA -> MediaRow(press)
                        Section.ROCKERS -> RockerRow(press)
                        Section.APPS -> AppTray(
                            order = RemoteLayout.appOrder(favourites, availableApps),
                            available = availableApps,
                            connected = status == Status.CONNECTED,
                            onOpen = { tv.launch(it) },
                        )
                    }
                }
            }
        }
    }

    if (picking) {
        PickerDialog(
            scanning = scanning,
            devices = devices,
            onSelect = { device ->
                stopScan()
                // An unidentified host's "name" is just its IP, so don't store that as a name.
                // The uuid is what decides whether this is the same TV as before, and so
                // whether the saved token survives.
                tv.connect(
                    rawIp = device.ip,
                    tvName = device.name.takeIf { device.identified },
                    tvUuid = device.uuid.takeIf { it.isNotBlank() },
                )
            },
            onRescan = startScan,
            onDismiss = stopScan,
        )
    }
}
