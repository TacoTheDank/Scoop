package tk.wasdennnoch.scoop.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.style.BackgroundColorSpan;
import android.util.TypedValue;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.SearchView;
import androidx.appcompat.widget.Toolbar;
import androidx.core.content.ContextCompat;

import java.util.Locale;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.data.crash.Crash;
import tk.wasdennnoch.scoop.data.crash.CrashLoader;
import tk.wasdennnoch.scoop.view.CroppingScrollView;
import tk.wasdennnoch.scoop.view.MergedScrollView;

public class DetailActivity extends AppCompatActivity implements SearchView.OnQueryTextListener, SearchView.OnCloseListener {

    public static final String EXTRA_CRASH = "crash";

    private TextView mCrashText;
    private TextView mCrashEdit;
    private MergedScrollView mCrashScroll;
    private Crash mCrash;
    private int mHighlightColor;
    private boolean mSelectionEnabled = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mHighlightColor = ContextCompat.getColor(this, R.color.highlightColor);
        mCrash = getIntent().getParcelableExtra(EXTRA_CRASH);
        getSupportActionBar().setTitle(CrashLoader.getAppName(this, mCrash.packageName, true));
        mCrashScroll = (MergedScrollView) findViewById(R.id.scroll);
        mCrashEdit = (TextView) findViewById(R.id.crashEdit);
        mCrashText = (TextView) findViewById(R.id.crash);
        mCrashText.setText(mCrash.stackTrace);
        mCrashEdit.setText(mCrash.stackTrace);
        mCrashEdit.setTextSize(TypedValue.COMPLEX_UNIT_PX, mCrashText.getTextSize());

        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        ((CroppingScrollView) findViewById(R.id.scroll)).setCropHorizontally(prefs.getBoolean("auto_wrap", false));
    }

    private void highlightText(String text) {
        final String stackTrace = mCrash.stackTrace.toLowerCase(Locale.ENGLISH);
        SpannableString span = new SpannableString(mCrash.stackTrace);
        if (!TextUtils.isEmpty(text)) {
            text = text.toLowerCase(Locale.ENGLISH); // Ignore case
            final int size = text.length();
            int index = 0;
            while ((index = stackTrace.indexOf(text, index)) != -1) {
                span.setSpan(new BackgroundColorSpan(mHighlightColor), index, index + size, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
                index += size;
            }
        }
        mCrashText.setText(span, TextView.BufferType.SPANNABLE);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        MenuItem searchItem = menu.findItem(R.id.action_search);
        SearchView searchView = (SearchView) searchItem.getActionView();
        searchView.setOnQueryTextListener(this);
        searchView.setOnCloseListener(this);
        return true;
    }

    @Override
    public boolean onClose() {
        highlightText(null);
        return false;
    }

    @Override
    public boolean onQueryTextChange(String newText) {
        highlightText(newText);
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
            case R.id.action_select:
                mSelectionEnabled = !mSelectionEnabled;
                item.setIcon(mSelectionEnabled ? R.drawable.ic_unselect : R.drawable.ic_select);
                mCrashEdit.setVisibility(mSelectionEnabled ? View.VISIBLE : View.GONE);
                mCrashScroll.setVisibility(mSelectionEnabled ? View.GONE : View.VISIBLE);
                break;
            case R.id.action_copy:
                ((ClipboardManager) getSystemService(CLIPBOARD_SERVICE)).setPrimaryClip(
                        ClipData.newPlainText(
                                getResources().getString(R.string.copy_label, CrashLoader.getAppName(this, mCrash.packageName, false)),
                                mCrash.stackTrace));
                Toast.makeText(this, R.string.copied_toast, Toast.LENGTH_LONG).show();
                return true;
            case R.id.action_share:
                Intent intent = new Intent(Intent.ACTION_SEND)
                        .setType("text/plain")
                        .putExtra(Intent.EXTRA_TEXT, mCrash.stackTrace);
                startActivity(Intent.createChooser(intent, getString(R.string.action_share)));
                return true;
        }
        return super.onOptionsItemSelected(item);
    }
}
