package tk.wasdennnoch.scoop.data.crash;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;

import com.afollestad.inquiry.Inquiry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.ui.MainActivity;

public class CrashLoader {

    private static Map<String, Drawable> sIconCache = new ArrayMap<>();
    private static Map<String, String> sNameCache = new ArrayMap<>();

    private WeakReference<MainActivity> mListener;

    public void loadData(MainActivity activity, final boolean combineSameStackTrace, final List<String> blacklist) {
        mListener = new WeakReference<>(activity);
        new Thread(new Runnable() {
            @Override
            public void run() {
                Crash[] result = Inquiry.get("main").select(Crash.class).all();
                final MainActivity listener = mListener.get();
                if (listener == null || listener.isFinishing() || (Build.VERSION.SDK_INT >= 17 && listener.isDestroyed()))
                    return;
                if (result == null) {
                    listener.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDataLoaded(null);
                        }
                    });
                    return;
                }
                final ArrayList<Crash> finalData = new ArrayList<>();
                Crash prevSameCrash = null;
                // Combine same stack traces. Don't ask me how it works, but it works. Probably way too complicated.
                for (int i = 1; i < result.length + 1; i++) {
                    Crash previousCrash = result[i - 1];
                    Crash c = i >= result.length ? null : result[i];
                    if (combineSameStackTrace && c != null && previousCrash.stackTrace.equals(c.stackTrace) && previousCrash.packageName.equals(c.packageName)) {
                        c.count = previousCrash.count + 1;
                        prevSameCrash = c;
                    } else {
                        if (prevSameCrash != null) {
                            if (!blacklist.contains(prevSameCrash.packageName))
                                finalData.add(prevSameCrash);
                            prevSameCrash = null;
                        } else {
                            if (!blacklist.contains(previousCrash.packageName))
                                finalData.add(previousCrash);
                        }
                    }
                }
                // Prefetch and cache the first items to avoid scroll lag.
                // There is the chance the Activity will be destroyed while the items
                // get prefetched but the chance is low as it doesn't take long to load
                for (int i = 0; i < finalData.size(); i++) {
                    Crash c = finalData.get(i);
                    getAppIcon(listener, c.packageName);
                    getAppName(listener, c.packageName, false);
                }
                listener.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        listener.onDataLoaded(finalData);
                    }
                });
            }
        }).start();
    }

    @NonNull
    public static Drawable getAppIcon(Context context, String packageName) {
        Drawable icon = sIconCache.get(packageName);
        if (icon != null) return icon;
        PackageManager manager = context.getPackageManager();
        try {
            ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
            icon = info.loadIcon(manager);
            sIconCache.put(packageName, icon);
            return icon;
        } catch (PackageManager.NameNotFoundException n) {
            icon = ContextCompat.getDrawable(context, R.drawable.ic_bug_vector);
            sIconCache.put(packageName, icon);
            return icon;
        }
    }

    @NonNull
    public static String getAppName(Context context, String packageName, boolean withNotInstalledInfo) {
        String name = sNameCache.get(packageName);
        if (name != null)
            return withNotInstalledInfo ? context.getString(R.string.app_not_installed, name) : name;
        PackageManager manager = context.getPackageManager();
        try {
            ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
            name = info.loadLabel(manager).toString();
            sNameCache.put(packageName, name);
            return withNotInstalledInfo ? context.getString(R.string.app_not_installed, name) : name;
        } catch (PackageManager.NameNotFoundException n) {
            name = packageName;
            sNameCache.put(packageName, name);
            return withNotInstalledInfo ? context.getString(R.string.app_not_installed, name) : name;
        }
    }

}
