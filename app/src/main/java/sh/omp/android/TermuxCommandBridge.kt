package sh.omp.android

import android.app.IntentService
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import java.util.concurrent.atomic.AtomicInteger

/**
 * Sends one fixed, read-only OMP diagnostic through Termux RUN_COMMAND.
 * No arbitrary shell input is accepted from UI or external intents.
 */
object TermuxCommandBridge {
    private const val TERMUX_PACKAGE = "com.termux"
    private const val TERMUX_SERVICE = "com.termux.app.RunCommandService"
    private const val ACTION_RUN_COMMAND = "com.termux.RUN_COMMAND"
    private const val EXTRA_COMMAND_PATH = "com.termux.RUN_COMMAND_PATH"
    private const val EXTRA_ARGUMENTS = "com.termux.RUN_COMMAND_ARGUMENTS"
    private const val EXTRA_WORKDIR = "com.termux.RUN_COMMAND_WORKDIR"
    private const val EXTRA_BACKGROUND = "com.termux.RUN_COMMAND_BACKGROUND"
    private const val EXTRA_PENDING_INTENT = "com.termux.RUN_COMMAND_PENDING_INTENT"
    private const val EXTRA_COMMAND_LABEL = "com.termux.RUN_COMMAND_COMMAND_LABEL"
    private const val EXTRA_COMMAND_DESCRIPTION = "com.termux.RUN_COMMAND_COMMAND_DESCRIPTION"

    private val nextRequestCode = AtomicInteger(1000)

    fun checkOmp(context: Context): String? {
        if (!isTermuxInstalled(context)) return "Termux is not installed."
        if (context.checkSelfPermission("com.termux.permission.RUN_COMMAND") != android.content.pm.PackageManager.PERMISSION_GRANTED) {
            return "Permission RUN_COMMAND belum diberikan. Buka App permissions dan izinkan OMP Android."
        }

        val callbackIntent = Intent(context, TermuxResultService::class.java)
        val requestCode = nextRequestCode.incrementAndGet()
        val pendingFlags = PendingIntent.FLAG_ONE_SHOT or
            (if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) PendingIntent.FLAG_MUTABLE else 0)
        val callback = PendingIntent.getService(context, requestCode, callbackIntent, pendingFlags)

        val command = Intent(ACTION_RUN_COMMAND).apply {
            setClassName(TERMUX_PACKAGE, TERMUX_SERVICE)
            putExtra(EXTRA_COMMAND_PATH, "/data/data/com.termux/files/usr/bin/bash")
            putExtra(EXTRA_ARGUMENTS, arrayOf("-lc", "if command -v omp >/dev/null 2>&1; then omp --version; else echo 'OMP_NOT_INSTALLED'; exit 127; fi"))
            putExtra(EXTRA_WORKDIR, "/data/data/com.termux/files/home")
            putExtra(EXTRA_BACKGROUND, true)
            putExtra(EXTRA_PENDING_INTENT, callback)
            putExtra(EXTRA_COMMAND_LABEL, "OMP runtime diagnostic")
            putExtra(EXTRA_COMMAND_DESCRIPTION, "Checks whether OMP is installed and returns its version.")
        }

        return try {
            context.startService(command)
            null
        } catch (error: SecurityException) {
            "Android denied Termux command access. Check RUN_COMMAND permission and allow-external-apps."
        } catch (error: Exception) {
            "Could not contact Termux: ${error.message ?: error.javaClass.simpleName}"
        }
    }

    private fun isTermuxInstalled(context: Context): Boolean = runCatching {
        @Suppress("DEPRECATION")
        context.packageManager.getPackageInfo(TERMUX_PACKAGE, 0)
        true
    }.getOrDefault(false)
}

class TermuxResultService : IntentService("TermuxResultService") {
    override fun onHandleIntent(intent: Intent?) {
        if (intent == null) return

        val result = intent.getBundleExtra(EXTRA_RESULT_BUNDLE)
        val prefs = getSharedPreferences(PREFS, Context.MODE_PRIVATE)

        if (result == null) {
            prefs.edit()
                .putString("status", "No result")
                .putString("detail", "Termux returned no result bundle. Check that Termux is version 0.109 or newer.")
                .putString("output", "")
                .remove("exit_code")
                .apply()
        } else {
            val stdout = result.getString(EXTRA_STDOUT).orEmpty()
            val stderr = result.getString(EXTRA_STDERR).orEmpty()
            val exitCode = result.getInt(EXTRA_EXIT_CODE, -1)
            val termuxError = result.getInt(EXTRA_ERROR, -1)
            val errorMessage = result.getString(EXTRA_ERROR_MESSAGE).orEmpty()
            val output = listOf(stdout, stderr).filter { it.isNotBlank() }.joinToString("\n").take(6000)
            val success = termuxError == -1 && exitCode == 0 && !stdout.contains("OMP_NOT_INSTALLED")

            prefs.edit()
                .putString("status", if (success) "OMP detected" else "Not ready")
                .putString(
                    "detail",
                    when {
                        termuxError != -1 -> errorMessage.ifBlank { "Termux could not execute the diagnostic." }
                        stdout.contains("OMP_NOT_INSTALLED") -> "OMP command was not found in the Termux environment."
                        exitCode != 0 -> "Diagnostic exited with code $exitCode."
                        else -> "OMP diagnostic completed."
                    }
                )
                .putString("output", output.ifBlank { errorMessage })
                .putInt("exit_code", exitCode)
                .apply()
        }

        sendBroadcast(Intent(ACTION_RESULT).setPackage(packageName))
    }

    companion object {
        const val ACTION_RESULT = "sh.omp.android.TERMUX_RESULT"
        const val PREFS = "termux_runtime_result"

        private const val EXTRA_RESULT_BUNDLE = "result"
        private const val EXTRA_STDOUT = "stdout"
        private const val EXTRA_STDERR = "stderr"
        private const val EXTRA_EXIT_CODE = "exitCode"
        private const val EXTRA_ERROR = "err"
        private const val EXTRA_ERROR_MESSAGE = "errmsg"
    }
}
