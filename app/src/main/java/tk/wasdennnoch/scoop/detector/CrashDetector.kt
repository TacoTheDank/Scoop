package tk.wasdennnoch.scoop.detector

import android.content.Intent
import android.util.Log
import eu.chainfire.librootjava.RootIPC
import tk.wasdennnoch.scoop.BuildConfig
import tk.wasdennnoch.scoop.CrashReceiver
import tk.wasdennnoch.scoop.XposedHook.*
import java.io.BufferedReader
import java.io.InputStreamReader

class CrashDetector : ICrashDetector.Stub() {

    private var _callback: ICrashDetectorCallback? = null

    private val logcatProcess: Process
    private val reader: BufferedReader

    init {
        Runtime.getRuntime().exec("logcat -c").waitFor()
        logcatProcess = Runtime.getRuntime().exec("logcat")
        reader = BufferedReader(InputStreamReader(logcatProcess.inputStream))
        val readThread = object : Thread() {

            var crashMessage: String? = null

            override fun run() {
                Log.d("CrashDetector", "thread started")
                while (true) {
                    reader.readLine()?.let { line ->
                        if (line.matches(linePattern)) {
                            crashMessage += "$line\n"
                        } else {
                            crashMessage?.let {
                                reportCrash(it.split("\n").joinToString("\n") {
                                    line -> line.replace(androidRuntimePattern, "")
                                })
                                crashMessage = null
                            }
                            if (line.matches(beginPattern)) {
                                crashMessage = "${reader.readLine()}\n${reader.readLine()}\n"
                            }
                        }
                    }
                }
            }
        }
        readThread.start()
    }

    fun reportCrash(crashMessage: String) {
        val lines = crashMessage.split("\n")
        if (lines.size < 2) return
        val packageName = processInfoPattern.find(lines[0])?.groupValues?.get(1) ?: return
        val exceptionInfo = lines[1]
        val intent = Intent(INTENT_ACTION)
                .setClassName(BuildConfig.APPLICATION_ID, CrashReceiver::class.java.name)
                .putExtra(INTENT_PACKAGE_NAME, packageName)
                .putExtra(INTENT_TIME, System.currentTimeMillis())
                .putExtra(INTENT_DESCRIPTION, exceptionInfo)
                .putExtra(INTENT_STACKTRACE, crashMessage)
        sendBroadcast(intent)
    }

    override fun kill() {
        android.os.Process.killProcess(android.os.Process.myPid())
    }

    companion object {

        private val androidRuntimePattern = Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}( )+[0-9]+( )+[0-9]+( )+E AndroidRuntime: ")
        private val beginPattern = Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}( )+[0-9]+( )+[0-9]+( )+E AndroidRuntime: FATAL EXCEPTION: main")
        private val linePattern = Regex("[0-9]{2}-[0-9]{2} [0-9]{2}:[0-9]{2}:[0-9]{2}.[0-9]{3}( )+[0-9]+( )+[0-9]+( )+E AndroidRuntime: \tat(.)*")
        private val processInfoPattern = Regex("Process: ([a-z][a-z0-9_]*(?:\\.[a-z0-9_]+)+[0-9a-z_]), PID: ([0-9]+)")

        private val reflectionClass = Class.forName("eu.chainfire.librootjava.Reflection")
        private val sendBroadcastMethod = reflectionClass.getDeclaredMethod("sendBroadcast", Intent::class.java)

        init {
            sendBroadcastMethod.isAccessible = true
        }

        @JvmStatic
        fun main(vararg args: String) {
            try {
                RootIPC(BuildConfig.APPLICATION_ID, CrashDetector(), 0, 30 * 1000, true)
            } catch (e: RootIPC.TimeoutException) {
                // a connection wasn't established
            }
        }

        fun sendBroadcast(intent: Intent) {
            sendBroadcastMethod.invoke(null, intent)
        }
    }
}
