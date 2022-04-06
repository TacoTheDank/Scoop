package taco.scoop.core.receiver

import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.app.TaskStackBuilder
import com.afollestad.inquiry.Inquiry
import taco.scoop.R
import taco.scoop.ScoopApplication.Companion.bootTime
import taco.scoop.core.data.crash.Crash
import taco.scoop.core.data.crash.CrashLoader
import taco.scoop.ui.activity.DetailActivity
import taco.scoop.ui.activity.MainActivity
import taco.scoop.util.Intents
import taco.scoop.util.PreferenceHelper.blacklistList
import taco.scoop.util.PreferenceHelper.ignoreThreadDeath
import taco.scoop.util.PreferenceHelper.showActionButtons
import taco.scoop.util.PreferenceHelper.showNotifications
import taco.scoop.util.PreferenceHelper.showStackTraceNotifications
import taco.scoop.util.convertToBitmap
import taco.scoop.util.getCompatColor
import taco.scoop.util.setPendingIntentFlag

class CrashReceiver : BroadcastReceiver() {

    private var packageName: String? = null
    private var description: String? = null
    private var stackTrace: String? = null

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intents.INTENT_ACTION)
            return

        description = intent.getStringExtra(Intents.INTENT_DESCRIPTION)!!
        if (description!!.startsWith(ThreadDeath::class.java.name) && ignoreThreadDeath)
            return

        packageName = intent.getStringExtra(Intents.INTENT_PACKAGE_NAME)
        val time = intent.getLongExtra(Intents.INTENT_TIME, System.currentTimeMillis())
        stackTrace = intent.getStringExtra(Intents.INTENT_STACKTRACE)

        if (blacklistList.contains(packageName)) {
            return
        }

        val crash: Crash?
        val update = intent.getBooleanExtra(Intents.INTENT_UPDATE, false)
        if (!update) {
            crash = Crash(time, packageName, description, stackTrace)
            updateCrashDatabase(context, crash)
            MainActivity.requestUpdate(crash)
        } else {
            crash = intent.getParcelableExtra("crash")
        }

        if (!showNotifications) {
            return
        }

        val clickIntent = Intent(context, DetailActivity::class.java)
            .putExtra(DetailActivity.EXTRA_CRASH, crash)
        val stackBuilder = TaskStackBuilder.create(context)
            .addParentStack(DetailActivity::class.java)
            .addNextIntent(clickIntent)
        val clickPendingIntent = stackBuilder.getPendingIntent(0, setPendingIntentFlag())

        val builder = NotificationCompat.Builder(context, "crashes").apply {
            setSmallIcon(R.drawable.ic_bug_report)
            setLargeIcon(CrashLoader.getAppIcon(context, packageName).convertToBitmap())
            setContentTitle(CrashLoader.getAppName(context, packageName, false))
            setContentText(description)
            setPriority(NotificationCompat.PRIORITY_MAX)
            setDefaults(NotificationCompat.DEFAULT_VIBRATE)
            setColor(context.getCompatColor(R.color.colorSecondary))
            setAutoCancel(true)
            setOnlyAlertOnce(true)
            setForegroundServiceBehavior(NotificationCompat.FOREGROUND_SERVICE_IMMEDIATE)
            setGroup("crashes")
            setContentIntent(clickPendingIntent)
            setNotificationStyle()
        }

        val notificationId = (time - bootTime).toInt()
        if (showActionButtons) {
            builder.addActionButtons(context, notificationId)
        }

        val manager = NotificationManagerCompat.from(context)
        manager.notify(notificationId, builder.build())
    }

    private fun updateCrashDatabase(context: Context, crash: Crash?) {
        Inquiry.newInstance(context, "crashes")
            .instanceName("receiver")
            .build()
        Inquiry.get("receiver")
            .insert(Crash::class.java)
            .values(listOf(crash))
            .run()
        Inquiry.destroy("receiver")
    }

    private fun NotificationCompat.Builder.setNotificationStyle(): NotificationCompat.Builder {
        return if (showStackTraceNotifications) {
            val style = NotificationCompat.InboxStyle()
            val traces = stackTrace!!.split("\n".toRegex()).toTypedArray()
            var i = 0
            while (i < 6 && i < traces.size) {
                // Inbox style only shows 6 entries
                style.addLine(traces[i])
                i++
            }
            setStyle(style)
        } else {
            setStyle(NotificationCompat.BigTextStyle().bigText(description))
        }
    }

    private fun NotificationCompat.Builder.addActionButtons(
        context: Context,
        notificationId: Int
    ): NotificationCompat.Builder {
        val copyIntent = Intent(context, NotificationActionReceiver::class.java)
            .putExtra("stackTrace", stackTrace)
            .putExtra("pkg", packageName)
            .setAction(Intents.INTENT_ACTION_COPY)
        val copyPendingIntent = PendingIntent.getBroadcast(
            context,
            notificationId, copyIntent, setPendingIntentFlag()
        )
        addAction(
            NotificationCompat.Action(
                R.drawable.ic_content_copy,
                context.getString(R.string.action_copy_short), copyPendingIntent
            )
        )

        val shareIntent = Intent(Intent.ACTION_SEND)
            .putExtra("stackTrace", stackTrace)
            .putExtra("pkg", packageName)
            .setType("text/plain")
            .putExtra(Intent.EXTRA_TEXT, stackTrace)
        val shareChooserIntent = Intent.createChooser(shareIntent, null)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        val sharePendingIntent = PendingIntent.getActivity(
            context,
            notificationId, shareChooserIntent, setPendingIntentFlag()
        )
        addAction(
            NotificationCompat.Action(
                R.drawable.ic_share,
                context.getString(R.string.action_share), sharePendingIntent
            )
        )
        return this
    }
}
