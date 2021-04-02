package tk.wasdennnoch.scoop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.widget.Toast
import tk.wasdennnoch.scoop.R
import tk.wasdennnoch.scoop.XposedHook
import tk.wasdennnoch.scoop.util.copyTextToClipboard

class ShareReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val stackTrace = intent.getStringExtra("stackTrace")!!
        val pkg = intent.getStringExtra("pkg")!!
        when (intent.action) {
            XposedHook.INTENT_ACTION_SHARE -> {
                val shareIntent = Intent(Intent.ACTION_SEND)
                    .setType("text/plain")
                    .putExtra(Intent.EXTRA_TEXT, stackTrace)
                context.startActivity(
                    Intent.createChooser(shareIntent, context.getString(R.string.action_share))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                )
            }
            XposedHook.INTENT_ACTION_COPY -> {
                context.copyTextToClipboard(R.string.copy_label, pkg, stackTrace)
                Toast.makeText(context, R.string.copied_toast, Toast.LENGTH_LONG).show()
            }
            XposedHook.INTENT_ACTION_COPY_LINK -> {
                context.copyTextToClipboard(
                    R.string.copy_link_label,
                    pkg,
                    intent.getStringExtra(XposedHook.INTENT_DOGBIN_LINK)!!
                )
                Toast.makeText(context, R.string.copied_link_toast, Toast.LENGTH_LONG).show()
            }
        }
    }
}
