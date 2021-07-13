package taco.scoop

import android.annotation.TargetApi
import android.app.Application
import android.app.NotificationChannel
import android.app.NotificationManager
import android.os.Build
import android.os.SystemClock
import androidx.core.content.getSystemService
import taco.scoop.util.PreferenceHelper.initPreferences

class ScoopApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initPreferences()

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            registerCrashesChannel()
            registerStatusChannel()
        }
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun registerCrashesChannel() {
        val name = getString(R.string.crash_channel)
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel("crashes", name, importance)
        channel.createNotifChannel()
    }

    @TargetApi(Build.VERSION_CODES.O)
    private fun registerStatusChannel() {
        val name = getString(R.string.status_channel)
        val importance = NotificationManager.IMPORTANCE_MIN
        val channel = NotificationChannel("status", name, importance)
        channel.setShowBadge(false)
        channel.createNotifChannel()
    }

    @TargetApi(Build.VERSION_CODES.O)
    fun NotificationChannel.createNotifChannel() {
        val notificationManager = getSystemService<NotificationManager>()
        notificationManager?.createNotificationChannel(this)
    }

    companion object {

        @JvmStatic
        val bootTime = System.currentTimeMillis() - SystemClock.uptimeMillis()
    }
}
