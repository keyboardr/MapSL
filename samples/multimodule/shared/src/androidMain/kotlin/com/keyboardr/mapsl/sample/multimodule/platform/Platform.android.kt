package com.keyboardr.mapsl.sample.multimodule.platform

import android.content.Context

actual class PlatformContext(val applicationContext: Context) {
  actual val applicationId = applicationContext.packageName
}