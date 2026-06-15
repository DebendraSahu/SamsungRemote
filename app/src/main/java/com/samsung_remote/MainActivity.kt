package com.samsung_remote

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.lifecycleScope
import com.samsung_remote.tv.SamsungTv
import com.samsung_remote.tv.Status
import com.samsung_remote.tv.TvScanner
import com.samsung_remote.ui.DarkColors
import com.samsung_remote.ui.RemoteLayout
import com.samsung_remote.ui.RemoteScreen
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {

    private lateinit var tv: SamsungTv
    private lateinit var scanner: TvScanner
    private lateinit var layout: RemoteLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        // Transparent bars, so the shell's own gradient runs the full height of the screen
        // rather than stopping at a black strip under the clock.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.dark(android.graphics.Color.TRANSPARENT),
        )
        super.onCreate(savedInstanceState)
        scanner = TvScanner(this)
        tv = SamsungTv(this, scanner)
        layout = RemoteLayout(this)
        setContent {
            MaterialTheme(colorScheme = DarkColors) {
                RemoteScreen(tv, scanner, layout)
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Reconnect to the one remembered TV whenever the app comes forward. This also
        // relocates it if the router handed it a new address, which is why it needs a scope.
        if (tv.status.value == Status.DISCONNECTED) {
            lifecycleScope.launch { tv.connectRemembered() }
        }
    }

    override fun onDestroy() {
        tv.disconnect()
        super.onDestroy()
    }
}
