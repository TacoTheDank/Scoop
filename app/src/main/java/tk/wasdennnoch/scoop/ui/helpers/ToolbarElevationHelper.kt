package tk.wasdennnoch.scoop.ui.helpers

import android.os.Build
import android.view.View

class ToolbarElevationHelper @JvmOverloads constructor(
    scrollingView: View, private val targetView: View,
    private val targetElevation: Float = targetView.elevation
) {

    private var elevated = false
        set(value) {
            if (field != value) {
                field = value
                if (value) {
                    targetView.animate()
                        .translationZ(targetElevation)
                        .setDuration(100)
                        .start()
                } else {
                    targetView.animate()
                        .translationZ(0f)
                        .setDuration(100)
                        .start()
                }
            }
        }

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            scrollingView.setOnScrollChangeListener { v, _, _, _, _ ->
                elevated = v.canScrollVertically(-1)
            }
            targetView.elevation = 0f
        }
    }
}
