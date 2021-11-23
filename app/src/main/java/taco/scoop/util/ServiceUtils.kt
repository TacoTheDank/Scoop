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

    ContextCompat.startForegroundService(this, serviceIntent)
    return true
}

fun Context.stopScoopService() {
    if (stopService(serviceIntent))
        isServiceActive = false
}

private val Context.serviceIntent
    get() = Intent(this, CrashDetectorService::class.java)
