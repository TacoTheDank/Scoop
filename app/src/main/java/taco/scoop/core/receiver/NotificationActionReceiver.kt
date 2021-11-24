package taco.scoop.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import taco.scoop.R
import taco.scoop.util.Intents
import taco.scoop.util.copyTextToClipboard
import taco.scoop.util.displayToast

class NotificationActionReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val stackTrace = intent?.getStringExtra("stackTrace")
        val pkg = intent?.getStringExtra("pkg")
        when (intent?.action) {
            Intents.INTENT_ACTION_COPY -> {
                context.copyTextToClipboard(R.string.copy_label, pkg, stackTrace)
                context.displayToast(R.string.copied_toast)
            }
        }
    }
}
