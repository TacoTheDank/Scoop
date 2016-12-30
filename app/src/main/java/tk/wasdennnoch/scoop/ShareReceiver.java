package tk.wasdennnoch.scoop;

import android.content.BroadcastReceiver;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

import tk.wasdennnoch.scoop.data.crash.CrashLoader;

public class ShareReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String stackTrace = intent.getStringExtra("stackTrace");
        String pkg = intent.getStringExtra("pkg");
        if (intent.getAction().equals(XposedHook.INTENT_ACTION_SHARE)) {
            Intent shareIntent = new Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, stackTrace);
            context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.action_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
        } else if (intent.getAction().equals(XposedHook.INTENT_ACTION_COPY)) {
            ((ClipboardManager) context.getSystemService(Context.CLIPBOARD_SERVICE)).setPrimaryClip(
                    ClipData.newPlainText(
                            context.getResources().getString(R.string.copy_label, CrashLoader.getAppName(context, pkg, false)),
                            stackTrace));
            Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_LONG).show();
        }
    }

}
