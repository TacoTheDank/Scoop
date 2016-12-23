package tk.wasdennnoch.scoop;

import android.app.Application;
import android.content.Intent;
import android.support.annotation.Keep;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;

@Keep
public class XposedHook implements IXposedHookLoadPackage {

    static final String INTENT_ACTION = "tk.wasdennnoch.scoop.EXCEPTION";
    static final String INTENT_PACKAGE_NAME = "pkg";
    static final String INTENT_TIME = "time";
    static final String INTENT_THROWABLE = "cause";

    private Application mApplication;
    private static String mPkg;
    private boolean mSent;

    @Override
    public void handleLoadPackage(final XC_LoadPackage.LoadPackageParam lpparam) throws Throwable {
        if (lpparam.packageName.equals("android")) return;
        XposedHelpers.findAndHookConstructor(Application.class, new XC_MethodHook() {
            @Override
            protected void beforeHookedMethod(XC_MethodHook.MethodHookParam param) throws Throwable {
                mPkg = lpparam.packageName;
                mApplication = (Application) param.thisObject;
                mSent = false;
            }
        });
        XposedHelpers.findAndHookMethod(Thread.class, "setDefaultUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, setUncaughtExceptionHandlerHook);
        XposedHelpers.findAndHookMethod(Thread.class, "setUncaughtExceptionHandler", Thread.UncaughtExceptionHandler.class, setUncaughtExceptionHandlerHook);
        XposedHelpers.findAndHookMethod(ThreadGroup.class, "uncaughtException", Thread.class, Throwable.class, uncaughtExceptionHook);
        hookUncaughtException(Thread.getDefaultUncaughtExceptionHandler().getClass()); // Gets initialized in between native application creation, handleLoadPackage gets called after native creation
    }

    private final XC_MethodHook setUncaughtExceptionHandlerHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            hookUncaughtException(param.args[0].getClass());
        }
    };

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

    private final XC_MethodHook uncaughtExceptionHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (mSent) {
                Log.d("scoop", "uncaughtExceptionHook (" + mPkg + "): Broadcast already sent, aborting");
                return;
            }
            Log.d("scoop", "uncaughtExceptionHook (" + mPkg + "): sending broadcast");
            Intent intent = new Intent(INTENT_ACTION)
                    .setPackage(XposedHook.class.getPackage().getName())
                    .putExtra(INTENT_PACKAGE_NAME, mApplication.getPackageName())
                    .putExtra(INTENT_TIME, System.currentTimeMillis())
                    .putExtra(INTENT_THROWABLE, (Throwable) param.args[1]);
            mApplication.sendBroadcast(intent);
            mSent = true; // Doesn't need to be reset as process dies anyways
        }
    };

}
