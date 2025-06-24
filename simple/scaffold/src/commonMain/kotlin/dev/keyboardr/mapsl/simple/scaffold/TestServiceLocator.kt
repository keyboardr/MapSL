package dev.keyboardr.mapsl.simple.scaffold

import androidx.annotation.VisibleForTesting
import dev.keyboardr.mapsl.SimpleServiceLocator
import dev.keyboardr.mapsl.simple.scaffold.TestServiceLocator.register
import dev.keyboardr.mapsl.testing.SimpleTestingServiceLocator
import kotlin.reflect.KClass

/**
 * An object for setting up the test environment when using the `simple-scaffold`.
 *
 * Its [register] method should be called in a test setup function (e.g., a method
 * annotated with `@Before` in JUnit4) to initialize the [MainServiceLocator]
 * with a test-specific instance that can provide mocks.
 */
@VisibleForTesting
public object TestServiceLocator {

  /**
   * Initializes and registers a service locator for tests.
   *
   * This should be called from a test `setUp` function. It sets the `instance` in
   * [MainServiceLocator] to a test-specific locator.
   *
   * @param mockFactory An implementation of [MockFactory] that integrates a mocking library
   * (like Mockito or MockK). This is required for the automatic mocking of services that
   * have not been explicitly registered. If this is `null`, any request for an unregistered
   * service will throw an exception.
   * @param registrationBlock A lambda where you can register specific fakes or pre-configured
   * mocks for a test suite. Any service registered here will be used instead of an
   * automatically generated mock.
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

  /**
   * Provides convenient access to the registered test locator instance.
   * Useful for registering fakes directly within a test method.
   */
  public val instance: SimpleServiceLocator<ServiceLocatorScope>
    get() = MainServiceLocator.instance
}

/**
 * An interface for integrating a mocking library with the [TestServiceLocator].
 *
 * Implement this interface to provide a way for the test locator to create mock
 * instances of services on demand.
 */
@VisibleForTesting
public interface MockFactory {

  /** Returns a new mock of type [T]. */
  public fun <T : Any> createMock(clazz: KClass<T>): T
}
