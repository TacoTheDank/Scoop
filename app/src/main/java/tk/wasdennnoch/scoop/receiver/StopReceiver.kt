package tk.wasdennnoch.scoop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import tk.wasdennnoch.scoop.ScoopApplication

class StopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val app = context.applicationContext as ScoopApplication
        app.stopService()
    }
}
