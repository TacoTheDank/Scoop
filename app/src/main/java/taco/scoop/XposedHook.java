package taco.scoop;

import android.app.Application;
import android.content.Intent;
import android.util.Log;

import de.robv.android.xposed.IXposedHookLoadPackage;
import de.robv.android.xposed.XC_MethodHook;
import de.robv.android.xposed.XC_MethodReplacement;
import de.robv.android.xposed.XposedHelpers;
import de.robv.android.xposed.callbacks.XC_LoadPackage;
import taco.scoop.receiver.CrashReceiver;

@SuppressWarnings("WeakerAccess")
public class XposedHook implements IXposedHookLoadPackage {

    private static String mPkg;
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
            Intent intent = new Intent(Intents.INTENT_ACTION)
                    .setClassName(BuildConfig.APPLICATION_ID, CrashReceiver.class.getName())
                    .putExtra(Intents.INTENT_PACKAGE_NAME, mApplication.getPackageName())
                    .putExtra(Intents.INTENT_TIME, System.currentTimeMillis())
                    .putExtra(Intents.INTENT_DESCRIPTION, t.toString())
                    .putExtra(Intents.INTENT_STACKTRACE, Log.getStackTraceString(t));
            // Just send everything here because it costs no performance (well, technically
            // it does, but the process is about to die anyways, so I don't care).
            // Also I have no idea how to detect custom subclasses efficiently.
            mApplication.sendBroadcast(intent);
            mSent = true; // Doesn't need to be reset as process dies soon
        }
    };
    private final XC_MethodHook setUncaughtExceptionHandlerHook = new XC_MethodHook() {
        @Override
        protected void beforeHookedMethod(MethodHookParam param) throws Throwable {
            if (param.args[0] != null)
                hookUncaughtException(param.args[0].getClass());
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
        XposedHelpers.findAndHookMethod(
                Thread.class,
                "setDefaultUncaughtExceptionHandler",
                Thread.UncaughtExceptionHandler.class,
                setUncaughtExceptionHandlerHook);

        XposedHelpers.findAndHookMethod(
                Thread.class,
                "setUncaughtExceptionHandler",
                Thread.UncaughtExceptionHandler.class,
                setUncaughtExceptionHandlerHook);

        XposedHelpers.findAndHookMethod(
                ThreadGroup.class,
                "uncaughtException",
                Thread.class,
                Throwable.class,
                uncaughtExceptionHook);

        // Gets initialized in between native application creation, handleLoadPackage gets
        //  called after native creation
        hookUncaughtException(Thread.getDefaultUncaughtExceptionHandler().getClass());

        if (lpparam.packageName.equals(BuildConfig.APPLICATION_ID)) {
            XposedHelpers.findAndHookMethod(
                    ScoopApplication.class,
                    "xposedActive",
                    XC_MethodReplacement.returnConstant(true));
        }
    }

    private void hookUncaughtException(Class<?> clazz) {
        int i = 0;
        do { // Search through superclasses
            try {
                XposedHelpers.findAndHookMethod(
                        clazz,
                        "uncaughtException",
                        Thread.class,
                        Throwable.class,
                        uncaughtExceptionHook);
                Log.d("scoop", "hookUncaughtException (" + mPkg
                        + "): Hooked class " + clazz.getName() + " after " + i + " loops");
                return;
            } catch (Throwable ignore) {
            }
            i++;
        } while ((clazz = clazz.getSuperclass()) != null);
        Log.d("scoop", "hookUncaughtException (" + mPkg + "): No class found to hook!");
    }
}
