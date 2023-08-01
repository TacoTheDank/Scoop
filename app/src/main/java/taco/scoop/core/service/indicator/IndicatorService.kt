package taco.scoop.core.service.indicator

import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.os.IBinder
import androidx.core.app.NotificationCompat
import taco.scoop.R
import taco.scoop.core.receiver.StopReceiver
import taco.scoop.util.getCompatColor
import taco.scoop.util.setPendingIntentFlag

open class IndicatorService : Service() {

    override fun onBind(intent: Intent): IBinder {
        TODO("unsupported.")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        return START_STICKY
    }

    override fun onCreate() {
        val stopPendingIntent = PendingIntent.getBroadcast(
            this,
            0,
            Intent(this, StopReceiver::class.java),
            setPendingIntentFlag()
        )
        val stopAction =
            NotificationCompat.Action(0, getString(R.string.action_kill), stopPendingIntent)

        startForeground(
            100, NotificationCompat.Builder(this, "status")
                .setSmallIcon(R.drawable.ic_bug_report)
                .setContentTitle(getString(R.string.scoop_running))
                .setColor(getCompatColor(R.color.colorAccent))
                .setShowWhen(false)
                .setPriority(NotificationCompat.PRIORITY_MIN)
                .setOngoing(true)
                .addAction(stopAction)
                .build()
        )
    }
}
