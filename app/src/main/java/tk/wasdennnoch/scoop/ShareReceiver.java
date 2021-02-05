package tk.wasdennnoch.scoop;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.widget.Toast;

public class ShareReceiver extends BroadcastReceiver {

    @Override
    public void onReceive(Context context, Intent intent) {
        String stackTrace = intent.getStringExtra("stackTrace");
        String pkg = intent.getStringExtra("pkg");
        switch (intent.getAction()) {
            case XposedHook.INTENT_ACTION_SHARE:
                Intent shareIntent = new Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, stackTrace);
                context.startActivity(Intent.createChooser(shareIntent, context.getString(R.string.action_share)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
                break;
            case XposedHook.INTENT_ACTION_COPY:
                Utils.copyTextToClipboard(
                        context, R.string.copy_label, pkg, stackTrace);
                Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_LONG).show();
                break;
            case XposedHook.INTENT_ACTION_COPY_LINK:
                Utils.copyTextToClipboard(
                        context, R.string.copy_link_label, pkg, intent.getStringExtra(XposedHook.INTENT_DOGBIN_LINK));
                Toast.makeText(context, R.string.copied_link_toast, Toast.LENGTH_LONG).show();
                break;
        }
    }
}
