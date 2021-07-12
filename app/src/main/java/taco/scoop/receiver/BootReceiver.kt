package taco.scoop.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log

class BootReceiver : BroadcastReceiver() {

    //Turns out android automatically handles application start
    //when it receives a broadcast. Because of this, ScoopApplication
    //starts by itself, hence, we don't need to do anything here.
    override fun onReceive(context: Context?, intent: Intent?) {
        if (intent?.action != Intent.ACTION_BOOT_COMPLETED) return

        Log.d("Scoop", "Started application on boot")
    }
}
