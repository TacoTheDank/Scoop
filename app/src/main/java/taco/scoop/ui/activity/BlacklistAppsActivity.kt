package taco.scoop.ui.activity

import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.core.view.isGone
import androidx.core.view.isVisible
import androidx.recyclerview.widget.LinearLayoutManager
import taco.scoop.R
import taco.scoop.core.data.app.App
import taco.scoop.core.data.app.AppLoader
import taco.scoop.databinding.ActivityBlacklistAppsBinding
import taco.scoop.ui.adapter.AppAdapter
import taco.scoop.ui.helper.ToolbarElevationHelper
import taco.scoop.util.PreferenceHelper
import taco.scoop.util.forceAppEnLanguage

class BlacklistAppsActivity : AppCompatActivity(), SearchView.OnQueryTextListener,
    SearchView.OnCloseListener {

    private var mAdapter: AppAdapter? = null
    private var mIsLoading = false

    private lateinit var _binding: ActivityBlacklistAppsBinding
    private val binding get() = _binding

    override fun onCreate(savedInstanceState: Bundle?) {
        forceAppEnLanguage()

        super.onCreate(savedInstanceState)

        _binding = ActivityBlacklistAppsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.blacklistToolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.blacklistView.layoutManager = LinearLayoutManager(this)

        savedInstanceState?.let {
            mIsLoading = it.getBoolean("mIsLoading")
        }

        mAdapter = AppAdapter()
        binding.blacklistView.adapter = mAdapter

        ToolbarElevationHelper(binding.blacklistView, binding.blacklistToolbar.toolbar)
    }

    override fun onResume() {
        super.onResume()
        if (!mIsLoading) {
            AppLoader().loadData(this)
            updateViewStates(true)
        }
    }

    override fun onSaveInstanceState(outState: Bundle) {
        super.onSaveInstanceState(outState)
        outState.putBoolean("mIsLoading", mIsLoading)
    }

    override fun onPause() {
        PreferenceHelper.editBlacklistPackages(mAdapter!!.selectedPackages)
        super.onPause()
    }

    private fun updateViewStates(loading: Boolean) {
        mIsLoading = loading
        val empty = mAdapter!!.isEmpty
        // When one is visible, the other isn't (and vice versa)
        binding.blacklistProgressbar.isVisible = loading
        binding.blacklistView.isGone = loading || empty
    }

    fun onDataLoaded(apps: ArrayList<App?>?) {
        mAdapter!!.setApps(apps, PreferenceHelper.blacklistList)
        updateViewStates(false)
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_blacklist, menu)
        val searchItem = menu.findItem(R.id.menu_blacklist_search)
        val searchView = searchItem.actionView as SearchView
        searchView.setOnQueryTextListener(this)
        searchView.setOnCloseListener(this)
        return true
    }

    override fun onClose(): Boolean {
        mAdapter!!.search(null)
        return false
    }

    override fun onQueryTextChange(newText: String): Boolean {
        mAdapter!!.search(newText)
        return true
    }

    override fun onQueryTextSubmit(query: String): Boolean {
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
