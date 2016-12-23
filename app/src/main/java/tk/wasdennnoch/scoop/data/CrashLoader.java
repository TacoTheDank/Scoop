package tk.wasdennnoch.scoop.data;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.support.annotation.NonNull;
import android.support.v4.content.ContextCompat;
import android.support.v4.util.ArrayMap;
import android.util.LruCache;

import com.afollestad.inquiry.Inquiry;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Map;

import tk.wasdennnoch.scoop.R;
import tk.wasdennnoch.scoop.ui.MainActivity;

@SuppressWarnings("WeakerAccess")
public class CrashLoader {

    private static final int CACHE_SIZE = 25;
    private static LruCache<String, Drawable> sIconCache = new LruCache<>(CACHE_SIZE);
    private static Map<String, String> sNameCache = new ArrayMap<>();

    private WeakReference<MainActivity> mListener;

    public void loadData(MainActivity listener) {
        if (mListener == null || mListener.get() == null) {
            mListener = new WeakReference<>(listener);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    Crash[] result = Inquiry.get("main").selectFrom("crashes", Crash.class).all();
                    final MainActivity listener = mListener.get();
                    if (listener == null || listener.isFinishing() || (Build.VERSION.SDK_INT >= 17 && listener.isDestroyed())) return;
                    if (result == null) {
                        listener.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                listener.onDataLoaded(null);
                            }
                        });
                        return;
                    }
                    // Prefetch and cache the first items to avoid scroll lag.
                    // There is the chance the Activity will be destroyed while the items
                    // get prefetched but the chance is low as it doesn't take long to load
                    for (int i = 0; i < CACHE_SIZE && i < result.length; i++) {
                        getAppIcon(listener, result[i].packageName);
                        getAppName(listener, result[i].packageName, false);
                    }
                    final ArrayList<Crash> data = new ArrayList<>();
                    Collections.addAll(data, result);
                    listener.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            listener.onDataLoaded(data);
                        }
                    });
                }
            }).start();
        }
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
            return ContextCompat.getDrawable(context, R.drawable.ic_bug_vector);
        }
    }

    @NonNull
    public static String getAppName(Context context, String packageName, boolean withNotInstalledInfo) {
        String name = sNameCache.get(packageName);
        if (name != null) return name;
        PackageManager manager = context.getPackageManager();
        try {
            ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
            name =  info.loadLabel(manager).toString();
            sNameCache.put(packageName, name);
            return name;
        } catch (PackageManager.NameNotFoundException n) {
            return withNotInstalledInfo ? context.getString(R.string.app_not_installed, packageName) : packageName;
        }
    }

}
