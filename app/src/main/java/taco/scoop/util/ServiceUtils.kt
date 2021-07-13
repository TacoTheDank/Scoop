package taco.scoop.util

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import taco.scoop.detector.CrashDetectorService

lateinit var intent: Intent
var isServiceActive = false

fun Context.initScoopService() {
    if (!::intent.isInitialized) {
        intent = Intent(this, CrashDetectorService::class.java)
    }

    if (!isServiceActive) {
        val thread = HandlerThread("startCrashDetector")
        thread.start()
        Handler(thread.looper).post(::startScoopService)
    }
    isServiceActive = true
}

fun Context.startScoopService(): Boolean {
    if (!isPermissionGranted())
        return false

    ContextCompat.startForegroundService(this, intent)
    return true
}

fun Context.stopScoopService() {
    if (stopService(intent))
        isServiceActive = false
}
