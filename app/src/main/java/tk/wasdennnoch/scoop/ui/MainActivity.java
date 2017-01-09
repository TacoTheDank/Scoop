package tk.wasdennnoch.scoop.ui;

import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v4.view.MenuItemCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.SearchView;
import android.support.v7.widget.Toolbar;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewStub;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.afollestad.inquiry.Inquiry;
import com.afollestad.materialcab.MaterialCab;
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.ArrayList;
import java.util.Arrays;

import tk.wasdennnoch.scoop.CrashReceiver;
import tk.wasdennnoch.scoop.MockThrowable;
import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.data.crash.Crash;
import tk.wasdennnoch.scoop.data.crash.CrashAdapter;
import tk.wasdennnoch.scoop.data.crash.CrashLoader;
import tk.wasdennnoch.scoop.ui.utils.AnimationUtils;
import tk.wasdennnoch.scoop.view.CrashRecyclerView;

public class MainActivity extends AppCompatActivity implements CrashAdapter.Listener, SearchView.OnQueryTextListener, SearchView.OnCloseListener, MaterialCab.Callback {

    private static final String EXTRA_CRASH = "tk.wasdennnoch.scoop.EXTRA_CRASH";

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

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); // To make vector drawables work as menu item drawables
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mList = (CrashRecyclerView) findViewById(R.id.list);
        mLoading = (ProgressBar) findViewById(R.id.loading);
        mNoItemsStub = (ViewStub) findViewById(R.id.noItemStub);

        mPrefs = PreferenceManager.getDefaultSharedPreferences(this);

        mAdapter = new CrashAdapter(this, this);
        mList.setAdapter(mAdapter);
        mList.setVisibility(View.GONE);

        Intent i = getIntent();
        mHasCrash = i.hasExtra(EXTRA_CRASH);
        if (mHasCrash) {
            Crash c = i.getParcelableExtra(EXTRA_CRASH);
            ArrayList<Crash> crashes = new ArrayList<>();
            crashes.add(c);
            crashes.addAll(c.children);
            mAdapter.setCrashes(crashes);
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
                    .putExtra("cause", new MockThrowable(new NullPointerException("Just a test")));
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    sendBroadcast(intent);
                }
            }, 1000);
        }
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
        if (isFinishing())
            Inquiry.destroy("main");
    }

    @Override
    public void onBackPressed() {
        if (mCab.isActive()) {
            mAdapter.setSelectionEnabled(false);
        } else {
            super.onBackPressed();
        }
    }

    private boolean isActive() {
        // TODO looks like this doesn't get hoked correctly
        return true; // Build.HARDWARE.equals("goldfish") || Build.HARDWARE.equals("ranchu"); // true on emulator
    }

    private void updateViewStates(boolean loading) {
        boolean empty = mAdapter.isEmpty();
        mLoading.setVisibility(loading ? View.VISIBLE : View.GONE);
        mList.setVisibility(loading || empty ? View.GONE : View.VISIBLE);
        //noinspection ConstantConditions
        if (!loading && empty && isActive()) {
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
                DrawableCompat.setTint(drawable.mutate(), ContextCompat.getColor(this, R.color.text_disabled_light));
                ImageSpan imageSpan = new ImageSpan(drawable, ImageSpan.ALIGN_BASELINE);
                spannable.setSpan(imageSpan, makeCrashText.length() - 1, makeCrashText.length(), Spannable.SPAN_INCLUSIVE_EXCLUSIVE);
                makeCrashTextView.setText(spannable);
            }
            mNoItems.setVisibility(View.VISIBLE);
        } else if (mNoItems != null) {
            mNoItems.setVisibility(View.GONE);
        }
        //noinspection ConstantConditions
        if (!isActive()) {
            ViewStub noXposedStub = (ViewStub) findViewById(R.id.noXposedStub);
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
    public void onCrashClicked(Crash crash) {
        if (mCombineApps && !mHasCrash) {
            startActivity(new Intent(this, MainActivity.class).putExtra(EXTRA_CRASH, crash));
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
                                Inquiry instance = Inquiry.get("main");
                                for (Crash c : items) {
                                    int pos = mAdapter.getPosition(c);
                                    for (int i = pos; i < pos + c.count; i++) {
                                        instance.delete(Crash.class).atPosition(pos).run();
                                    }
                                    mAdapter.removeCrash(c);
                                }
                                mAdapter.setSelectionEnabled(false);
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
        SearchView searchView = (SearchView) MenuItemCompat.getActionView(searchItem);
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
        }
        return super.onOptionsItemSelected(item);
    }

    public static void requestUpdate(Crash newCrash) {
        sUpdateRequired = true;
        if (sVisible) {
            sNewCrash = newCrash;
        }
    }

    private final Runnable mUpdateCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            if (sUpdateRequired) {
                sUpdateRequired = false;
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

}
