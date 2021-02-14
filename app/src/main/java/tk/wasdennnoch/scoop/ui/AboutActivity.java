package tk.wasdennnoch.scoop.ui;

import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.MenuItem;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.AppCompatButton;
import androidx.core.content.pm.PackageInfoCompat;

import tk.wasdennnoch.scoop.R;

public class AboutActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_about);
        setSupportActionBar(findViewById(R.id.about_toolbar));
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        AppCompatButton checkUpdate = findViewById(R.id.about_updates);

        try {
            PackageInfo pInfo = getPackageManager().getPackageInfo(getPackageName(), 0);
            ((TextView) findViewById(R.id.about_version))
                    .setText(String.format(getString(R.string.about_version),
                            pInfo.versionName,
                            PackageInfoCompat.getLongVersionCode(pInfo)));
        } catch (PackageManager.NameNotFoundException e) {
            // No.
        }

        ((TextView) findViewById(R.id.about_credits)).setText(getString(R.string.about_author,
                "@MrWasdennnoch (XDA), @paphonb (XDA), @TacoTheDank (GitHub)"));

        checkUpdate.setOnClickListener(v -> {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("https://github.com/TacoTheDank/Scoop/releases"));
            startActivity(i);
        });
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
