package tk.wasdennnoch.scoop;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.preference.PreferenceManager;

import androidx.core.app.NotificationCompat;
import androidx.core.app.TaskStackBuilder;
import androidx.core.content.ContextCompat;

import com.afollestad.inquiry.Inquiry;

import java.util.Arrays;
import java.util.Collections;

import tk.wasdennnoch.scoop.data.crash.Crash;
import tk.wasdennnoch.scoop.data.crash.CrashLoader;
import tk.wasdennnoch.scoop.dogbin.DogbinUploadService;
import tk.wasdennnoch.scoop.ui.DetailActivity;
import tk.wasdennnoch.scoop.ui.MainActivity;

public class CrashReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent broadcastIntent) {

        if (!broadcastIntent.getAction().equals(XposedHook.INTENT_ACTION)) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String packageName = broadcastIntent.getStringExtra(XposedHook.INTENT_PACKAGE_NAME);
        long time = broadcastIntent.getLongExtra(XposedHook.INTENT_TIME, System.currentTimeMillis());
        String description = broadcastIntent.getStringExtra(XposedHook.INTENT_DESCRIPTION);
        String stackTrace = broadcastIntent.getStringExtra(XposedHook.INTENT_STACKTRACE);

        boolean update = broadcastIntent.getBooleanExtra(XposedHook.INTENT_UPDATE, false);
        boolean hideUpload = broadcastIntent.getBooleanExtra(XposedHook.INTENT_HIDE_UPLOAD, false);
        boolean uploadError = broadcastIntent.getBooleanExtra(XposedHook.INTENT_UPLOAD_ERROR, false);
        String dogbinLink = broadcastIntent.getStringExtra(XposedHook.INTENT_DOGBIN_LINK);

        if (description.startsWith(ThreadDeath.class.getName()) && prefs.getBoolean("ignore_threaddeath", true))
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

        if (prefs.getBoolean("show_notification", true) &&
                !Arrays.asList(PreferenceManager.getDefaultSharedPreferences(context).getString("blacklisted_packages", "").split(",")).contains(packageName)) {
            NotificationManager manager = ContextCompat.getSystemService(context, NotificationManager.class);

            Intent clickIntent = new Intent(context, DetailActivity.class).putExtra(DetailActivity.EXTRA_CRASH, crash);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                    .addParentStack(DetailActivity.class)
                    .addNextIntent(clickIntent);
            PendingIntent clickPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context, "crashes")
                    .setSmallIcon(R.drawable.ic_bug_report)
                    .setLargeIcon(drawableToBitmap(CrashLoader.getAppIcon(context, packageName)))
                    .setContentTitle(CrashLoader.getAppName(context, packageName, false))
                    .setContentText(description)
                    .setPriority(NotificationCompat.PRIORITY_MAX)
                    .setDefaults(Notification.DEFAULT_VIBRATE)
                    .setColor(ContextCompat.getColor(context, R.color.colorAccent))
                    .setAutoCancel(true)
                    .setOnlyAlertOnce(true)
                    .setGroup("crashes")
                    .setContentIntent(clickPendingIntent);

            if (prefs.getBoolean("show_stack_trace_notif", false)) {
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

            if (prefs.getBoolean("show_action_buttons", true)) {
                Intent copyIntent = new Intent(context, ShareReceiver.class)
                        .putExtra("stackTrace", stackTrace)
                        .putExtra("pkg", packageName)
                        .setAction(XposedHook.INTENT_ACTION_COPY);
                PendingIntent copyPendingIntent = PendingIntent.getBroadcast(context,
                        notificationId, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(new NotificationCompat.Action(R.drawable.ic_content_copy,
                        context.getString(R.string.action_copy_short), copyPendingIntent));

                Intent shareIntent = new Intent(context, ShareReceiver.class)
                        .putExtra("stackTrace", stackTrace)
                        .putExtra("pkg", packageName)
                        .setAction(XposedHook.INTENT_ACTION_SHARE);
                PendingIntent sharePendingIntent = PendingIntent.getBroadcast(context,
                        notificationId, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                builder.addAction(new NotificationCompat.Action(R.drawable.ic_share,
                        context.getString(R.string.action_share), sharePendingIntent));

                if (dogbinLink != null) {

                    Intent copyLinkIntent = new Intent(context, ShareReceiver.class)
                            .putExtra("pkg", packageName)
                            .putExtra(XposedHook.INTENT_DOGBIN_LINK, dogbinLink)
                            .setAction(XposedHook.INTENT_ACTION_COPY_LINK);
                    PendingIntent copyLinkPendingIntent = PendingIntent.getBroadcast(context,
                            notificationId, copyLinkIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.addAction(new NotificationCompat.Action(0,
                            context.getString(R.string.action_dogbin_copy_link), copyLinkPendingIntent));

                } else if (!hideUpload) {

                    int uploadTitle = uploadError ? R.string.action_dogbin_upload_error : R.string.action_dogbin_upload;
                    Intent dogbinIntent = new Intent(context, DogbinUploadService.class)
                            .putExtra("data", broadcastIntent)
                            .putExtra("crash", crash);
                    PendingIntent dogbinPendingIntent = PendingIntent.getService(context,
                            notificationId, dogbinIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                    builder.addAction(new NotificationCompat.Action(0,
                            context.getString(uploadTitle), dogbinPendingIntent));

                } else {
                    builder.setProgress(0, 0, true);
                    builder.setOngoing(true);
                }
            }

            manager.notify(notificationId, builder.build());
        }
    }

    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable)
            return ((BitmapDrawable) drawable).getBitmap();
        Bitmap bitmap = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        return bitmap;
    }

}
