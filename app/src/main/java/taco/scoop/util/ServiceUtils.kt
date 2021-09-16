package taco.scoop.util

import android.content.Context
import android.content.Intent
import android.os.Handler
import android.os.HandlerThread
import androidx.core.content.ContextCompat
import taco.scoop.core.service.detector.CrashDetectorService

var isServiceActive = false

fun Context.initScoopService() {

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

    val intent = Intent(this, CrashDetectorService::class.java)
    ContextCompat.startForegroundService(this, intent)
    return true
}

fun Context.stopScoopService() {
    val intent = Intent(this, CrashDetectorService::class.java)
    if (stopService(intent))
        isServiceActive = false
}
