package taco.scoop.ui.activity

import android.os.Bundle
import android.text.Spannable
import android.text.style.BackgroundColorSpan
import android.util.TypedValue
import android.view.Menu
import android.view.MenuItem
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.app.ShareCompat
import androidx.core.text.toSpannable
import androidx.core.view.isGone
import androidx.core.view.isVisible
import taco.scoop.R
import taco.scoop.core.data.crash.Crash
import taco.scoop.core.data.crash.CrashLoader
import taco.scoop.databinding.ActivityDetailBinding
import taco.scoop.util.PreferenceHelper
import taco.scoop.util.copyTextToClipboard
import taco.scoop.util.displayToast
import taco.scoop.util.getCompatColor
import taco.scoop.util.isNeitherNullNorEmpty
import java.util.Locale

class DetailActivity : AppCompatActivity(), SearchView.OnQueryTextListener,
    SearchView.OnCloseListener {

    private var mCrash: Crash? = null
    private var mHighlightColor = 0
    private var mSelectionEnabled = false
    private lateinit var binding: ActivityDetailBinding

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        binding = ActivityDetailBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.detailToolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        mHighlightColor = getCompatColor(R.color.highlightColor)
        mCrash = intent.getParcelableExtraCompat(EXTRA_CRASH)
        supportActionBar?.title = CrashLoader.getAppName(this, mCrash!!.packageName, true)

        binding.detailCrashLogText.text = mCrash!!.stackTrace
        binding.detailCrashEdit.setText(mCrash!!.stackTrace)
        binding.detailCrashEdit.setTextSize(
            TypedValue.COMPLEX_UNIT_PX, binding.detailCrashLogText.textSize
        )

        binding.detailScrollView.setCropHorizontally(PreferenceHelper.autoWrap)
    }

    private fun highlightText(text: String?) {
        var newText = text
        val stackTrace = mCrash!!.stackTrace.lowercase(Locale.ENGLISH)
        val span = mCrash!!.stackTrace.toSpannable()
        if (newText.isNeitherNullNorEmpty()) {
            newText = newText.lowercase(Locale.ENGLISH) // Ignore case
            val size = newText.length
            var index = 0
            while (stackTrace.indexOf(newText, index).also { index = it } != -1) {
                span.setSpan(
                    BackgroundColorSpan(mHighlightColor),
                    index,
                    index + size,
                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE
                )
                index += size
            }
        }
        binding.detailCrashLogText.setText(span, TextView.BufferType.SPANNABLE)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_detail, menu)
        val searchItem = menu.findItem(R.id.menu_detail_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setOnCloseListener(this)
        return true
    }

    override fun onClose(): Boolean {
        highlightText(null)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        highlightText(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            android.R.id.home -> {
                finish()
                return true
            }
            R.id.menu_detail_select -> {
                mSelectionEnabled = !mSelectionEnabled
                item.setIcon(
                    if (mSelectionEnabled) {
                        R.drawable.ic_unselect
                    } else {
                        R.drawable.ic_select
                    }
                )
                // When one is visible, the other isn't (and vice versa)
                binding.detailCrashEdit.isVisible = mSelectionEnabled
                binding.detailScrollView.isGone = mSelectionEnabled
            }
            R.id.menu_detail_copy -> {
                copyTextToClipboard(
                    R.string.copy_label,
                    mCrash!!.packageName,
                    mCrash!!.stackTrace
                )
                displayToast(R.string.copied_toast)
                return true
            }
            R.id.menu_detail_share -> {
                val intent = ShareCompat.IntentBuilder(this)
                    .setType("text/plain")
                    .setText(mCrash!!.stackTrace)
                    .setChooserTitle(R.string.action_share)
                    .createChooserIntent()
                startActivity(intent)
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    companion object {
        const val EXTRA_CRASH = "crash"
    }
}
