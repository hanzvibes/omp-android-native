package sh.omp.android

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.DocumentsContract
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

private const val WORKSPACE_PREFS = "omp_workspace"
private const val WORKSPACE_URI_KEY = "tree_uri"
private const val WORKSPACE_NAME_KEY = "tree_name"
private const val SHARED_ROOT_URI_KEY = "shared_root_uri"
private const val SHARED_ROOT_NAME_KEY = "shared_root_name"
private const val SYNC_FOLDER = "OMPWorkspaces"

@Composable
fun WorkspaceManager() {
    val context = LocalContext.current
    val prefs = remember { context.getSharedPreferences(WORKSPACE_PREFS, Context.MODE_PRIVATE) }
    val scope = rememberCoroutineScope()
    var workspaceUri by remember { mutableStateOf(prefs.getString(WORKSPACE_URI_KEY, null)) }
    var workspaceName by remember { mutableStateOf(prefs.getString(WORKSPACE_NAME_KEY, null)) }
    var sharedRootUri by remember { mutableStateOf(prefs.getString(SHARED_ROOT_URI_KEY, null)) }
    var sharedRootName by remember { mutableStateOf(prefs.getString(SHARED_ROOT_NAME_KEY, null)) }
    var error by remember { mutableStateOf<String?>(null) }
    var syncStatus by remember { mutableStateOf<String?>(null) }
    var syncing by remember { mutableStateOf(false) }

    val sourcePicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            if (persistFolderAccess(context, uri)) {
                val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "Selected workspace"
                prefs.edit().putString(WORKSPACE_URI_KEY, uri.toString())
                    .putString(WORKSPACE_NAME_KEY, name).apply()
                workspaceUri = uri.toString()
                workspaceName = name
                error = null
                syncStatus = null
            } else error = "Android gagal menyimpan izin folder project. Coba pilih ulang."
        }
    }

    val sharedRootPicker = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocumentTree()
    ) { uri: Uri? ->
        if (uri != null) {
            val isPrimaryRoot = runCatching {
                DocumentsContract.getTreeDocumentId(uri) == "primary:"
            }.getOrDefault(false)
            if (!isPrimaryRoot) {
                error = "Pilih root Internal storage (bukan subfolder atau SD card) agar path Termux bisa dipetakan."
            } else if (persistFolderAccess(context, uri)) {
                val name = DocumentFile.fromTreeUri(context, uri)?.name ?: "Internal storage"
                prefs.edit().putString(SHARED_ROOT_URI_KEY, uri.toString())
                    .putString(SHARED_ROOT_NAME_KEY, name).apply()
                sharedRootUri = uri.toString()
                sharedRootName = name
                error = null
            } else error = "Izin akses root penyimpanan tidak bisa disimpan."
        }
    }

    val sourceAvailable = remember(workspaceUri) {
        workspaceUri?.let { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(it))?.exists() == true }.getOrDefault(false) } ?: false
    }
    val sharedRootAvailable = remember(sharedRootUri) {
        sharedRootUri?.let { runCatching { DocumentFile.fromTreeUri(context, Uri.parse(it))?.exists() == true }.getOrDefault(false) } ?: false
    }
    val safeWorkspaceName = safeDirectoryName(workspaceName ?: "workspace")
    val termuxPath = "~/storage/shared/$SYNC_FOLDER/$safeWorkspaceName"

    Card(colors = CardDefaults.cardColors(containerColor = Color(0xFF151923)), shape = RoundedCornerShape(16.dp)) {
        Column(Modifier.fillMaxWidth().padding(15.dp), verticalArrangement = Arrangement.spacedBy(9.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text("WORKSPACE", color = Color(0xFF9299A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
                Text(
                    if (sourceAvailable && sharedRootAvailable) "  READY TO SYNC" else "  SETUP REQUIRED",
                    color = if (sourceAvailable && sharedRootAvailable) Color(0xFFB7F397) else Color(0xFFFFC66D),
                    fontSize = 10.sp, fontWeight = FontWeight.Bold
                )
            }
            Text(workspaceName ?: "No project folder selected", color = Color(0xFFE8EBF0), fontWeight = FontWeight.SemiBold)
            Text(
                if (sourceAvailable) "Source folder access is saved." else "Choose the project folder that OMP should work on.",
                color = Color(0xFF9299A8), fontSize = 12.sp
            )
            Button(
                onClick = { sourcePicker.launch(null) },
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7F397), contentColor = Color(0xFF10140D))
            ) { Text(if (workspaceUri == null) "Choose project folder" else "Change project folder") }

            Text("TERMUX SHARED STORAGE", color = Color(0xFF9299A8), fontSize = 10.sp, fontWeight = FontWeight.Bold)
            Text(
                if (sharedRootAvailable) "Destination: $sharedRootName / $SYNC_FOLDER" else "Choose the root of Internal storage. Files will be copied into $SYNC_FOLDER.",
                color = Color(0xFF9299A8), fontSize = 12.sp
            )
            OutlinedButton(onClick = { sharedRootPicker.launch(null) }) {
                Text(if (sharedRootUri == null) "Choose internal storage root" else "Change sync destination")
            }

            if (error != null) Text(error!!, color = Color(0xFFFFC66D), fontSize = 12.sp)
            if (syncStatus != null) Text(syncStatus!!, color = Color(0xFFB7F397), fontSize = 12.sp)

            Button(
                onClick = {
                    val source = workspaceUri?.let(Uri::parse)
                    val destination = sharedRootUri?.let(Uri::parse)
                    if (source == null || destination == null) {
                        error = "Pilih folder project dan root Internal storage terlebih dahulu."
                    } else {
                        syncing = true
                        error = null
                        syncStatus = "Preparing workspace sync…"
                        scope.launch {
                            val result = withContext(Dispatchers.IO) {
                                runCatching { syncWorkspaceToSharedStorage(context, source, destination, safeWorkspaceName) }
                            }
                            syncing = false
                            result.onSuccess { report ->
                                syncStatus = "Synced ${report.files} files · ${formatBytes(report.bytes)}. Termux path: $termuxPath"
                            }.onFailure { failure ->
                                error = failure.message ?: "Workspace sync failed."
                                syncStatus = null
                            }
                        }
                    }
                },
                enabled = sourceAvailable && sharedRootAvailable && !syncing,
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFB7F397), contentColor = Color(0xFF10140D))
            ) { Text(if (syncing) "Syncing workspace…" else "Sync workspace to Termux") }

            OutlinedButton(onClick = {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as android.content.ClipboardManager
                clipboard.setPrimaryClip(android.content.ClipData.newPlainText("Termux workspace path", termuxPath))
                android.widget.Toast.makeText(context, "Termux path copied", android.widget.Toast.LENGTH_SHORT).show()
            }, enabled = sourceAvailable) { Text("Copy Termux workspace path") }

            Text(
                "Sync copies project files into shared storage and overwrites matching files. It does not delete files from the destination. .git, dependencies, build output, and secret env files are excluded. Run termux-setup-storage in Termux before using this path.",
                color = Color(0xFF9299A8), fontSize = 11.sp
            )

            if (workspaceUri != null || sharedRootUri != null) {
                OutlinedButton(onClick = {
                    listOf(WORKSPACE_URI_KEY, WORKSPACE_NAME_KEY, SHARED_ROOT_URI_KEY, SHARED_ROOT_NAME_KEY).forEach { key ->
                        prefs.getString(key, null)?.let { raw ->
                            releaseFolderAccess(context, Uri.parse(raw))
                        }
                    }
                    prefs.edit().remove(WORKSPACE_URI_KEY).remove(WORKSPACE_NAME_KEY)
                        .remove(SHARED_ROOT_URI_KEY).remove(SHARED_ROOT_NAME_KEY).apply()
                    workspaceUri = null
                    workspaceName = null
                    sharedRootUri = null
                    sharedRootName = null
                    syncStatus = null
                    error = null
                }) { Text("Disconnect folders") }
            }
        }
    }
}

