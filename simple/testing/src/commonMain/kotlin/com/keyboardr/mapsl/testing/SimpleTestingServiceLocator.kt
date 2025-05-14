package com.keyboardr.mapsl.testing

import com.keyboardr.mapsl.SimpleServiceLocator
import kotlin.reflect.KClass

public abstract class SimpleTestingServiceLocator<S>(scope: S) :
  SimpleServiceLocator<S>(scope, allowReregister = true) {

  /**
   * Returns a mock instance for type [T]
   */
  protected abstract fun <T : Any> createMock(clazz: KClass<T>): T

  override fun <T : Any> onMiss(key: KClass<T>): T {
    return createMock(key)
  }

  override fun <T : Any> onInvalidScope(key: KClass<T>): T {
    return createMock(key)
  }
}
