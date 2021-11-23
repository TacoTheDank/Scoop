@file:JvmName("Utils")

package taco.scoop.util

import android.app.PendingIntent
import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.graphics.Bitmap
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import android.os.Build
import android.provider.Settings
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import androidx.core.graphics.applyCanvas
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.updateBounds
import taco.scoop.core.data.crash.CrashLoader
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

val pendingIntentFlags
    get() =
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S)
            PendingIntent.FLAG_IMMUTABLE
        else
            PendingIntent.FLAG_UPDATE_CURRENT

fun Context.getAttrColor(attr: Int): Int {
    val ta = this.obtainStyledAttributes(intArrayOf(attr))
    val colorAccent = ta.getColor(0, 0)
    ta.recycle()
    return colorAccent
}

/**
 * Copies the text to the clipboard.
 *
 * @param label  User-visible label for the clip data.
 * @param text   The actual text in the clip.
 */
private fun Context.copyToClipboard(label: CharSequence?, text: CharSequence?) {
    val clipboard = this.getSystemService<ClipboardManager>()
    val data = ClipData.newPlainText(label, text)
    clipboard?.setPrimaryClip(data)
}

fun Context.copyTextToClipboard(label: Int, pkg: String?, str: String?) {
    this.copyToClipboard(
        this.getString(
            label, CrashLoader.getAppName(this, pkg, false)
        ), str
    )
}

/**
 * Displays a toast to the viewer.
 *
 * @param string  The text displayed in the toast.
 */
fun Context.displayToast(@StringRes string: Int) {
    Toast.makeText(this, string, Toast.LENGTH_SHORT).show()
}

// Borrowed from https://github.com/K1rakishou/Kuroba-Experimental
@Suppress("ReplaceSizeCheckWithIsNotEmpty", "NOTHING_TO_INLINE")
@OptIn(ExperimentalContracts::class)
inline fun CharSequence?.isNeitherNullNorEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNeitherNullNorEmpty != null)
    }

    return this != null && this.length > 0
}

fun Drawable.convertToBitmap(): Bitmap? {
    if (this is BitmapDrawable) {
        return this.bitmap
    }
    val bitmap = createBitmap(this.intrinsicWidth, this.intrinsicHeight)

    return bitmap.applyCanvas {
        updateBounds(
            right = this.width,
            bottom = this.height
        )
        draw(this)
    }
}

fun Context.openSystemNotificationSettings() {
    try {
        Intent().apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                action = Settings.ACTION_APP_NOTIFICATION_SETTINGS
                putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
            } else {
                action = "android.settings.APP_NOTIFICATION_SETTINGS"
                putExtra("app_package", packageName)
                putExtra("app_uid", applicationInfo.uid)
            }
        }.also(this::startActivity)
    } catch (e: Exception) {
        e.printStackTrace()
    }
}