private data class SyncReport(val files: Int, val bytes: Long)

private fun syncWorkspaceToSharedStorage(
    context: Context,
    sourceUri: Uri,
    sharedRootUri: Uri,
    workspaceName: String
): SyncReport {
    val source = DocumentFile.fromTreeUri(context, sourceUri)
        ?: error("Project folder is no longer available. Select it again.")
    val sharedRoot = DocumentFile.fromTreeUri(context, sharedRootUri)
        ?: error("Internal storage access expired. Select it again.")
    check(source.isDirectory && sharedRoot.isDirectory) { "Selected locations must both be folders." }

    val workspaceParent = sharedRoot.findFile(SYNC_FOLDER)
        ?: sharedRoot.createDirectory(SYNC_FOLDER)
        ?: error("Cannot create $SYNC_FOLDER in shared storage.")
    check(workspaceParent.isDirectory) { "$SYNC_FOLDER exists but is not a folder." }

    val destination = workspaceParent.findFile(workspaceName)
        ?: workspaceParent.createDirectory(workspaceName)
        ?: error("Cannot create workspace destination.")
    check(destination.isDirectory) { "Workspace destination exists but is not a folder." }

    val counter = SyncCounter()
    copyDirectory(context, source, destination, counter, depth = 0)
    return SyncReport(counter.files, counter.bytes)
}

