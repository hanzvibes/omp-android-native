package sh.omp.android

import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile

private const val WORKSPACE_PREFS = "omp_workspace"
private const val WORKSPACE_URI_KEY = "tree_uri"
private const val WORKSPACE_NAME_KEY = "tree_name"

@Composable
fun WorkspaceManager() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(WORKSPACE_PREFS, Context.MODE_PRIVATE) }
    var workspaceUri by remember { mutableStateOf(prefs.getString(WORKSPACE_URI_KEY, null)) }
    var workspaceName by remember { mutableStateOf(prefs.getString(WORKSPACE_NAME_KEY, null)) }
    var error by remember { mutableStateOf<String?>(null) }

    val folderPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val granted = persistFolderAccess(context, uri)
            if (granted) {
                val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "Selected workspace"
                prefs.edit()
                    .putString(WORKSPACE_URI_KEY, uri.toString())
                    .putString(WORKSPACE_NAME_KEY, name)
                    .apply()
                workspaceUri = uri.toString()
                workspaceName = name
                error = null
            } else {
                error = "Android tidak memberikan izin akses permanen ke folder ini. Coba pilih folder lain."
            }
        }
    }

    val folderAvailable = remember(workspaceUri) {
        workspaceUri?.let { raw ->
            runCatching { DocumentFile.fromTreeUri(context, Uri.parse(raw))?.exists() == true }
                .getOrDefault(false)
        } ?: false
    }

    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFF151923)),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            Modifier.fillMaxWidth().padding(15.dp),
            verticalArrangement = Arrangement.spacedBy(9.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WORKSPACE", color = Color(0xFF9299A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (folderAvailable) "  ACCESS SAVED" else "  NOT CONNECTED",
                    color = if (folderAvailable) Color(0xFFB7F397) else Color(0xFFFFC66D),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold
                )
            }

            Text(
                workspaceName ?: "No workspace selected",
                color = Color(0xFFE8EBF0),
                fontWeight = FontWeight.SemiBold
            )
            Text(
                when {
                    folderAvailable -> "OMP Android can access this folder through Android's document provider. Termux sync is not connected yet."
                    workspaceUri != null -> "Saved folder is unavailable. Select it again to restore access."
                    else -> "Choose a project folder. Access is stored across app restarts."
                },
                color = Color(0xFF9299A8),
                fontSize = 12.sp
            )
            if (error != null) Text(error!!, color = Color(0xFFFFC66D), fontSize = 12.sp)

            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(
                    onClick = { folderPicker.launch(null) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFFB7F397),
                        contentColor = Color(0xFF10140D)
                    )
                ) { Text(if (workspaceUri == null) "Choose folder" else "Change folder") }

                if (workspaceUri != null) {
                    OutlinedButton(onClick = {
                        runCatching {
                            context.contentResolver.releasePersistableUriPermission(
                                Uri.parse(workspaceUri),
                                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
                                    android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
                            )
                        }
                        prefs.edit().remove(WORKSPACE_URI_KEY).remove(WORKSPACE_NAME_KEY).apply()
                        workspaceUri = null
                        workspaceName = null
                        error = null
                    }) { Text("Remove") }
                }
            }
        }
    }
}

private fun persistFolderAccess(context: Context, uri: Uri): Boolean {
    val flags = android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION or
        android.content.Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    return runCatching {
        context.contentResolver.takePersistableUriPermission(uri, flags)
        true
    }.getOrElse {
        runCatching {
            context.contentResolver.takePersistableUriPermission(
                uri,
                android.content.Intent.FLAG_GRANT_READ_URI_PERMISSION
            )
            true
        }.getOrDefault(false)
    }
}
