package tk.wasdennnoch.scoop

import android.content.Context

object Utils {

    @JvmStatic
    fun getAttrColor(context: Context, attr: Int): Int {
        val ta = context.obtainStyledAttributes(intArrayOf(attr))
        val colorAccent = ta.getColor(0, 0)
        ta.recycle()
        return colorAccent
    }
}
