package taco.scoop.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import com.topjohnwu.superuser.Shell
import taco.scoop.BuildConfig

const val readLogsPermission = Manifest.permission.READ_LOGS

fun Context.readLogsPermissionGranted(): Boolean {
    return ContextCompat.checkSelfPermission(
        this,
        readLogsPermission
    ) == PackageManager.PERMISSION_GRANTED
}

fun Context.isPermissionGranted(tryGranting: Boolean = true): Boolean {
    val granted = readLogsPermissionGranted()
    return if (!granted && tryGranting) {
        runReadLogsGrantShell()
        isPermissionGranted(false)
    } else {
        granted
    }
}

fun runReadLogsGrantShell() {
    Shell.su(
        "pm grant ${BuildConfig.APPLICATION_ID} $readLogsPermission"
    ).exec()
}
