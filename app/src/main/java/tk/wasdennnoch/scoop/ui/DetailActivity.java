package tk.wasdennnoch.scoop.ui;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.TextView;
import android.widget.Toast;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.data.Crash;
import tk.wasdennnoch.scoop.data.CrashLoader;

public class DetailActivity extends AppCompatActivity {

    public static final String EXTRA_CRASH = "stacktrace";

    private Crash mCrash;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_detail);
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        mCrash = getIntent().getParcelableExtra(EXTRA_CRASH);
        getSupportActionBar().setTitle(CrashLoader.getAppName(this, mCrash.packageName, true));
        TextView crashText = (TextView) findViewById(R.id.crash);
        crashText.setText(mCrash.stackTrace);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_detail, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                finish();
                return true;
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
