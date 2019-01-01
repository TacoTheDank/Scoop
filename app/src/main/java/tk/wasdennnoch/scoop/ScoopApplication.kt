package tk.wasdennnoch.scoop

import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import eu.chainfire.libsuperuser.Shell
import tk.wasdennnoch.scoop.detector.CrashDetectorLauncher

class ScoopApplication : Application() {

    lateinit var launcher: CrashDetectorLauncher

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerCrashesChannel()
            registerStatusChannel()
        }

        if (!xposedActive()) {
            launcher = CrashDetectorLauncher(this)
            val launcherThread = object : Thread("CrashDetectorLauncher") {
                override fun run() {
                    if (Shell.SU.available()) {
                        launcher.launchProcess()
                    }
                }
            }
            launcherThread.start()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun registerCrashesChannel() {
        val name = getString(R.string.crash_channel)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("crashes", name, importance)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun registerStatusChannel() {
        val name = getString(R.string.status_channel)
        val importance = NotificationManager.IMPORTANCE_MIN
        val channel = NotificationChannel("status", name, importance)
        channel.setShowBadge(false)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    companion object {

        fun xposedActive() = false
    }
}
