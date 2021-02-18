package tk.wasdennnoch.scoop.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.preference.PreferenceManager;
import androidx.recyclerview.widget.LinearLayoutManager;

import java.util.ArrayList;
import java.util.Arrays;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.ToolbarElevationHelper;
import tk.wasdennnoch.scoop.data.app.App;
import tk.wasdennnoch.scoop.data.app.AppAdapter;
import tk.wasdennnoch.scoop.data.app.AppLoader;
import tk.wasdennnoch.scoop.databinding.ActivityBlacklistAppsBinding;

public class BlacklistAppsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private ActivityBlacklistAppsBinding binding;
    private SharedPreferences mPrefs;
    private AppAdapter mAdapter;
    private boolean mIsLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityBlacklistAppsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        setSupportActionBar(binding.blacklistToolbar.toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        binding.blacklistView.setLayoutManager(new LinearLayoutManager(this));

        if (savedInstanceState != null) {
            mIsLoading = savedInstanceState.getBoolean("mIsLoading");
        }

        mAdapter = new AppAdapter();
        binding.blacklistView.setAdapter(mAdapter);

        new ToolbarElevationHelper(binding.blacklistView, binding.blacklistToolbar.toolbar);
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (!mIsLoading) {
            new AppLoader().loadData(this);
            updateViewStates(true);
        }
    }

    @Override
    protected void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putBoolean("mIsLoading", mIsLoading);
    }

    @Override
    protected void onPause() {
        mPrefs.edit().putString("blacklisted_packages", TextUtils.join(",", mAdapter.getSelectedPackages())).apply();
        super.onPause();
    }

    private void updateViewStates(boolean loading) {
        mIsLoading = loading;
        boolean empty = mAdapter.isEmpty();
        binding.blacklistProgressbar.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.blacklistView.setVisibility(loading || empty ? View.GONE : View.VISIBLE);
    }

    public void onDataLoaded(ArrayList<App> apps) {
        mAdapter.setApps(apps, Arrays.asList(mPrefs.getString("blacklisted_packages", "").split(",")));
        updateViewStates(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_blacklist, menu);
        MenuItem searchItem = menu.findItem(R.id.menu_blacklist_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        return true;
    }

    @Override
    public boolean onClose() {
        mAdapter.search(null);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.search(newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            finish();
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
