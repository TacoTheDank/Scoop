package tk.wasdennnoch.scoop.ui;

import android.content.Intent;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.app.AppCompatDelegate;
import android.support.v7.widget.RecyclerView;
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
import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;

import java.util.List;

import tk.wasdennnoch.scoop.CrashReceiver;
import tk.wasdennnoch.scoop.MockThrowable;
import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.data.Crash;
import tk.wasdennnoch.scoop.data.CrashAdapter;
import tk.wasdennnoch.scoop.data.CrashLoader;

public class MainActivity extends AppCompatActivity implements CrashAdapter.OnCrashClickListener {

    private static final int UPDATE_DELAY = 1000;
    private static boolean sUpdateRequired; // cheapest way to keep track of updates lol

    private Handler mHandler;
    private CrashLoader mLoader = new CrashLoader();
    private RecyclerView mList;
    private ProgressBar mLoading;
    private ViewStub mNoItemsStub;
    private View mNoItems;
    private CrashAdapter mAdapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        AppCompatDelegate.setCompatVectorFromResourcesEnabled(true); // To make vector drawables work as menu item drawables
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));

        mList = (RecyclerView) findViewById(R.id.list);
        mLoading = (ProgressBar) findViewById(R.id.loading);
        mNoItemsStub = (ViewStub) findViewById(R.id.noItemStub);

        mAdapter = new CrashAdapter(this);
        mList.setAdapter(mAdapter);
        mList.setVisibility(View.GONE);

        Inquiry.newInstance(this, "crashes")
                .instanceName("main")
                .build();

        sUpdateRequired = false;
        loadData();
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
            }, 4000);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        mHandler.postDelayed(mUpdateCheckerRunnable, UPDATE_DELAY);
    }

    @Override
    public void onPause() {
        super.onPause();
        mHandler.removeCallbacks(mUpdateCheckerRunnable);
        if (isFinishing())
            Inquiry.destroy("main");
    }

    private void loadData() {
        mLoading.setVisibility(View.VISIBLE);
        if (mNoItems != null)
            mNoItems.setVisibility(View.GONE);
        mList.setVisibility(View.GONE);
        mLoader.loadData(this);
    }

    public void onDataLoaded(List<Crash> data) {
        mLoading.setVisibility(View.GONE);
        mAdapter.setCrashes(data);
        if (data != null && !data.isEmpty()) {
            mList.setVisibility(View.VISIBLE);
        } else {
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
        }
    }

    @Override
    public void onCrashClicked(Crash crash) {
        startActivity(new Intent(this, DetailActivity.class).putExtra(DetailActivity.EXTRA_CRASH, crash));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
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
                                        .dropTable("crashes"); // bam!
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

    public static void requestUpdate() {
        sUpdateRequired = true;
    }

    private final Runnable mUpdateCheckerRunnable = new Runnable() {
        @Override
        public void run() {
            if (sUpdateRequired) {
                loadData();
                sUpdateRequired = false;
            }
            mHandler.postDelayed(mUpdateCheckerRunnable, UPDATE_DELAY);
        }
    };

}