private class SyncCounter(var files: Int = 0, var bytes: Long = 0L)

private fun copyDirectory(
    context: Context,
    source: DocumentFile,
    destination: DocumentFile,
    counter: SyncCounter,
    depth: Int
) {
    check(depth <= 20) { "Workspace has too many nested folders (limit: 20)." }
    source.listFiles().forEach { child ->
        val name = child.name ?: return@forEach
        if (shouldSkipWorkspaceEntry(name)) return@forEach

        if (child.isDirectory) {
            val target = destination.findFile(name) ?: destination.createDirectory(name)
                ?: error("Cannot create folder: $name")
            check(target.isDirectory) { "A file already exists where folder '$name' should be." }
            copyDirectory(context, child, target, counter, depth + 1)
        } else if (child.isFile) {
            counter.files += 1
            check(counter.files <= MAX_SYNC_FILES) { "Workspace exceeds the $MAX_SYNC_FILES-file sync limit." }
            val target = destination.findFile(name) ?: destination.createFile(
                child.type ?: "application/octet-stream", name
            ) ?: error("Cannot create file: $name")
            check(target.isFile) { "A folder already exists where file '$name' should be." }

            context.contentResolver.openInputStream(child.uri)?.use { input ->
                context.contentResolver.openOutputStream(target.uri, "wt")?.use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        counter.bytes += read
                        check(counter.bytes <= MAX_SYNC_BYTES) { "Workspace exceeds the 50 MB sync limit." }
                        output.write(buffer, 0, read)
                    }
                } ?: error("Cannot write file: $name")
            } ?: error("Cannot read file: $name")
        }
    }
}

private const val MAX_SYNC_FILES = 3000
private const val MAX_SYNC_BYTES = 50L * 1024L * 1024L

private fun shouldSkipWorkspaceEntry(name: String): Boolean {
    val lower = name.lowercase()
    val excluded = setOf(".git", "node_modules", "build", ".gradle", ".next", ".idea", "dist", ".venv", "venv", "target", ".turbo", "coverage")
    return lower in excluded || lower == ".env" || lower.startsWith(".env.") ||
        lower.endsWith(".pem") || lower.endsWith(".key") || lower == "secrets" || lower == "credentials"
}

private fun safeDirectoryName(value: String): String {
    val safe = value.trim().replace(Regex("[^A-Za-z0-9._-]+"), "-").trim('.', '-', '_')
    return safe.take(60).ifBlank { "workspace" }
}

private fun formatBytes(bytes: Long): String = when {
    bytes < 1024 -> "$bytes B"
    bytes < 1024 * 1024 -> "${bytes / 1024} KB"
    else -> "${bytes / (1024 * 1024)} MB"
}

private fun persistFolderAccess(context: Context, uri: Uri): Boolean {
    val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
    return runCatching {
        context.contentResolver.takePersistableUriPermission(uri, flags)
        true
    }.getOrElse {
        runCatching {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            true
        }.getOrDefault(false)
    }
}

private fun releaseFolderAccess(context: Context, uri: Uri) {
    runCatching {
        val grant = context.contentResolver.persistedUriPermissions.firstOrNull { it.uri == uri }
        val heldFlags = grant?.let {
            (if (it.isReadPermission) Intent.FLAG_GRANT_READ_URI_PERMISSION else 0) or
                (if (it.isWritePermission) Intent.FLAG_GRANT_WRITE_URI_PERMISSION else 0)
        } ?: 0
        if (heldFlags != 0) context.contentResolver.releasePersistableUriPermission(uri, heldFlags)
    }
}
