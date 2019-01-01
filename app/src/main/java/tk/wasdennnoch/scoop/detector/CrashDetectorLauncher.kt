package tk.wasdennnoch.scoop.detector

import android.content.Context
import android.content.Intent
import android.os.DeadObjectException
import eu.chainfire.librootjava.RootIPCReceiver
import eu.chainfire.libsuperuser.Shell
import eu.chainfire.librootjava.RootJava
import tk.wasdennnoch.scoop.BuildConfig
import tk.wasdennnoch.scoop.IndicatorService

class CrashDetectorLauncher(private val context: Context) {

    private val processName = "${BuildConfig.APPLICATION_ID}:crashDetector"

    private val ipcReceiver = object : RootIPCReceiver<ICrashDetector>(null, 0) {
        override fun onConnect(ipc: ICrashDetector) {
            crashDetector = ipc
            context.startService(Intent(context, IndicatorService::class.java))
        }

        override fun onDisconnect(ipc: ICrashDetector) {
            context.stopService(Intent(context, IndicatorService::class.java))
            crashDetector = null
            if (manualKill) {
                manualKill = false
            } else {
                launchProcess()
            }
        }
    }

    private var manualKill = false
    private var crashDetector: ICrashDetector? = null
    private var rootShell: Shell.Interactive? = null

    init {
        ipcReceiver.setContext(context)
    }

    fun launchProcess(): Boolean {
        if (crashDetector != null) return true
        if (!Shell.SU.available()) return false
        if (rootShell?.isRunning != true) {
            rootShell = Shell.Builder()
                    .useSU()
                    .open().also { shell ->
                        shell.addCommand("killall -9 $processName")
                        shell.addCommand(RootJava.getLaunchScript(context, CrashDetectorRoot::class.java,
                                null, null, null, processName))
                        shell.addCommand("exit")
                    }
        }
        return true
    }

    fun kill() {
        manualKill = true
        try {
            crashDetector?.kill()
        } catch (e: DeadObjectException) {

        }
    }
}
