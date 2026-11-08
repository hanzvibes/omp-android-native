package sh.omp.android

import android.content.BroadcastReceiver
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.content.ContextCompat

private const val TERMUX_PACKAGE = "com.termux"
private const val TERMUX_PREREQUISITES = "pkg update && pkg install coreutils curl git clang cmake make pkg-config rust python libc++"
private const val OMP_INSTALL_COMMAND = "curl -fsSL https://raw.githubusercontent.com/PlayWithG/oh-my-pi-termux/termux/scripts/install.sh | sh -s -- --source"

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(
                colorScheme = darkColorScheme(
                    background = Color(0xFF0B0D12),
                    surface = Color(0xFF151923),
                    primary = Color(0xFFB7F397)
                )
            ) { OmpHomeScreen() }
        }
    }
}

@Composable
private fun OmpHomeScreen() {
    val context = LocalContext.current
    var termuxInstalled by remember { mutableStateOf(isTermuxInstalled(context)) }
    var runtimeResult by remember { mutableStateOf(readRuntimeResult(context)) }
    var checking by remember { mutableStateOf(false) }

    DisposableEffect(context) {
        val receiver = object : BroadcastReceiver() {
            override fun onReceive(receiverContext: Context?, intent: Intent?) {
                runtimeResult = readRuntimeResult(context)
                checking = false
            }
        }
        ContextCompat.registerReceiver(
            context,
            receiver,
            IntentFilter(TermuxResultService.ACTION_RESULT),
            ContextCompat.RECEIVER_NOT_EXPORTED
        )
        onDispose { context.unregisterReceiver(receiver) }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(15.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OMP", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("ANDROID NATIVE", color = Muted, fontSize = 11.sp)
                }
                Text("EARLY ACCESS", color = Accent, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }

            StatusCard(
                title = "TERMUX ENVIRONMENT",
                value = if (termuxInstalled) "Detected" else "Not installed",
                detail = if (termuxInstalled) {
                    "Termux is available. OMP still needs to be installed and verified."
                } else {
                    "Install Termux first. This APK uses Termux as its Android ARM64 runtime."
                },
                statusColor = if (termuxInstalled) Accent else Warning
            )

            StatusCard(
                title = "OMP RUNTIME",
                value = runtimeResult.status,
                detail = runtimeResult.detail,
                statusColor = if (runtimeResult.exitCode == 0) Accent else Warning
            )

            if (runtimeResult.output.isNotBlank()) {
                Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(14.dp)) {
                    Column(Modifier.fillMaxWidth().padding(14.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                        Text("LAST CHECK OUTPUT", color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
                        Text(runtimeResult.output, color = Color(0xFFE0E4EA), fontSize = 12.sp)
                    }
                }
            }

            Text("PROJECT", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            WorkspaceManager()

            Text("RUNTIME CONNECTION", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
                Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
                    Text("1. Allow OMP Android in Termux", fontWeight = FontWeight.SemiBold)
                    Text(
                        "Open Android Settings → Apps → OMP Android → Permissions → Additional permissions → Run commands in Termux environment.",
                        color = Muted, fontSize = 12.sp
                    )
                    OutlinedButton(onClick = {
                        context.startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.parse("package:${context.packageName}")))
                    }) { Text("Open app permissions") }

                    Divider(color = Color(0xFF303644))

                    Text("2. Enable external commands in Termux", fontWeight = FontWeight.SemiBold)
                    Text(
                        "In Termux, set allow-external-apps=true in ~/.termux/termux.properties, then restart Termux.",
                        color = Muted, fontSize = 12.sp
                    )
                    CopyCommandButton("Copy Termux setting command", "mkdir -p ~/.termux && grep -q '^allow-external-apps' ~/.termux/termux.properties 2>/dev/null && sed -i 's/^allow-external-apps.*/allow-external-apps=true/' ~/.termux/termux.properties || echo 'allow-external-apps=true' >> ~/.termux/termux.properties")
                }
            }

            Text("SETUP", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            SetupStep("01", "Open Termux", "Launch Termux and complete its first-run setup.") {
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = {
                            val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
                            if (launchIntent != null) context.startActivity(launchIntent)
                            else Toast.makeText(context, "Termux is not installed", Toast.LENGTH_SHORT).show()
                        },
                        enabled = termuxInstalled,
                        colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10140D))
                    ) { Text("Open Termux") }
                    OutlinedButton(onClick = { termuxInstalled = isTermuxInstalled(context) }) { Text("Refresh") }
                }
            }
            SetupStep("02", "Install prerequisites", "Copy and run this inside Termux.") {
                CopyCommandButton("Copy prerequisites", TERMUX_PREREQUISITES)
            }
            SetupStep("03", "Install OMP", "Review the community installer, then run it manually inside Termux.") {
                CopyCommandButton("Copy OMP installer", OMP_INSTALL_COMMAND)
            }

            Button(
                onClick = {
                    checking = true
                    runtimeResult = RuntimeCheckResult("Checking…", "Waiting for Termux to return the command result.", "", null)
                    val error = TermuxCommandBridge.checkOmp(context)
                    if (error != null) {
                        checking = false
                        runtimeResult = RuntimeCheckResult("Check failed", error, "", null)
                    }
                },
                enabled = termuxInstalled && !checking,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10140D))
            ) { Text(if (checking) "Checking OMP…" else "Check OMP runtime") }

            Text(
                "Only this fixed diagnostic command is sent. Installer commands are never executed by the APK.",
                color = Muted, fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String, detail: String, statusColor: Color) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(18.dp)) {
        Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = statusColor, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SetupStep(number: String, title: String, description: String, content: @Composable ColumnScope.() -> Unit) {
    Card(colors = CardDefaults.cardColors(containerColor = SurfaceColor), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
                Text(number, color = Accent, fontWeight = FontWeight.Bold, fontSize = 12.sp)
                Text(title, fontWeight = FontWeight.SemiBold)
            }
            Text(description, color = Muted, fontSize = 12.sp)
            content()
        }
    }
}

@Composable
private fun CopyCommandButton(label: String, command: String) {
    val context = LocalContext.current
    OutlinedButton(onClick = { copyToClipboard(context, label, command) }, colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)) {
        Text(label)
    }
}

private fun isTermuxInstalled(context: Context): Boolean = runCatching {
    @Suppress("DEPRECATION")
    context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
    true
}.getOrDefault(false)

private fun copyToClipboard(context: Context, label: String, command: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, command))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

private fun readRuntimeResult(context: Context): RuntimeCheckResult {
    val prefs = context.getSharedPreferences(TermuxResultService.PREFS, Context.MODE_PRIVATE)
    return RuntimeCheckResult(
        status = prefs.getString("status", "Not checked") ?: "Not checked",
        detail = prefs.getString("detail", "Run a diagnostic check to verify OMP.") ?: "",
        output = prefs.getString("output", "") ?: "",
        exitCode = if (prefs.contains("exit_code")) prefs.getInt("exit_code", -1) else null
    )
}

private data class RuntimeCheckResult(
    val status: String,
    val detail: String,
    val output: String,
    val exitCode: Int?
)

private val Accent = Color(0xFFB7F397)
private val Muted = Color(0xFF9299A8)
private val Warning = Color(0xFFFFC66D)
private val SurfaceColor = Color(0xFF151923)
