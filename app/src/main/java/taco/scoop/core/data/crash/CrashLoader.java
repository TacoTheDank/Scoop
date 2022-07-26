package taco.scoop.core.data.crash;

import android.content.Context;
import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;
import android.graphics.drawable.Drawable;

import androidx.annotation.NonNull;
import androidx.collection.ArrayMap;
import androidx.core.content.ContextCompat;

import java.lang.ref.WeakReference;
import java.text.Collator;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Map;

import taco.scoop.R;
import taco.scoop.core.db.CrashDatabase;
import taco.scoop.ui.activity.MainActivity;

public class CrashLoader {

    private static final Map<String, Drawable> sIconCache = new ArrayMap<>();
    private static final Map<String, String> sNameCache = new ArrayMap<>();

    private WeakReference<MainActivity> mListener;
    private boolean mCombineSameTrace;
    private boolean mCombineSameApps;
    private List<String> mBlacklist;

    @NonNull
    public static Drawable getAppIcon(Context context, String packageName) {
        Drawable icon = sIconCache.get(packageName);
        if (icon == null) {
            PackageManager manager = context.getPackageManager();
            try {
                ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
                icon = info.loadIcon(manager);
            } catch (PackageManager.NameNotFoundException n) {
                icon = ContextCompat.getDrawable(context, R.drawable.ic_bug_report);
            }
            sIconCache.put(packageName, icon);
        }
        return icon;
    }

    @NonNull
    public static String getAppName(Context context, String packageName, boolean withNotInstalledInfo) {
        String name = sNameCache.get(packageName);
        boolean notInstalled = false;
        if (name == null) {
            PackageManager manager = context.getPackageManager();
            try {
                ApplicationInfo info = manager.getApplicationInfo(packageName, 0);
                name = info.loadLabel(manager).toString();
            } catch (PackageManager.NameNotFoundException n) {
                name = packageName;
                notInstalled = true;
            }
            // Let's hope no app name starts with this
            sNameCache.put(packageName, notInstalled ? "!=!" + name : name);
        }
        if (!notInstalled && name.startsWith("!=!")) {
            name = name.substring(3);
            notInstalled = true;
        }
        return withNotInstalledInfo && notInstalled ? context.getString(R.string.app_not_installed, name) : name;
    }

    public void loadData(MainActivity activity, boolean combineSameStackTrace,
                         boolean combineSameApps, List<String> blacklist) {
        mListener = new WeakReference<>(activity);
        mCombineSameTrace = combineSameStackTrace;
        mCombineSameApps = combineSameApps;
        mBlacklist = blacklist;
        new Thread(() -> {
            Crash[] result = CrashDatabase.selectAll();
            final MainActivity listener = mListener.get();
            if (listener == null || listener.isFinishing() || listener.isDestroyed())
                return;
            if (result == null) {
                listener.runOnUiThread(() -> listener.onDataLoaded(null));
                return;
            }
            ArrayList<Crash> data = new ArrayList<>(Arrays.asList(result));
            if (mCombineSameApps)
                sortApps(listener, data);
            data = combineStackTraces(data);
            data = combineSameApps(data);
            setupAdapterData(data);
            // Prefetch and cache the first items to avoid scroll lag.
            // There is the chance the Activity will be destroyed while the items
            // get prefetched but the chance is low as it doesn't take long to load
            for (int i = 0; i < data.size(); i++) {
                Crash c = data.get(i);
                getAppIcon(listener, c.packageName);
                if (!mCombineSameApps) // App names already get fetched during sorting, no need to fetch again
                    getAppName(listener, c.packageName, false);
            }
            final ArrayList<Crash> finalData = data;
            listener.runOnUiThread(() -> listener.onDataLoaded(finalData));
        }).start();
    }

    private ArrayList<Crash> combineStackTraces(List<Crash> crashes) {
        final ArrayList<Crash> newData = new ArrayList<>();
        Crash prevSameCrash = null;
        // Combine same stack traces. Don't ask me how it works, but it works. Probably way too complicated.
        for (int i = 1; i < crashes.size() + 1; i++) {
            Crash previousCrash = crashes.get(i - 1);
            Crash c = i >= crashes.size() ? null : crashes.get(i);
            if (mCombineSameTrace && c != null
                    && previousCrash.stackTrace.equals(c.stackTrace)
                    && previousCrash.packageName.equals(c.packageName)
            ) {
                c.count = previousCrash.count + 1;
                if (c.hiddenIds == null)
                    c.hiddenIds = new ArrayList<>();
                if (previousCrash.hiddenIds != null)
                    c.hiddenIds.addAll(previousCrash.hiddenIds);
                c.hiddenIds.add(previousCrash.id);
                prevSameCrash = c;
            } else {
                if (prevSameCrash != null) {
                    if (!mBlacklist.contains(prevSameCrash.packageName))
                        newData.add(prevSameCrash);
                    prevSameCrash = null;
                } else {
                    if (!mBlacklist.contains(previousCrash.packageName))
                        newData.add(previousCrash);
                }
            }
        }
        return newData;
    }

    private void sortApps(final Context context, ArrayList<Crash> crashes) {
        Collections.sort(crashes, new Comparator<Crash>() {
            private final Collator sC = Collator.getInstance();

            @Override
            public int compare(Crash o1, Crash o2) {
                return sC.compare(
                        getAppName(context, o1.packageName, false),
                        getAppName(context, o2.packageName, false));
            }
        });
    }

    private ArrayList<Crash> combineSameApps(ArrayList<Crash> crashes) {
        if (!mCombineSameApps) return crashes;
        ArrayList<Crash> newData = new ArrayList<>();
        for (int i = 0; i < crashes.size(); i++) {
            Crash c = crashes.get(i);
            Crash p = i == 0 ? null : crashes.get(i - 1);
            if (p != null && c.packageName.equals(p.packageName)) {
                Crash c2 = newData.get(newData.size() - 1);
                if (c2.children == null) {
                    c2.children = new ArrayList<>();
                }
                c2.children.add(c);
            } else {
                newData.add(c);
            }
        }
        return newData;
    }

    private void setupAdapterData(ArrayList<Crash> crashes) {
        // Set time to latest crash time and count together the.. count
        for (Crash c : crashes) {
            long newestTime = c.time;
            int count = c.count;
            if (c.children != null) {
                for (Crash cc : c.children) {
                    count += cc.count;
                    if (cc.time > newestTime)
                        newestTime = cc.time;
                }
            }
            c.time = newestTime;
            c.displayCount = count;
        }
    }
}
