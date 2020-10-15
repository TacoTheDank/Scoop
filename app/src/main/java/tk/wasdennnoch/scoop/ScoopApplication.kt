package tk.wasdennnoch.scoop

import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Handler
import android.os.HandlerThread
import android.os.SystemClock
import androidx.core.content.ContextCompat
import tk.wasdennnoch.scoop.detector.CrashDetectorService

class ScoopApplication : Application() {

    private val permissionLock = Object()

    override fun onCreate() {
        super.onCreate()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerCrashesChannel()
            registerStatusChannel()
        }

        if (!xposedActive()) {
            val thread = HandlerThread("startCrashDetector")
            thread.start()
            Handler(thread.looper).post {
                startService()
            }
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

    fun startService(): Boolean {
        if (!isPermissionGranted()) return false
        startService(Intent(this, CrashDetectorService::class.java))
        return true
    }

    fun stopService() {
        stopService(Intent(this, CrashDetectorService::class.java))
    }

    private fun isPermissionGranted(tryGranting: Boolean = true): Boolean {
        synchronized(permissionLock) {
            val permission = "android.permission.READ_LOGS"
            val granted = ContextCompat.checkSelfPermission(
                this,
                permission
            ) == PackageManager.PERMISSION_GRANTED
            return if (!granted && tryGranting) {
                Runtime.getRuntime().exec(
                    "su -c pm grant ${BuildConfig.APPLICATION_ID}" +
                            " $permission"
                ).waitFor()
                isPermissionGranted(false)
            } else {
                granted
            }
        }
    }

    companion object {

        fun xposedActive() = false

        @JvmStatic
        val bootTime = System.currentTimeMillis() - SystemClock.uptimeMillis()
    }
}
