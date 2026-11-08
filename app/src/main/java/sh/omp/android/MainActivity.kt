package sh.omp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import com.termux.terminal.TerminalSession
import com.termux.view.TerminalView

private val Background = Color(0xFF090B10)
private val Panel = Color(0xFF121722)
private val Accent = Color(0xFFB7F397)
private val Muted = Color(0xFF8993A5)

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = android.graphics.Color.rgb(9, 11, 16)
        window.navigationBarColor = android.graphics.Color.rgb(9, 11, 16)
        setContent {
            MaterialTheme(colorScheme = androidx.compose.material3.darkColorScheme()) {
                OmpTerminalScreen()
            }
        }
    }
}

@Composable
private fun OmpTerminalScreen() {
    val context = LocalContext.current
    val client = remember(context) { EmbeddedTerminalClient(context.applicationContext) }
    val session = remember(context) {
        TerminalSession(
            "/system/bin/sh",
            context.filesDir.absolutePath,
            arrayOf("/system/bin/sh"),
            arrayOf(
                "HOME=${context.filesDir.absolutePath}",
                "PATH=/system/bin:/system/xbin",
                "TERM=xterm-256color",
                "LANG=C.UTF-8"
            ),
            2000,
            client
        )
    }

    DisposableEffect(session) {
        onDispose { session.finishIfRunning() }
    }

    Surface(Modifier.fillMaxSize(), color = Background) {
        Column(
            Modifier.fillMaxSize().padding(14.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                Modifier.fillMaxWidth().background(Panel, RoundedCornerShape(14.dp)).padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(Modifier.weight(1f)) {
                    Text("OMP", color = Color.White, fontSize = 22.sp, fontWeight = FontWeight.Bold)
                    Text("ANDROID · EMBEDDED TERMINAL", color = Muted, fontSize = 10.sp, letterSpacing = 1.1.sp)
                }
                Text("PTY MVP", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Box(Modifier.size(7.dp).background(Accent, RoundedCornerShape(50)))
                Text("LOCAL ANDROID SHELL", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.SemiBold)
                Spacer(Modifier.weight(1f))
                Text("NO TERMUX APP · NO LOCALHOST", color = Muted, fontSize = 9.sp)
            }

            AndroidView(
                modifier = Modifier.fillMaxWidth().weight(1f).background(Color(0xFF05070A), RoundedCornerShape(12.dp)),
                factory = { viewContext ->
                    TerminalView(viewContext, null).apply {
                        setTerminalViewClient(client)
                        setTextSize(14)
                        isFocusableInTouchMode = true
                        attachSession(session)
                        client.attachTerminalView(this)
                        requestFocus()
                    }
                },
                update = { view ->
                    if (view.getCurrentSession() !== session) view.attachSession(session)
                }
            )

            Text(
                "Embedded PTY foundation. OMP/Bun runtime is not installed in this build yet.",
                color = Muted,
                fontSize = 10.sp,
                modifier = Modifier.padding(horizontal = 3.dp)
            )
        }
    }
}
