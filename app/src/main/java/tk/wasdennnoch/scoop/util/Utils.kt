@file:JvmName("Utils")

package tk.wasdennnoch.scoop.util

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.content.getSystemService
import tk.wasdennnoch.scoop.data.crash.CrashLoader
import kotlin.contracts.ExperimentalContracts
import kotlin.contracts.contract

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
inline fun CharSequence?.isNotNullNorEmpty(): Boolean {
    contract {
        returns(true) implies (this@isNotNullNorEmpty != null)
    }

    return this != null && this.length > 0
}
