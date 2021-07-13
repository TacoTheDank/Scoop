package taco.scoop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import taco.scoop.util.stopScoopService

class StopReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        context.stopScoopService()
    }
}
