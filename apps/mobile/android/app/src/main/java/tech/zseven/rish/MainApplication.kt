package tech.zseven.rish

import android.app.Application
import com.facebook.react.PackageList
import com.facebook.react.ReactApplication
import com.facebook.react.ReactHost
import com.facebook.react.ReactNativeApplicationEntryPoint.loadReactNative
import com.facebook.react.defaults.DefaultReactHost.getDefaultReactHost

class MainApplication : Application(), ReactApplication {

  override val reactHost: ReactHost by lazy {
    getDefaultReactHost(
      context = applicationContext,
      useDevSupport = BuildConfig.DEBUG && !BuildConfig.RISH_STANDALONE,
      packageList =
        PackageList(this).packages.apply {
          // Task alerts have a native implementation. Runtime/workspace
          // modules still reject unavailable capabilities explicitly.
          add(tech.zseven.rish.RishNativePackage())
        },
    )
  }

  override fun onCreate() {
    super.onCreate()
    installCrashLogging()
    tech.zseven.rish.tasks.TaskExperience.initialize(this)
    loadReactNative(this)
  }

  /** Debug-only: persist the uncaught exception so a physical-device crash is diagnosable. */
  private fun installCrashLogging() {
    val previous = Thread.getDefaultUncaughtExceptionHandler()
    Thread.setDefaultUncaughtExceptionHandler { thread, error ->
      try {
        val trace = java.io.StringWriter()
        error.printStackTrace(java.io.PrintWriter(trace))
        val header = "Rish debug crash log" +
          "\nthread=" + thread.name +
          "\ntime=" + java.util.Date() +
          "\nstandalone=" + BuildConfig.RISH_STANDALONE +
          "\nguestRuntime=" + BuildConfig.RISH_GUEST_RUNTIME_BUNDLED +
          "\ndevSupport=" + (BuildConfig.DEBUG && !BuildConfig.RISH_STANDALONE) +
          "\nsdk=" + android.os.Build.VERSION.SDK_INT +
          "\npage=" + android.system.Os.sysconf(android.system.OsConstants._SC_PAGESIZE) +
          "\n\n"
        for (dir in listOfNotNull(getExternalFilesDir(null), filesDir)) {
          try {
            java.io.File(dir, "rish-crash.txt").writeText(header + trace.toString())
          } catch (_: Throwable) {
          }
        }
      } catch (_: Throwable) {
      }
      previous?.uncaughtException(thread, error)
    }
  }
}
