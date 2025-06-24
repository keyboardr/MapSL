package dev.keyboardr.mapsl.sample.multimodule

import android.annotation.SuppressLint
import android.app.ActivityManager
import android.app.Application
import android.os.Build
import android.os.Process
import androidx.core.content.getSystemService
import dev.keyboardr.mapsl.SimpleServiceLocator
import dev.keyboardr.mapsl.sample.multimodule.locator.MainServiceLocator
import dev.keyboardr.mapsl.sample.multimodule.locator.ServiceLocatorScope
import dev.keyboardr.mapsl.sample.multimodule.platform.PlatformContext

class MultimoduleApplication : Application() {
  override fun onCreate() {
    super.onCreate()
    MainServiceLocator.register(
      SimpleServiceLocator(ServiceLocatorScope.Process(processName = getProcessNameCompat())),
      PlatformContext(this)
    )
  }

  private fun getProcessNameCompat(): String {
    return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
      getProcessName()
    } else {
      processNameFromActivityThread() ?: processNameFromActivityManager() ?: ""
    }
  }

  private fun processNameFromActivityManager(): String? {
    val pid: Int = Process.myPid()
    val manager = getSystemService<ActivityManager>()!!

    val fromActivityManager =
      manager.runningAppProcesses?.filterNotNull()?.firstOrNull { it.pid == pid }?.processName
    return fromActivityManager
  }

  private fun processNameFromActivityThread(): String? {
    // Using the same technique as Application.getProcessName() for older devices
    // Using reflection since ActivityThread is an internal API
    try {
      @SuppressLint("PrivateApi")
      val activityThread = Class.forName("android.app.ActivityThread")
      val methodName = "currentProcessName"

      @SuppressLint("DiscouragedPrivateApi")
      val getProcessName = activityThread.getDeclaredMethod(methodName)

      return getProcessName.invoke(null) as String
    } catch (_: Exception) {
      return null
    }
  }
}