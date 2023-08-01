package taco.scoop.core.xposed

import android.app.Application
import android.util.Log
import com.highcapable.yukihookapi.YukiHookAPI.encase
import com.highcapable.yukihookapi.annotation.xposed.InjectYukiHookWithXposed
import com.highcapable.yukihookapi.hook.core.YukiMemberHookCreator
import com.highcapable.yukihookapi.hook.entity.YukiBaseHooker
import com.highcapable.yukihookapi.hook.param.PackageParam
import com.highcapable.yukihookapi.hook.xposed.proxy.IYukiHookXposedInit
import taco.scoop.BuildConfig

@InjectYukiHookWithXposed
object YukiXposedHook : IYukiHookXposedInit {
    private var mPkg = ""

    override fun onHook() = encase {
        var mSent = false
        var mApplication: Application
        mPkg = packageName

        if (mPkg == "android")
            return@encase

        findClass("android.app.Application").hook {
            injectMember {
                constructor {
                    Application::class.java
                }
                beforeHook {
                    mApplication = instance()
                    mSent = false
                }
            }
        }

        findClass("java.lang.Thread").hook {
            injectMember {
                method {
                    name = "setDefaultUncaughtExceptionHandler"
                    param(Thread.UncaughtExceptionHandler::class.java)
                }
                beforeHook {
                    if (args[0] != null) {
                        args[0]?.let { hookUncaughtException(it.javaClass) }
                    }
                }
            }
        }

        loadApp(name = BuildConfig.APPLICATION_ID) {

        }
    }

    private fun hookUncaughtException(clazz: Class<*>) {
        var clazz1 = clazz
        var i = 0
        do { // Search through superclasses
            try {
                XposedHelpers.findAndHookMethod(
                    clazz1,
                    "uncaughtException",
                    Thread::class.java,
                    Throwable::class.java,
                    uncaughtExceptionHook
                )
                Log.d(
                    "scoop", "hookUncaughtException (" + mPkg
                            + "): Hooked class " + clazz1.name + " after " + i + " loops"
                )
                return
            } catch (ignore: Throwable) {
            }
            i++
        } while (clazz1.superclass.also { clazz1 = it } != null)
        Log.d("scoop", "hookUncaughtException ($mPkg): No class found to hook!")
    }
}
