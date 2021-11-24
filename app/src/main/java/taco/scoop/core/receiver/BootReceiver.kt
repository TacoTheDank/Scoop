package taco.scoop.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import taco.scoop.util.PreferenceHelper
import taco.scoop.util.initScoopService

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED)
            return

        if (PreferenceHelper.autostartOnBoot) {
            context.initScoopService()

            Log.d("Scoop", "Started application on boot")
        }
    }
}
