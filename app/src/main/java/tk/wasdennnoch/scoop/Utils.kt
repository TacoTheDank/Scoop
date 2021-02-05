package tk.wasdennnoch.scoop

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import androidx.core.content.ContextCompat
import tk.wasdennnoch.scoop.data.crash.CrashLoader

object Utils {

    @JvmStatic
    fun getAttrColor(context: Context, attr: Int): Int {
        val ta = context.obtainStyledAttributes(intArrayOf(attr))
        val colorAccent = ta.getColor(0, 0)
        ta.recycle()
        return colorAccent
    }

    /**
     * Copies the text to the clipboard
     *
     * @param text The text to place in the clipboard
     */
    @JvmStatic
    private fun copyToClipboard(context: Context, label: CharSequence, text: CharSequence) {
        val clipboard: ClipboardManager =
            ContextCompat.getSystemService(context, ClipboardManager::class.java)!!
        val data = ClipData.newPlainText(label, text)
        clipboard.primaryClip = data
    }

    @JvmStatic
    fun copyTextToClipboard(context: Context, label: Int, pkg: String, str: String) {
        copyToClipboard(
            context,
            context.resources.getString(label, CrashLoader.getAppName(context, pkg, false)),
            str
        )
    }
}
