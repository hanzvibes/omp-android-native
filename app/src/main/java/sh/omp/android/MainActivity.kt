package sh.omp.android

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            MaterialTheme(colorScheme = darkColorScheme(
                background = Color(0xFF0B0D12),
                surface = Color(0xFF151923),
                primary = Color(0xFFB7F397)
            )) { OmpHomeScreen() }
        }
    }
}

@Composable
private fun OmpHomeScreen() {
    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        Column(
            Modifier.fillMaxSize().padding(22.dp),
            verticalArrangement = Arrangement.spacedBy(18.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Column(Modifier.weight(1f)) {
                    Text("OMP", fontSize = 28.sp, fontWeight = FontWeight.Bold)
                    Text("ANDROID NATIVE", color = Color(0xFF9299A8), fontSize = 11.sp)
                }
                Text("EARLY ACCESS", color = Color(0xFFB7F397), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            }
            Card(
                colors = CardDefaults.cardColors(containerColor = Color(0xFF151923)),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(Modifier.fillMaxWidth().padding(18.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("RUNTIME STATUS", color = Color(0xFF9299A8), fontSize = 10.sp)
                    Text("Not installed", fontSize = 22.sp, fontWeight = FontWeight.SemiBold)
                    Text("The Android ARM64 runtime has not been bootstrapped yet.", color = Color(0xFF9299A8), fontSize = 13.sp)
                }
            }
            Text("WORKSPACE", color = Color(0xFF9299A8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151923))) {
                Column(Modifier.fillMaxWidth().padding(16.dp), verticalArrangement = Arrangement.spacedBy(5.dp)) {
                    Text("No project selected", fontWeight = FontWeight.Medium)
                    Text("Workspace picker is planned for the next milestone.", color = Color(0xFF9299A8), fontSize = 12.sp)
                }
            }
            Text("QUICK ACCESS", color = Color(0xFF9299A8), fontSize = 11.sp, fontWeight = FontWeight.Bold)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                FeatureCard("Agent", "OMP sessions", Modifier.weight(1f))
                FeatureCard("Terminal", "Command runner", Modifier.weight(1f))
            }
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color(0xFF202817)),
                shape = RoundedCornerShape(14.dp)
            ) {
                Text(
                    "This is a native UI scaffold only. OMP runtime execution is not connected yet.",
                    Modifier.padding(14.dp), color = Color(0xFFB7F397), fontSize = 12.sp
                )
            }
        }
    }
}

@Composable
private fun FeatureCard(title: String, subtitle: String, modifier: Modifier = Modifier) {
    Card(modifier, colors = CardDefaults.cardColors(containerColor = Color(0xFF151923)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = Color(0xFF9299A8), fontSize = 11.sp)
        }
    }
}
