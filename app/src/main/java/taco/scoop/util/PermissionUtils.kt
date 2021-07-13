package taco.scoop.util

import android.content.Context

fun Context.isPermissionGranted(tryGranting: Boolean = true): Boolean {
    val granted = readLogsPermissionGranted()
    return if (!granted && tryGranting) {
        runReadLogsGrantShell()
        isPermissionGranted(false)
    } else {
        granted
    }
}
