package tk.wasdennnoch.scoop.detector

import android.content.Intent
import eu.chainfire.librootjava.RootIPC
import tk.wasdennnoch.scoop.BuildConfig

class CrashDetectorRoot : CrashDetector() {

    override fun sendBroadcast(intent: Intent) {
        sendBroadcastMethod.invoke(null, intent)
    }

    companion object {

        private val reflectionClass = Class.forName("eu.chainfire.librootjava.Reflection")
        private val sendBroadcastMethod = reflectionClass.getDeclaredMethod("sendBroadcast", Intent::class.java)

        init {
            sendBroadcastMethod.isAccessible = true
        }

        @JvmStatic
        fun main(vararg args: String) {
            try {
                RootIPC(BuildConfig.APPLICATION_ID, CrashDetectorRoot(), 0, 30 * 1000, true)
            } catch (e: RootIPC.TimeoutException) {
                // a connection wasn't established
            }
        }
    }
}
