package taco.scoop.ui

import android.content.Intent
import android.content.SharedPreferences
import android.os.AsyncTask
import android.os.Bundle
import android.os.Handler
import android.view.Menu
import android.view.MenuItem
import android.view.View
import androidx.activity.result.contract.ActivityResultContracts.StartActivityForResult
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.preference.PreferenceManager
import com.afollestad.inquiry.Inquiry
import com.afollestad.materialcab.attached.AttachedCab
import com.afollestad.materialcab.attached.destroy
import com.afollestad.materialcab.attached.isActive
import com.afollestad.materialcab.createCab
import taco.scoop.R
import taco.scoop.ScoopApplication
import taco.scoop.ScoopApplication.Companion.serviceActive
import taco.scoop.data.crash.Crash
import taco.scoop.data.crash.CrashAdapter
import taco.scoop.data.crash.CrashLoader
import taco.scoop.databinding.ActivityMainBinding
import taco.scoop.ui.helpers.ToolbarElevationHelper
import java.util.*
import kotlin.collections.ArrayList

class MainActivity : AppCompatActivity(), CrashAdapter.Listener, SearchView.OnQueryTextListener,
    SearchView.OnCloseListener {
    private val mLoader = CrashLoader()
    private var mCombineApps = false
    private var mHasCrash = false
    private var mPrefs: SharedPreferences? = null
    private var mHandler: Handler? = null
    private var mAdapter: CrashAdapter? = null
    private var mNoItemsScreen: View? = null
    private var mCab: AttachedCab? = null

    private var mDestroyed = false
    private var mCheckPending = true
    private var mIsAvailable = true
    private var mWasLoading = false
    private val mUpdateCheckerRunnable: Runnable = object : Runnable {
        override fun run() {
            if (sUpdateRequired) {
                sUpdateRequired = false
                if (mCombineApps) // It doesn't look right when there's suddenly a single crash of
                // a different app in the list
                    return
                if (sVisible && sNewCrash != null) {
                    mAdapter!!.addCrash(sNewCrash)
                    updateViewStates(false)
                    sNewCrash = null
                } else {
                    loadData()
                }
            }
            mHandler!!.postDelayed(this, UPDATE_DELAY.toLong())
        }
    }

    private lateinit var _binding: ActivityMainBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this)
        updateLocale()

        super.onCreate(savedInstanceState)

        _binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.mainToolbar.toolbar)

        mAdapter = CrashAdapter(this, this)
        binding.mainCrashView.adapter = mAdapter
        binding.mainCrashView.isGone = true
        ToolbarElevationHelper(binding.mainCrashView, binding.mainToolbar.toolbar)

        val i = intent
        mHasCrash = i.hasExtra(EXTRA_CRASH)
        if (mHasCrash) {
            val c: Crash? = i.getParcelableExtra(EXTRA_CRASH)
            val crashes = ArrayList<Crash?>()
            crashes.add(c)
            c?.children?.let(crashes::addAll)
            mAdapter!!.setCrashes(crashes)
            supportActionBar?.title =
                CrashLoader.getAppName(this, c?.packageName, true)
            supportActionBar?.setDisplayHomeAsUpEnabled(true)
        }

        mCombineApps = mPrefs!!.getBoolean("combine_same_apps", false)
        mAdapter!!.setCombineSameApps(!mHasCrash && mCombineApps)
        binding.mainCrashView.setReverseOrder(mHasCrash || !mCombineApps)

        Inquiry.newInstance(this, "crashes")
            .instanceName("main")
            .build()

        if (savedInstanceState == null) {
            sUpdateRequired = false
            if (!mHasCrash) {
                loadData()
            } else {
                updateViewStates(false)
            }
        } else {
            mAdapter!!.restoreInstanceState(savedInstanceState)
            updateViewStates(false)
        }
        mHandler = Handler()

        AvailabilityCheck().execute()
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        mAdapter!!.saveInstanceState(outState)
    }

    override fun onResume() {
        super.onResume()
        // Cheap way to instantly apply changes
        mAdapter!!.setSearchPackageName(
            this, mPrefs!!.getBoolean("search_package_name", true)
        )
        sVisible = true
        mHandler!!.post(mUpdateCheckerRunnable)
    }

    public override fun onPause() {
        super.onPause()
        sVisible = false
        mHandler!!.removeCallbacks(mUpdateCheckerRunnable)
        if (isFinishing && !mHasCrash) {
            Inquiry.destroy("main")
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        mDestroyed = true
    }

    override fun onBackPressed() {
        if (mCab.isActive()) {
            mAdapter!!.setSelectionEnabled(false)
        } else {
            super.onBackPressed()
        }
    }

    private fun updateLocale() {
        if (mPrefs!!.getBoolean("force_english", false)) {
            // TODO: Use ConfigurationCompat
            val config = resources.configuration
            config.locale = Locale.ENGLISH
            resources.updateConfiguration(config, null)
        }
    }

    private fun updateViewStates(loading: Boolean?) {
        var newLoading = loading
        if (newLoading == null) {
            newLoading = mWasLoading
        }
        mWasLoading = newLoading
        if (mCheckPending) {
            newLoading = true
        }
        val empty = mAdapter!!.isEmpty
        binding.mainProgressbar.isVisible = newLoading
        binding.mainCrashView.isGone = newLoading || empty || !mIsAvailable

        if (!newLoading && empty && mIsAvailable) {
            if (mNoItemsScreen == null) {
                mNoItemsScreen = binding.mainNoItemsStub.inflate()
            }
            mNoItemsScreen!!.isVisible = true
        } else if (mNoItemsScreen != null) {
            mNoItemsScreen!!.isGone = true
        }
        if (!mIsAvailable) {
            if (mNoItemsScreen == null) {
                mNoItemsScreen = binding.mainNoPermissionStub.inflate()
            }
        }
    }

    private fun loadData() {
        mAdapter!!.setSelectionEnabled(false)
        updateViewStates(true)
        mLoader.loadData(
            this,
            mPrefs!!.getBoolean("combine_same_stack_trace", true),
            mPrefs!!.getBoolean("combine_same_apps", false),
            listOf(
                *mPrefs
                    ?.getString("blacklisted_packages", "")
                    ?.split(",".toRegex())!!.toTypedArray()
            )
        )
    }

    fun onDataLoaded(data: ArrayList<Crash?>?) {
        mAdapter!!.setCrashes(data)
        updateViewStates(false)
    }

    private fun setCabActive(active: Boolean) {
        if (active) {
            showCab()
        } else {
            mCab.destroy()
        }
    }

    private val activityResultLauncher = registerForActivityResult(StartActivityForResult()) {
        if (it.resultCode == RESULT_OK) {
            loadData()
        }
    }

    override fun onCrashClicked(crash: Crash) {
        if (mCombineApps && !mHasCrash) {
            activityResultLauncher.launch(
                Intent(this, MainActivity::class.java)
                    .putExtra(EXTRA_CRASH, crash)
            )
        } else {
            startActivity(
                Intent(this, DetailActivity::class.java)
                    .putExtra(DetailActivity.EXTRA_CRASH, crash)
            )
        }
    }

    override fun onToggleSelectionMode(enabled: Boolean) {
        setCabActive(enabled)
    }

    override fun onItemSelected(count: Int) {
        mCab?.apply {
            title(
                literal = String.format(
                    resources.getQuantityString(
                        R.plurals.items_selected_count,
                        count
                    ), count
                )
            )
        }
    }

    private fun showCab() {
        mCab = createCab(R.id.main_cab_stub) {
            menu(R.menu.menu_cab)
            slideDown(300)
            closeDrawable(R.drawable.ic_close)
            onSelection {
                if (it.itemId == R.id.menu_cab_delete) {
                    showDeletePrompt()
                }
                true
            }
            onDestroy {
                mAdapter!!.setSelectionEnabled(false)
                true
            }
        }
    }

    private fun showDeletePrompt() {
        val items = mAdapter!!.selectedItems
        if (items.isEmpty()) {
            return
        }
        val content = String.format(
            resources.getQuantityString(R.plurals.delete_multiple_confirm, items.size),
            items.size
        )
        AlertDialog.Builder(this@MainActivity)
            .setMessage(content)
            .setNegativeButton(android.R.string.cancel, null)
            .setPositiveButton(android.R.string.ok) { _, _ ->
                deleteItems(items)
                mAdapter!!.setSelectionEnabled(false)
                setResult(RESULT_OK) // Reload overview when going back to reflect changes
                if (mHasCrash && mAdapter!!.isEmpty) {
                    finish() // Everything deleted, go back to overview
                } else {
                    updateViewStates(false)
                }
            }
            .show()
    }

    private fun deleteItems(items: ArrayList<Crash>) {
        // TODO THIS IS A MESS
        val instance = Inquiry.get("main")
        for (c in items) {
            if (!mHasCrash && c.children != null) {
                for (cc in c.children) {
                    cc.hiddenIds?.let {
                        instance.delete(Crash::class.java)
                            .whereIn("_id", *it.toTypedArray())
                            .run()
                    }
                    mAdapter!!.removeCrash(cc)
                }
                instance.delete(Crash::class.java)
                    .values(c.children)
                    .run()
            }
            c.hiddenIds?.let {
                instance.delete(Crash::class.java)
                    .whereIn("_id", *it.toTypedArray())
                    .run()
            }
            instance.delete(Crash::class.java)
                .values(listOf(c))
                .run()
            mAdapter!!.removeCrash(c)
        }
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_main, menu)
        val searchItem = menu.findItem(R.id.menu_main_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setOnCloseListener(this)
        return true
    }

    override fun onClose(): Boolean {
        mAdapter!!.search(this, null)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        mAdapter!!.search(this, newText)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        when (item.itemId) {
            R.id.menu_main_clear -> {
                AlertDialog.Builder(this)
                    .setMessage(R.string.dialog_clear_content)
                    .setNegativeButton(android.R.string.cancel, null)
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        Inquiry.get("main")
                            .dropTable(Crash::class.java) // bam!
                        onDataLoaded(null)
                    }
                    .show()
                return true
            }
            R.id.menu_main_settings -> {
                startActivity(Intent(this, SettingsActivity::class.java))
                return true
            }
            android.R.id.home -> {
                finish()
                return true
            }
        }
        return super.onOptionsItemSelected(item)
    }

    internal inner class AvailabilityCheck : AsyncTask<Void?, Void?, Boolean>() {
        override fun doInBackground(vararg params: Void?): Boolean {
            val app = application as ScoopApplication
            return serviceActive() || app.startService()
        }

        override fun onPostExecute(available: Boolean) {
            if (!mDestroyed) {
                mCheckPending = false
                mIsAvailable = available
                updateViewStates(null)
            }
        }
    }

    companion object {
        private const val EXTRA_CRASH = "taco.scoop.EXTRA_CRASH"
        private const val UPDATE_DELAY = 200
        private var sUpdateRequired = false
        private var sVisible = false
        private var sNewCrash: Crash? = null

        @JvmStatic
        fun requestUpdate(newCrash: Crash?) {
            sUpdateRequired = true
            if (sVisible) {
                sNewCrash = newCrash
            }
        }
    }
}
