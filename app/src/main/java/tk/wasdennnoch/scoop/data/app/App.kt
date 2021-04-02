package tk.wasdennnoch.scoop.data.app

import android.graphics.drawable.Drawable

class App internal constructor(val icon: Drawable, val name: String, val packageName: String) {
    @JvmField
    var selected = false
    override fun toString(): String {
        return "App[" +
                "; packageName " + packageName +
                "; selected " + selected +
                "]"
    }
}
