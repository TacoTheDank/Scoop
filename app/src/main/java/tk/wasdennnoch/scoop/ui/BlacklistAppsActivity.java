package tk.wasdennnoch.scoop.ui;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import androidx.core.view.MenuItemCompat;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;

import java.util.ArrayList;
import java.util.Arrays;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.ToolbarElevationHelper;
import tk.wasdennnoch.scoop.data.app.App;
import tk.wasdennnoch.scoop.data.app.AppAdapter;
import tk.wasdennnoch.scoop.data.app.AppLoader;

public class BlacklistAppsActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    private SharedPreferences mPrefs;
    private RecyclerView mList;
    private ProgressBar mLoading;
    private AppAdapter mAdapter;
    private boolean mIsLoading;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_blacklist_apps);

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mList = (RecyclerView) findViewById(R.id.list);
        mList.setLayoutManager(new LinearLayoutManager(this));
        mLoading = (ProgressBar) findViewById(R.id.loading);

        if (savedInstanceState != null) {
            mIsLoading = savedInstanceState.getBoolean("mIsLoading");
        }

        mAdapter = new AppAdapter();
        mList.setAdapter(mAdapter);

        new ToolbarElevationHelper(mList, toolbar);
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
    protected void onSaveInstanceState(Bundle outState) {
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
        mLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        mList.setVisibility(loading || empty ? View.GONE : View.VISIBLE);
    }

    public void onDataLoaded(ArrayList<App> apps) {
        mAdapter.setApps(apps, Arrays.asList(mPrefs.getString("blacklisted_packages", "").split(",")));
        updateViewStates(false);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_blacklist, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
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
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

}
