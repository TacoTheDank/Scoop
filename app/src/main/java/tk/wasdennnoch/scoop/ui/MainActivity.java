package tk.wasdennnoch.scoop.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Configuration;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.afollestad.inquiry.Inquiry;
import com.afollestad.materialcab.MaterialCab;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Locale;

import tk.wasdennnoch.scoop.CrashReceiver;
import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.ScoopApplication;
import tk.wasdennnoch.scoop.ToolbarElevationHelper;
import tk.wasdennnoch.scoop.Utils;
import tk.wasdennnoch.scoop.data.crash.Crash;
import tk.wasdennnoch.scoop.data.crash.CrashAdapter;
import tk.wasdennnoch.scoop.data.crash.CrashLoader;
import tk.wasdennnoch.scoop.ui.utils.AnimationUtils;
import tk.wasdennnoch.scoop.view.CrashRecyclerView;

public class MainActivity extends AppCompatActivity implements CrashAdapter.Listener,
        SearchView.OnQueryTextListener, SearchView.OnCloseListener, MaterialCab.Callback {

    private static final String EXTRA_CRASH = "tk.wasdennnoch.scoop.EXTRA_CRASH";
    private static final int CODE_CHILDREN_VIEW = 1;

    private static final int UPDATE_DELAY = 200;
    private static boolean sUpdateRequired;
    private static boolean sVisible;
    private static Crash sNewCrash;

    private boolean mCombineApps;
    private boolean mHasCrash;
    private SharedPreferences mPrefs;
    private Handler mHandler;
    private CrashAdapter mAdapter;
    private CrashLoader mLoader = new CrashLoader();
    private CrashRecyclerView mList;
    private ProgressBar mLoading;
    private ViewStub mNoItemsStub;
    private View mNoItems;

    private MaterialCab mCab;
    private boolean mAnimatingCab; // Required to properly animate the cab (otherwise it instantly hides when pressing the up button)

    private boolean mDestroyed;

    private boolean mCheckPending = true;
    private boolean mIsAvailable = true;
    private boolean mWasLoading = false;
    private final Runnable mUpdateCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            if (sUpdateRequired) {
                sUpdateRequired = false;
                if (mCombineApps)
                    return; // It doesn't look right when there's suddenly a single crash of a different app in the list
                if (sVisible && sNewCrash != null) {
                    mAdapter.addCrash(sNewCrash);
                    updateViewStates(false);
                    sNewCrash = null;
                } else {
                    loadData();
                }
            }
            mHandler.postDelayed(mUpdateCheckerRunnable, UPDATE_DELAY);
        }
    };
    private Toolbar mToolbar;
    private int mToolbarElevation;
    private boolean mWasElevated;

    public static void requestUpdate(Crash newCrash) {
        sUpdateRequired = true;
        if (sVisible) {
            sNewCrash = newCrash;
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);
        if (mPrefs.getBoolean("force_english", false)) {
            Configuration config = getResources().getConfiguration();
            //noinspection deprecation
            config.locale = Locale.ENGLISH;
            //noinspection deprecation
            getResources().updateConfiguration(config, null);
        }

        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); // To make vector drawables work as menu item drawables
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar(mToolbar = findViewById(R.id.toolbar));

        mList = (CrashRecyclerView) findViewById(R.id.list);
        mLoading = (ProgressBar) findViewById(R.id.loading);
        mNoItemsStub = (ViewStub) findViewById(R.id.noItemStub);

        mAdapter = new CrashAdapter(this, this);
        mList.setAdapter(mAdapter);
        mList.setVisibility(View.GONE);
        new ToolbarElevationHelper(mList, mToolbar);

        Intent i = getIntent();
        mHasCrash = i.hasExtra(EXTRA_CRASH);
        if (mHasCrash) {
            Crash c = i.getParcelableExtra(EXTRA_CRASH);
            ArrayList<Crash> crashes = new ArrayList<>();
            crashes.add(c);
            if (c.children != null)
                crashes.addAll(c.children);
            mAdapter.setCrashes(crashes);
            //noinspection ConstantConditions
            getSupportActionBar().setTitle(CrashLoader.getAppName(this, c.packageName, true));
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
        }

        mCombineApps = mPrefs.getBoolean("combine_same_apps", false);
        mAdapter.setCombineSameApps(!mHasCrash && mCombineApps);
        mList.setReverseOrder(mHasCrash || !mCombineApps);

        Inquiry.newInstance(this, "crashes")
                .instanceName("main")
                .build();

        if (savedInstanceState == null) {
            sUpdateRequired = false;
            mCab = new MaterialCab(this, R.id.cab_stub);
            if (!mHasCrash)
                loadData();
            else
                updateViewStates(false);
        } else {
            mAdapter.restoreInstanceState(savedInstanceState);
            mCab = MaterialCab.restoreState(savedInstanceState, this, this);
            updateViewStates(false);
        }
        mHandler = new Handler();

        //noinspection ConstantConditions,ConstantIfStatement
        if (false) {
            final Intent intent = new Intent("tk.wasdennnoch.scoop.EXCEPTION")
                    .setClassName(getPackageName(), CrashReceiver.class.getName())
                    .putExtra("pkg", getPackageName())
                    .putExtra("time", System.currentTimeMillis())
                    .putExtra("description", "java.lang.NullPointerException: Someone screwed up")
                    .putExtra("stacktrace", "Testtrace Testtrace Testtrace Testtrace Testtrace Testtrace Testtrace Testtrace Testtrace Testtrace Testtrace Testtrace");
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendBroadcast(intent);
                }
            }, 1000);
        }

        new AvailabilityCheck().execute();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        mAdapter.saveInstanceState(outState);
        mCab.saveState(outState);
    }

    @Override
    protected void onResume() {
        super.onResume();
        mAdapter.setSearchPackageName(this, mPrefs.getBoolean("search_package_name", true)); // Cheap way to instantly apply changes
        sVisible = true;
        mHandler.post(mUpdateCheckerRunnable);
    }

    @Override
    public void onPause() {
        super.onPause();
        sVisible = false;
        mHandler.removeCallbacks(mUpdateCheckerRunnable);
        if (isFinishing() && !mHasCrash)
            Inquiry.destroy("main");
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        mDestroyed = true;
    }

    @Override
    public void onBackPressed() {
        if (mCab.isActive()) {
            mAdapter.setSelectionEnabled(false);
        } else {
            super.onBackPressed();
        }
    }

    private void updateViewStates(Boolean loading) {
        if (loading == null) {
            loading = mWasLoading;
        }
        mWasLoading = loading;
        if (mCheckPending) {
            loading = true;
        }
        boolean empty = mAdapter.isEmpty();
        mLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        mList.setVisibility(loading || empty || !mIsAvailable ? View.GONE : View.VISIBLE);
        //noinspection ConstantConditions
        if (!loading && empty && mIsAvailable) {
            if (mNoItems == null) {
                mNoItems = mNoItemsStub.inflate();
                // Need to do it that way because the devious face doesn't show
                // up as text pre-LP for some reason (at least in my emulators)
                TextView makeCrashTextView = (TextView) findViewById(R.id.makeCrash);
                String makeCrashText = getResources().getString(R.string.make_crash_plain);
                SpannableString spannable = new SpannableString(makeCrashText);
                Drawable drawable = ContextCompat.getDrawable(this, R.drawable.ic_devious_face);
                int height = makeCrashTextView.getLineHeight();
                int width = (int) (((float) height / drawable.getIntrinsicHeight()) * drawable.getIntrinsicWidth());
                drawable.setBounds(0, 0, width, height);
                drawable = DrawableCompat.wrap(drawable);
                DrawableCompat.setTint(drawable.mutate(), Utils.getAttrColor(this, android.R.attr.textColorSecondary));
                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
                spannable.setSpan(imageSpan, makeCrashText.length() - 1, makeCrashText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                makeCrashTextView.setText(spannable);
            }
            mNoItems.setVisibility(View.VISIBLE);
        } else if (mNoItems != null) {
            mNoItems.setVisibility(View.GONE);
        }
        if (!mIsAvailable) {
            ViewStub noXposedStub = findViewById(R.id.noXposedStub);
            if (noXposedStub != null)
                noXposedStub.inflate();
        }
    }

    private void loadData() {
        mAdapter.setSelectionEnabled(false);
        updateViewStates(true);
        mLoader.loadData(this,
                mPrefs.getBoolean("combine_same_stack_trace", true),
                mPrefs.getBoolean("combine_same_apps", false),
                Arrays.asList(mPrefs.getString("blacklisted_packages", "").split(",")));
    }

    public void onDataLoaded(ArrayList<Crash> data) {
        mAdapter.setCrashes(data);
        updateViewStates(false);
    }

    private void setCabActive(boolean active) {
        if (active) {
            mCab.start(this);
            mCab.getToolbar().setOutlineProvider(null);
            AnimationUtils.slideToolbar(mCab.getToolbar(), false, AnimationUtils.ANIM_DURATION_DEFAULT);
        } else {
            mAnimatingCab = true;
            AnimationUtils.slideToolbar(mCab.getToolbar(), true, AnimationUtils.ANIM_DURATION_DEFAULT, false, new Runnable() {
                @Override
                public void run() {
                    mAnimatingCab = false;
                    mCab.finish();
                }
            });
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == CODE_CHILDREN_VIEW && resultCode == RESULT_OK)
            loadData();
    }

    @Override
    public void onCrashClicked(Crash crash) {
        if (mCombineApps && !mHasCrash) {
            startActivityForResult(new Intent(this, MainActivity.class).putExtra(EXTRA_CRASH, crash), CODE_CHILDREN_VIEW);
        } else {
            startActivity(new Intent(this, DetailActivity.class).putExtra(DetailActivity.EXTRA_CRASH, crash));
        }
    }

    @Override
    public void onToggleSelectionMode(boolean enabled) {
        setCabActive(enabled);
    }

    @Override
    public void onItemSelected(int count) {
        mCab.setTitle(String.format(getResources().getQuantityString(R.plurals.items_selected_count, count), count));
    }

    @Override
    public boolean onCabCreated(MaterialCab cab, Menu menu) {
        return true;
    }

    @Override
    public boolean onCabItemClicked(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                final ArrayList<Crash> items = mAdapter.getSelectedItems();
                if (items.isEmpty()) return true;
                String content = String.format(getResources().getQuantityString(R.plurals.delete_multiple_confirm, items.size()), items.size());
                new MaterialDialog.Builder(this)
                        .content(content)
                        .positiveText(R.string.dialog_ok)
                        .negativeText(R.string.dialog_cancel)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog materialDialog, @NonNull DialogAction dialogAction) {
                                // TODO THIS IS A MESS
                                Inquiry instance = Inquiry.get("main");
                                for (Crash c : items) {
                                    if (!mHasCrash && c.children != null) {
                                        for (Crash cc : c.children) {
                                            if (cc.hiddenIds != null) {
                                                instance.delete(Crash.class).whereIn("_id", cc.hiddenIds.toArray()).run();
                                            }
                                            mAdapter.removeCrash(cc);
                                        }
                                        instance.delete(Crash.class).values(c.children).run();
                                    }
                                    if (c.hiddenIds != null) {
                                        instance.delete(Crash.class).whereIn("_id", c.hiddenIds.toArray()).run();
                                    }
                                    instance.delete(Crash.class).values(c).run();
                                    mAdapter.removeCrash(c);
                                }
                                mAdapter.setSelectionEnabled(false);
                                setResult(RESULT_OK); // Reload overview when going back to reflect changes
                                if (mHasCrash && mAdapter.isEmpty()) // Everything deleted, go back to overview
                                    finish();
                                else
                                    updateViewStates(false);
                            }
                        })
                        .show();
                return true;
        }
        return true;
    }

    @Override
    public boolean onCabFinished(MaterialCab cab) {
        mAdapter.setSelectionEnabled(false);
        return !mAnimatingCab;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        return true;
    }

    @Override
    public boolean onClose() {
        mAdapter.search(this, null);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        mAdapter.search(this, newText);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_clear:
                new MaterialDialog.Builder(this)
                        .content(R.string.dialog_clear_content)
                        .negativeText(android.R.string.cancel)
                        .positiveText(android.R.string.ok)
                        .onPositive(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                Inquiry.get("main")
                                        .dropTable(Crash.class); // bam!
                                onDataLoaded(null);
                            }
                        }).show();
                return true;
            case R.id.action_settings:
                startActivity(new Intent(this, SettingsActivity.class));
                return true;
            case android.R.id.home:
                finish();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    class AvailabilityCheck extends AsyncTask<Void, Void, Boolean> {

        @Override
        protected Boolean doInBackground(Void... voids) {
            ScoopApplication app = (ScoopApplication) getApplication();
            return ScoopApplication.Companion.xposedActive() || app.startService();
        }

        @Override
        protected void onPostExecute(Boolean available) {
            if (!mDestroyed) {
                mCheckPending = false;
                mIsAvailable = available;
                updateViewStates(null);
            }
        }
    }

}
