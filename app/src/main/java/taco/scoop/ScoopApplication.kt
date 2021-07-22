package taco.scoop

import android.app.Application
import android.os.SystemClock
import androidx.core.app.NotificationChannelCompat
import androidx.core.app.NotificationManagerCompat
import taco.scoop.util.PreferenceHelper.initPreferences

class ScoopApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        initPreferences(resources)
        registerChannels()
    }

    private fun registerChannels() {
        val crashesChannel =
            NotificationChannelCompat.Builder("crashes", NotificationManagerCompat.IMPORTANCE_HIGH)
                .setName(getString(R.string.crash_channel))
                .build()

        val statusChannel =
            NotificationChannelCompat.Builder("status", NotificationManagerCompat.IMPORTANCE_MIN)
                .setName(getString(R.string.status_channel))
                .setShowBadge(false)
                .build()

        val notificationManager = NotificationManagerCompat.from(this)
        val channelArray = mutableListOf(crashesChannel, statusChannel)
        notificationManager.createNotificationChannelsCompat(channelArray)
    }

    companion object {

        @JvmStatic
        val bootTime = System.currentTimeMillis() - SystemClock.uptimeMillis()
    }
}
