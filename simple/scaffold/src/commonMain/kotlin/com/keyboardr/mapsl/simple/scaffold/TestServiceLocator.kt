package com.keyboardr.mapsl.simple.scaffold

import androidx.annotation.VisibleForTesting
import com.keyboardr.mapsl.SimpleServiceLocator
import com.keyboardr.mapsl.testing.SimpleTestingServiceLocator
import kotlin.reflect.KClass

/**
 * Registers a service locator appropriate for use in tests. This should not be referenced from
 * production code.
 */
@VisibleForTesting
public object TestServiceLocator {

  /**
   * Registers a service locator for tests. This should be called from a test setUp function
   * (annotated with `@Before` in JUnit4).
   *
   * If automatic mocking is desired, you must provide a [MockFactory] to use. If no factory is
   * provided, an exception will be thrown when requesting classes that do not have a value
   * registered.
   *
   * [registrationBlock] is provided as a convenient place to register services common to all tests
   * in a test suite.
   */
  public fun register(
    mockFactory: MockFactory? = null,
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {},
  ) {
    MainServiceLocator.register(object :
      SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {
      override fun <T : Any> createMock(clazz: KClass<T>): T = mockFactory?.createMock(clazz)
        ?: throw IllegalArgumentException("No service was registered for $clazz and no `MockFactory` was provided")

    }, registrationBlock)
  }

  public val instance: SimpleServiceLocator<ServiceLocatorScope>
    get() = MainServiceLocator.instance
}

/** Provides mocks for use with [TestServiceLocator]. */
@VisibleForTesting
public interface MockFactory {

  /** Returns a new mock of type [T] */
  public fun <T : Any> createMock(clazz: KClass<T>): T
}