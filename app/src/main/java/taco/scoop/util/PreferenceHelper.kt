package taco.scoop.util

import android.content.Context
import android.content.SharedPreferences
import android.text.TextUtils
import androidx.core.content.edit
import androidx.preference.PreferenceManager

object PreferenceHelper {
    private lateinit var sharedPreferences: SharedPreferences

    fun Context.initPreferences() {
        sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this)
    }

    @JvmStatic
    fun showNotifications(): Boolean {
        return sharedPreferences.getBoolean("show_notification", true)
    }

    @JvmStatic
    fun showActionButtons(): Boolean {
        return sharedPreferences.getBoolean("show_action_buttons", true)
    }

    @JvmStatic
    fun showStackTraceNotifications(): Boolean {
        return sharedPreferences.getBoolean("show_stack_trace_notif", false)
    }

    fun combineSameApps(): Boolean {
        return sharedPreferences.getBoolean("combine_same_apps", false)
    }

    fun combineSameStackTrace(): Boolean {
        return sharedPreferences.getBoolean("combine_same_stack_trace", true)
    }

    fun searchPackageName(): Boolean {
        return sharedPreferences.getBoolean("search_package_name", true)
    }

    fun autoWrap(): Boolean {
        return sharedPreferences.getBoolean("auto_wrap", false)
    }

    @JvmStatic
    fun ignoreThreadDeath(): Boolean {
        return sharedPreferences.getBoolean("ignore_threaddeath", true)
    }

    fun forceEnglish(): Boolean {
        return sharedPreferences.getBoolean("force_english", false)
    }


    @JvmStatic
    fun getBlacklistedPackages(): String? {
        return sharedPreferences.getString("blacklisted_packages", "")
    }

    fun editBlacklistPackages(packages: ArrayList<String>) {
        sharedPreferences.edit {
            putString("blacklisted_packages", TextUtils.join(",", packages))
        }
    }
}
