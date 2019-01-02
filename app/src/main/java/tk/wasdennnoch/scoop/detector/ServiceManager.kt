package tk.wasdennnoch.scoop.detector

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.ServiceConnection
import android.content.pm.PackageManager
import android.os.IBinder
import androidx.core.content.ContextCompat
import eu.chainfire.libsuperuser.Shell

class ServiceManager(private val context: Context) {

    private var crashDetector: ICrashDetector? = null
    private val serviceConnection = object : ServiceConnection {

        override fun onServiceConnected(name: ComponentName, service: IBinder) {
            crashDetector = ICrashDetector.Stub.asInterface(service)
        }

        override fun onServiceDisconnected(name: ComponentName) {
            crashDetector = null
        }
    }

    fun startService(): Boolean {
        if (crashDetector != null) return true
        val suAvailable = Shell.SU.available()
        if (!suAvailable && !isPermissionGranted()) return false
        val intent = Intent(context, CrashDetectorService::class.java)
                .putExtra("rootMode", suAvailable)
        context.startService(intent)
        context.bindService(intent, serviceConnection, 0)
        return true
    }

    fun stopService() {
        context.stopService(Intent(context, CrashDetectorService::class.java))
    }

    private fun isPermissionGranted(): Boolean {
        return ContextCompat.checkSelfPermission(context,
                "android.permission.READ_LOGS") == PackageManager.PERMISSION_GRANTED
    }

}
