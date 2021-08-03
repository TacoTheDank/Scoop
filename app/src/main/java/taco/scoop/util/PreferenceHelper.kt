package taco.scoop.util

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Resources
import androidx.annotation.StringRes
import androidx.core.content.edit
import androidx.preference.PreferenceManager
import taco.scoop.R

object PreferenceHelper {
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var mRes: Resources

    fun Context.initPreferences(res: Resources) {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mRes = res
    }

    private fun getKey(@StringRes key: Int): String {
        return mRes.getString(key)
    }

    private fun getSharedBoolean(@StringRes key: Int, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(getKey(key), defValue)
    }

    @JvmStatic
    fun showNotifications(): Boolean {
        return getSharedBoolean(R.string.prefKey_show_notification, true)
    }

    @JvmStatic
    fun showActionButtons(): Boolean {
        return getSharedBoolean(R.string.prefKey_show_action_buttons, true)
    }

    @JvmStatic
    fun showStackTraceNotifications(): Boolean {
        return getSharedBoolean(R.string.prefKey_show_stack_trace_notif, false)
    }

    fun combineSameApps(): Boolean {
        return getSharedBoolean(R.string.prefKey_combine_same_apps, false)
    }

    fun combineSameStackTrace(): Boolean {
        return getSharedBoolean(R.string.prefKey_combine_same_stack_trace, true)
    }

    fun searchPackageName(): Boolean {
        return getSharedBoolean(R.string.prefKey_search_package_name, true)
    }

    fun autoWrap(): Boolean {
        return getSharedBoolean(R.string.prefKey_auto_wrap, false)
    }

    fun autostartOnBoot(): Boolean {
        return getSharedBoolean(R.string.prefKey_autostart_on_boot, false)
    }

    @JvmStatic
    fun ignoreThreadDeath(): Boolean {
        return getSharedBoolean(R.string.prefKey_ignore_threaddeath, true)
    }

    fun forceEnglish(): Boolean {
        return getSharedBoolean(R.string.prefKey_force_english, false)
    }


    @JvmStatic
    fun getBlacklistedPackages(): String? {
        return sharedPreferences.getString(
            getKey(R.string.key_blacklisted_packages), ""
        )
    }

    fun editBlacklistPackages(packages: ArrayList<String>) {
        sharedPreferences.edit {
            putString(
                getKey(R.string.key_blacklisted_packages),
                packages.joinToString(",")
            )
        }
    }
}
