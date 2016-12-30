package tk.wasdennnoch.scoop;

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
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.afollestad.inquiry.Inquiry;

import java.util.Arrays;

import tk.wasdennnoch.scoop.data.crash.Crash;
import tk.wasdennnoch.scoop.data.crash.CrashLoader;
import tk.wasdennnoch.scoop.ui.DetailActivity;
import tk.wasdennnoch.scoop.ui.MainActivity;

public class CrashReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent broadcastIntent) {

        if (!broadcastIntent.getAction().equals(XposedHook.INTENT_ACTION)) return;

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);

        String packageName = broadcastIntent.getStringExtra(XposedHook.INTENT_PACKAGE_NAME);
        long time = broadcastIntent.getLongExtra(XposedHook.INTENT_TIME, System.currentTimeMillis());
        Throwable throwable = (Throwable) broadcastIntent.getSerializableExtra(XposedHook.INTENT_THROWABLE);

        if (throwable instanceof ThreadDeath && prefs.getBoolean("ignore_threaddeath", true))
            return;

        String description = throwable.toString();
        String stackTrace = Log.getStackTraceString(throwable);

        Crash crash = new Crash(time, packageName, description, stackTrace);

        Inquiry.newInstance(context, "crashes")
                .instanceName("receiver")
                .build();

        Inquiry.get("receiver")
                .insert(Crash.class)
                .values(crash)
                .run();

        Inquiry.destroy("receiver");

        if (prefs.getBoolean("show_notification", true) &&
                !Arrays.asList(PreferenceManager.getDefaultSharedPreferences(context).getString("blacklisted_packages", "").split(",")).contains(packageName)) {
            NotificationManager manager = (NotificationManager) context.getSystemService(Context.NOTIFICATION_SERVICE);

            Intent clickIntent = new Intent(context, DetailActivity.class).putExtra(DetailActivity.EXTRA_CRASH, crash);
            TaskStackBuilder stackBuilder = TaskStackBuilder.create(context)
                    .addParentStack(DetailActivity.class)
                    .addNextIntent(clickIntent);
            PendingIntent clickPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

            NotificationCompat.Builder builder = new NotificationCompat.Builder(context)
                    .setSmallIcon(R.drawable.ic_bug_notification)
                    .setLargeIcon(drawableToBitmap(CrashLoader.getAppIcon(context, packageName)))
                    .setContentTitle(CrashLoader.getAppName(context, packageName, false))
                    .setContentText(description)
                    .setColor(ContextCompat.getColor(context, R.color.colorPrimary))
                    .setAutoCancel(true)
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

            if (prefs.getBoolean("show_action_buttons", true)) {
                Intent copyIntent = new Intent(context, ShareReceiver.class).putExtra("stackTrace", stackTrace).putExtra("pkg", packageName).setAction(XposedHook.INTENT_ACTION_COPY);
                PendingIntent copyPendingIntent = PendingIntent.getBroadcast(context, 0, copyIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Action copyAction = new NotificationCompat.Action(R.drawable.ic_copy_notification, context.getString(R.string.action_copy_short), copyPendingIntent);
                builder.addAction(copyAction);

                Intent shareIntent = new Intent(context, ShareReceiver.class).putExtra("stackTrace", stackTrace).putExtra("pkg", packageName).setAction(XposedHook.INTENT_ACTION_SHARE);
                PendingIntent sharePendingIntent = PendingIntent.getBroadcast(context, 0, shareIntent, PendingIntent.FLAG_UPDATE_CURRENT);
                NotificationCompat.Action shareAction = new NotificationCompat.Action(R.drawable.ic_share_notification, context.getString(R.string.action_share), sharePendingIntent);
                builder.addAction(shareAction);
            }

            manager.notify(1, builder.build());
        }

        MainActivity.requestUpdate(crash);
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
