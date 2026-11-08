package sh.omp.android

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

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

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
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
                    "Termux is available. OMP itself still needs to be installed and verified."
                } else {
                    "Install the Termux app first. This APK does not bundle a Linux userspace."
                },
                statusColor = if (termuxInstalled) Accent else Color(0xFFFFC66D)
            )

            StatusCard(
                title = "OMP RUNTIME",
                value = "Not verified",
                detail = "The app cannot inspect Termux private files directly. Verify OMP from the Termux shell.",
                statusColor = Color(0xFFFFC66D)
            )

            Text("SETUP", color = Muted, fontSize = 11.sp, fontWeight = FontWeight.Bold)
            SetupStep(
                number = "01",
                title = "Open Termux",
                description = "Launch the installed Termux app, then return here to refresh its status."
            ) {
                Button(
                    onClick = {
                        val launchIntent = context.packageManager.getLaunchIntentForPackage(TERMUX_PACKAGE)
                        if (launchIntent != null) {
                            context.startActivity(launchIntent)
                        } else {
                            Toast.makeText(context, "Termux is not installed", Toast.LENGTH_SHORT).show()
                        }
                    },
                    enabled = termuxInstalled,
                    colors = ButtonDefaults.buttonColors(containerColor = Accent, contentColor = Color(0xFF10140D))
                ) { Text("Open Termux") }
                OutlinedButton(onClick = { termuxInstalled = isTermuxInstalled(context) }) {
                    Text("Refresh status")
                }
            }

            SetupStep(
                number = "02",
                title = "Install build prerequisites",
                description = "Copy this command, paste it inside Termux, and wait for packages to finish installing."
            ) {
                CopyCommandButton("Copy prerequisites", TERMUX_PREREQUISITES)
            }

            SetupStep(
                number = "03",
                title = "Install OMP from source",
                description = "This community installer builds the Android-native addon. Review the source before running it."
            ) {
                CopyCommandButton("Copy OMP installer", OMP_INSTALL_COMMAND)
            }

            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF202817)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "No shell command is executed by this app. Commands are copied only after you tap the button, then you run them manually in Termux.",
                    Modifier.padding(14.dp), color = Accent, fontSize = 12.sp
                )
            }

            Text(
                "After installation, run: omp --version  •  omp --help",
                color = Muted,
                fontSize = 11.sp
            )
        }
    }
}

@Composable
private fun StatusCard(title: String, value: String, detail: String, statusColor: Color) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(18.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(7.dp)
        ) {
            Text(title, color = Muted, fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(value, color = statusColor, fontSize = 21.sp, fontWeight = FontWeight.SemiBold)
            Text(detail, color = Muted, fontSize = 12.sp)
        }
    }
}

@Composable
private fun SetupStep(
    number: String,
    title: String,
    description: String,
    content: @Composable ColumnScope.() -> Unit
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceColor),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
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
    OutlinedButton(
        onClick = { copyToClipboard(context, label, command) },
        colors = ButtonDefaults.outlinedButtonColors(contentColor = Accent)
    ) { Text(label) }
}

private fun isTermuxInstalled(context: Context): Boolean =
    runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    }.getOrDefault(false)

private fun copyToClipboard(context: Context, label: String, command: String) {
    val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
    clipboard.setPrimaryClip(ClipData.newPlainText(label, command))
    Toast.makeText(context, "$label copied", Toast.LENGTH_SHORT).show()
}

private val Accent = Color(0xFFB7F397)
private val Muted = Color(0xFF9299A8)
private val SurfaceColor = Color(0xFF151923)
