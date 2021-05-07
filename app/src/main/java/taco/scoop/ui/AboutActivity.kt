package taco.scoop.ui

import android.content.Intent
import android.content.pm.PackageManager
import android.os.Bundle
import android.view.MenuItem
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.pm.PackageInfoCompat
import androidx.core.net.toUri
import taco.scoop.R
import taco.scoop.databinding.ActivityAboutBinding

class AboutActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivityAboutBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.aboutToolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        binding.aboutCredits.text = getString(
            R.string.about_author,
            "@MrWasdennnoch (XDA), @paphonb (XDA), @TacoTheDank (GitHub)"
        )

        try {
            val pInfo = packageManager.getPackageInfo(packageName, 0)
            binding.aboutVersion.text = String.format(
                getString(R.string.about_version),
                pInfo.versionName,
                PackageInfoCompat.getLongVersionCode(pInfo)
            )
        } catch (e: PackageManager.NameNotFoundException) {
            // No.
        }

        binding.aboutGithub.setOnClickListener {
            val i = Intent(
                Intent.ACTION_VIEW,
                "https://github.com/TacoTheDank/Scoop".toUri()
            )
            startActivity(i)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }
}
