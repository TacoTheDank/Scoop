package taco.scoop.ui.view

import android.content.Context
import android.util.AttributeSet
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView

class CrashRecyclerView @JvmOverloads constructor(
    context: Context?,
    attrs: AttributeSet? = null,
    defStyle: Int = 0
) : RecyclerView(
    context!!, attrs, defStyle
) {
    private val mManager: LinearLayoutManager = LinearLayoutManager(context)
    fun setReverseOrder(reverse: Boolean) {
        mManager.reverseLayout = reverse
        mManager.stackFromEnd = reverse
    }

    init {
        layoutManager = mManager
    }
}
