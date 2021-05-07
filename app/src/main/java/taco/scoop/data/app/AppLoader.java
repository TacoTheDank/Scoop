package taco.scoop.data.app;

import android.content.pm.ApplicationInfo;
import android.content.pm.PackageManager;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import taco.scoop.ui.BlacklistAppsActivity;

public class AppLoader {

    private WeakReference<BlacklistAppsActivity> mListener;

    public void loadData(BlacklistAppsActivity activity) {
        mListener = new WeakReference<>(activity);
        new Thread(() -> {
            PackageManager pm = mListener.get().getPackageManager();
            // TODO: Deal with https://developer.android.com/about/versions/11/privacy/package-visibility
            List<ApplicationInfo> apps = pm.getInstalledApplications(0);
            Collections.sort(apps, new ApplicationInfo.DisplayNameComparator(pm));
            final ArrayList<App> finalApps = new ArrayList<>();
            Loop:
            for (ApplicationInfo a : apps) {
                // Some apps (like .Wave) add multiple launcher icons, this bugs blacklist
                // selection, so only add one item per package name
                for (App app : finalApps) {
                    if (app.getPackageName().equals(a.packageName))
                        continue Loop;
                }
                finalApps.add(new App(a.loadIcon(pm), a.loadLabel(pm).toString(), a.packageName));
            }

            final BlacklistAppsActivity listener = mListener.get();
            if (listener == null || listener.isFinishing() || listener.isDestroyed())
                return;

            listener.runOnUiThread(() -> listener.onDataLoaded(finalApps));
        }).start();
    }
}
