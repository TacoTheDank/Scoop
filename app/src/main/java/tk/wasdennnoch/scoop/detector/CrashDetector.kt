package tk.wasdennnoch.scoop.detector

import android.content.Intent
import android.util.Log
import eu.chainfire.libsuperuser.Shell
import tk.wasdennnoch.scoop.BuildConfig
import tk.wasdennnoch.scoop.CrashReceiver
import tk.wasdennnoch.scoop.XposedHook.*
import java.io.BufferedReader
import java.io.InputStreamReader

abstract class CrashDetector : ICrashDetector.Stub() {

    private val logcatProcess: Process
    private val reader: BufferedReader

    init {
        val prefix = if (Shell.SU.available()) "su -c " else ""
        Runtime.getRuntime().exec(prefix + "logcat -c").waitFor()
        logcatProcess = Runtime.getRuntime().exec(prefix + "logcat")
        reader = BufferedReader(InputStreamReader(logcatProcess.inputStream))
        val readThread = object : Thread() {

            var pendingLine: String? = null

            override fun run() {
                Log.d("CrashDetector", "thread started")
                while (true) {
                    (pendingLine ?: reader.readLine())?.let { line ->
                        pendingLine = null
                        if (line.matches(fePattern)) {
                            pendingLine = reportCrash(reader)
                        }
                    } ?: break
                }
            }
        }
        readThread.start()
    }

    private fun reportCrash(reader: BufferedReader): String? {
        val lines = ArrayList<String>()
        var index = 0
        var lastLine: String?
        var foundTrace = false
        var foundTraceAt = 0
        while (true) {
            val line = reader.readLine()
            lastLine = line
            if (line != null && line.matches(beginPattern)) {
                if (!foundTrace && line.matches(linePattern)) {
                    foundTrace = true
                    foundTraceAt = index
                }
                lines.add(line.replace(arPattern, ""))
            } else {
                break
            }
            index++
        }
        if (lines.size < 2) return lastLine
        val packageName = processInfoPattern.find(lines[0])?.groupValues?.get(1) ?: return lastLine
        val intent = Intent(INTENT_ACTION)
                .setClassName(BuildConfig.APPLICATION_ID, CrashReceiver::class.java.name)
                .putExtra(INTENT_PACKAGE_NAME, packageName)
                .putExtra(INTENT_TIME, System.currentTimeMillis())
                .putExtra(INTENT_DESCRIPTION, lines.subList(1, foundTraceAt).joinToString("\n"))
                .putExtra(INTENT_STACKTRACE, lines.joinToString("\n"))
        sendBroadcast(intent)
        return lastLine
    }

    protected abstract fun sendBroadcast(intent: Intent)

    override fun kill() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    companion object {

        private val beginPattern = Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}( )+[0-9]+( )+[0-9]+( )+E AndroidRuntime: (.)*")
        private val arPattern = Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}( )+[0-9]+( )+[0-9]+( )+E AndroidRuntime: ")
        private val fePattern = Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}( )+[0-9]+( )+[0-9]+( )+E AndroidRuntime: FATAL EXCEPTION: main")
        private val linePattern = Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}( )+[0-9]+( )+[0-9]+( )+E AndroidRuntime: \tat(.)*")
        private val processInfoPattern = Regex("Process: ([a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)+[0-9a-z_]), PID: ([0-9]+)")
    }
}
