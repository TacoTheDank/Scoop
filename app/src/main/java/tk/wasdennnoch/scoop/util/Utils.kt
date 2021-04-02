@file:JvmName("Utils")

package tk.wasdennnoch.scoop.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.getSystemService
import tk.wasdennnoch.scoop.data.crash.CrashLoader

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
    val clipboard = this.getSystemService<ClipboardManager>()!!
    val data = ClipData.newPlainText(label, text)
    clipboard.setPrimaryClip(data)
}

fun Context.copyTextToClipboard(label: Int, pkg: String, str: String) {
    this.copyToClipboard(
        this.resources.getString(
            label, CrashLoader.getAppName(this, pkg, false)
        ),
        str
    )
}
