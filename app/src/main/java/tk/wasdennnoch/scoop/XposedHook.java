package tk.wasdennnoch.scoop;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@SuppressWarnings("WeakerAccess")
public class XposedHook implements IXposedHookLoadPackage {

    public static final String INTENT_ACTION = "tk.wasdennnoch.scoop.EXCEPTION";
    public static final String INTENT_ACTION_COPY = "tk.wasdennnoch.scoop.ACTION_COPY";
    public static final String INTENT_ACTION_COPY_LINK = "tk.wasdennnoch.scoop.ACTION_COPY_LINK";
    public static final String INTENT_ACTION_SHARE = "tk.wasdennnoch.scoop.ACTION_SHARE";
    public static final String INTENT_PACKAGE_NAME = "pkg";
    public static final String INTENT_TIME = "time";
    public static final String INTENT_DESCRIPTION = "description";
    public static final String INTENT_STACKTRACE = "stacktrace";
    public static final String INTENT_UPDATE = "update";
    public static final String INTENT_HIDE_UPLOAD = "hideUpload";
    public static final String INTENT_UPLOAD_ERROR = "uploadError";
    public static final String INTENT_DOGBIN_LINK = "dogbinLink";
    private static String mPkg;
    private final XC_MethodHook setUncaughtExceptionHandlerHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (param.args[0] != null)
                hookUncaughtException(param.args[0].getClass());
        }
    };
    private Application mApplication;
    private boolean mSent;
    private final XC_MethodHook uncaughtExceptionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (mSent) {
                Log.d("scoop", "uncaughtExceptionHook (" + mPkg + "): Broadcast already sent");
                return;
            }
            Log.d("scoop", "uncaughtExceptionHook (" + mPkg + "): Sending broadcast");
            Throwable t = (Throwable) param.args[1];
            Intent intent = new Intent(INTENT_ACTION)
                    .setClassName(BuildConfig.APPLICATION_ID, CrashReceiver.class.getName())
                    .putExtra(INTENT_PACKAGE_NAME, mApplication.getPackageName())
                    .putExtra(INTENT_TIME, System.currentTimeMillis())
                    .putExtra(INTENT_DESCRIPTION, t.toString())
                    .putExtra(INTENT_STACKTRACE, Log.getStackTraceString(t));
            // Just send everything here because it costs no performance (well, technically
            // it does, but the process is about to die anyways, so I don't care).
            // Also I have no idea how to detect custom subclasses efficiently.
            mApplication.sendBroadcast(intent);
            mSent = true; // Doesn't need to be reset as process dies soon
        }
    };

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) return;
        mPkg = lpparam.packageName;
        XposedHelpers.findAndHookConstructor(Application.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                mApplication = (Application) param.thisObject;
                mSent = false;
            }
        });
        XposedHelpers.findAndHookMethod(Thread.class, "setDefaultUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, setUncaughtExceptionHandlerHook);
        XposedHelpers.findAndHookMethod(Thread.class, "setUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, setUncaughtExceptionHandlerHook);
        XposedHelpers.findAndHookMethod(ThreadGroup.class, "uncaughtException", Thread.class, Throwable.class, uncaughtExceptionHook);
        hookUncaughtException(Thread.getDefaultUncaughtExceptionHandler().getClass()); // Gets initialized in between native application creation, handleLoadPackage gets called after native creation

        if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod(ScoopApplication.class, "xposedActive", XC_MethodReplacement.returnConstant(true));
        }
    }

    private void hookUncaughtException(Class<?> clazz) {
        int i = 0;
        do { // Search through superclasses
            try {
                XposedHelpers.findAndHookMethod(clazz, "uncaughtException", Thread.class, Throwable.class, uncaughtExceptionHook);
                Log.d("scoop", "hookUncaughtException (" + mPkg + "): Hooked class " + clazz.getName() + " after " + i + " loops");
                return;
            } catch (Throwable ignore) {
            }
            i++;
        } while ((clazz = clazz.getSuperclass()) != null);
        Log.d("scoop", "hookUncaughtException (" + mPkg + "): No class found to hook!");
    }

}
