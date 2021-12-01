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

    fun Context.initPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
        mRes = this.resources
    }

    private fun getKey(@StringRes key: Int): String {
        return mRes.getString(key)
    }

    private fun getSharedBoolean(@StringRes key: Int, defValue: Boolean): Boolean {
        return sharedPreferences.getBoolean(getKey(key), defValue)
    }

    val showNotifications: Boolean
        get() = getSharedBoolean(R.string.prefKey_show_notification, true)

    val showActionButtons: Boolean
        get() = getSharedBoolean(R.string.prefKey_show_action_buttons, true)

    val showStackTraceNotifications: Boolean
        get() = getSharedBoolean(R.string.prefKey_show_stack_trace_notif, false)

    val combineSameApps: Boolean
        get() = getSharedBoolean(R.string.prefKey_combine_same_apps, false)

    val combineSameStackTrace: Boolean
        get() = getSharedBoolean(R.string.prefKey_combine_same_stack_trace, true)

    val searchPackageName: Boolean
        get() = getSharedBoolean(R.string.prefKey_search_package_name, true)

    val autoWrap: Boolean
        get() = getSharedBoolean(R.string.prefKey_auto_wrap, false)

    val autostartOnBoot: Boolean
        get() = getSharedBoolean(R.string.prefKey_autostart_on_boot, false)

    val ignoreThreadDeath: Boolean
        get() = getSharedBoolean(R.string.prefKey_ignore_threaddeath, true)

    val forceEnglish: Boolean
        get() = getSharedBoolean(R.string.prefKey_force_english, false)

    private val blacklistedPackages: String?
        get() = sharedPreferences.getString(
            getKey(R.string.key_blacklisted_packages), ""
        )

    val blacklistList: List<String>
        get() = listOf(*blacklistedPackages?.split(",".toRegex())!!.toTypedArray())

    fun editBlacklistPackages(packages: ArrayList<String>) {
        sharedPreferences.edit {
            putString(
                getKey(R.string.key_blacklisted_packages),
                packages.joinToString(",")
            )
        }
    }
}
