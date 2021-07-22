package taco.scoop.ui.activity

import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import androidx.annotation.StringRes
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.commit
import androidx.fragment.app.replace
import androidx.preference.Preference
import androidx.preference.PreferenceFragmentCompat
import taco.scoop.R
import taco.scoop.databinding.ActivitySettingsBinding
import taco.scoop.util.openSystemNotificationSettings
import taco.scoop.util.readLogsPermissionGranted

class SettingsActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        val binding = ActivitySettingsBinding.inflate(layoutInflater)
        setContentView(binding.root)
        setSupportActionBar(binding.settingsToolbar.toolbar)
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        supportFragmentManager.commit {
            replace<SettingsFragment>(binding.settingsContainer.id)
        }
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == android.R.id.home) {
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    class SettingsFragment : PreferenceFragmentCompat() {
        override fun onCreatePreferences(savedInstanceState: Bundle?, rootKey: String?) {
            addPreferencesFromResource(R.xml.preferences)

            val notifSettingsPref = findPreference(R.string.prefKey_notif_sys_settings)
            notifSettingsPref?.setOnPreferenceClickListener {
                requireContext().openSystemNotificationSettings()
                true
            }

            val blacklistedAppsPref = findPreference(R.string.prefKey_blacklisted_apps)
            blacklistedAppsPref?.setActivityIntent(BlacklistAppsActivity::class.java)

            val permissionStatusPref = findPreference(R.string.prefKey_permission_status)
            permissionStatusPref?.summary =
                if (requireContext().readLogsPermissionGranted()) {
                    getString(R.string.settings_permission_status_summary_true)
                } else {
                    getString(R.string.settings_permission_status_summary_false)
                }

            val aboutPref = findPreference(R.string.prefKey_about_scoop)
            aboutPref?.setActivityIntent(AboutActivity::class.java)
        }

        private fun Preference.setActivityIntent(clazz: Class<*>) {
            intent = Intent(activity, clazz)
        }

        private fun findPreference(@StringRes key: Int): Preference? {
            return findPreference(getString(key))
        }
    }
}
