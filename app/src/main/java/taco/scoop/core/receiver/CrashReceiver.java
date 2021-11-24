package taco.scoop.core.receiver;

import static taco.scoop.util.Utils.setPendingIntentFlag;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.afollestad.inquiry.Inquiry;

import java.util.Arrays;
import java.util.Collections;

import taco.scoop.R;
import taco.scoop.ScoopApplication;
import taco.scoop.core.data.crash.Crash;
import taco.scoop.core.data.crash.CrashLoader;
import taco.scoop.ui.activity.DetailActivity;
import taco.scoop.ui.activity.MainActivity;
import taco.scoop.util.Intents;
import taco.scoop.util.PreferenceHelper;
import taco.scoop.util.Utils;

public class CrashReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent broadcastIntent) {

        if (!broadcastIntent.getAction().equals(Intents.INTENT_ACTION)) return;

        String packageName = broadcastIntent.getStringExtra(Intents.INTENT_PACKAGE_NAME);
        long time = broadcastIntent.getLongExtra(Intents.INTENT_TIME, System.currentTimeMillis());
        String description = broadcastIntent.getStringExtra(Intents.INTENT_DESCRIPTION);
        String stackTrace = broadcastIntent.getStringExtra(Intents.INTENT_STACKTRACE);

        boolean update = broadcastIntent.getBooleanExtra(Intents.INTENT_UPDATE, false);

        if (description.startsWith(ThreadDeath.class.getName()) && PreferenceHelper.ignoreThreadDeath())
            return;

        Crash crash;
        if (!update) {
            crash = new Crash(time, packageName, description, stackTrace);

            Inquiry.newInstance(context, "crashes")
                    .instanceName("receiver")
                    .build();

            Inquiry.get("receiver")
                    .insert(Crash.class)
                    .values(Collections.singletonList(crash))
                    .run();

            Inquiry.destroy("receiver");

            MainActivity.requestUpdate(crash);
        } else {
            crash = broadcastIntent.getParcelableExtra("crash");
        }

        if (PreferenceHelper.showNotifications() &&
                !Arrays.asList(PreferenceHelper.getBlacklistedPackages().split(",")).contains(packageName)) {
            NotificationManager manager = ContextCompat.getSystemService(context, NotificationManager.class);

            Intent clickIntent = new Intent(context, DetailActivity.class).putExtra(DetailActivity.EXTRA_CRASH, crash);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                    .addParentStack(DetailActivity.class)
                    .addNextIntent(clickIntent);
            PendingIntent clickPendingIntent = stackBuilder.getPendingIntent(0, setPendingIntentFlag());

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "crashes")
                    .setSmallIcon(R.drawable.ic_bug_report)
                    .setLargeIcon(Utils.convertToBitmap(CrashLoader.getAppIcon(context, packageName)))
                    .setContentTitle(CrashLoader.getAppName(context, packageName, false))
                    .setContentText(description)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setGroup("crashes")
                    .setContentIntent(clickPendingIntent);

            if (PreferenceHelper.showStackTraceNotifications()) {
                NotificationCompat.InboxStyle style = new NotificationCompat.InboxStyle();
                String[] traces = stackTrace.split("\n");
                for (int i = 0; i < 6 && i < traces.length; i++) { // Inbox style only shows 6 entries
                    style.addLine(traces[i]);
                }
                builder.setStyle(style);
            } else {
                builder.setStyle(new NotificationCompat.BigTextStyle().bigText(description));
            }

            int notificationId = (int) (time - ScoopApplication.getBootTime());

            if (PreferenceHelper.showActionButtons()) {
                Intent copyIntent = new Intent(context, ShareReceiver.class)
                        .putExtra("stackTrace", stackTrace)
                        .putExtra("pkg", packageName)
                        .setAction(Intents.INTENT_ACTION_COPY);
                PendingIntent copyPendingIntent = PendingIntent.getBroadcast(context,
                        notificationId, copyIntent, setPendingIntentFlag());
                builder.addAction(new NotificationCompat.Action(R.drawable.ic_content_copy,
                        context.getString(R.string.action_copy_short), copyPendingIntent));

                Intent shareIntent = new Intent(context, ShareReceiver.class)
                        .putExtra("stackTrace", stackTrace)
                        .putExtra("pkg", packageName)
                        .setAction(Intents.INTENT_ACTION_SHARE);
                PendingIntent sharePendingIntent = PendingIntent.getBroadcast(context,
                        notificationId, shareIntent, setPendingIntentFlag());
                builder.addAction(new NotificationCompat.Action(R.drawable.ic_share,
                        context.getString(R.string.action_share), sharePendingIntent));
            }

            manager.notify(notificationId, builder.build());
        }
    }
}
