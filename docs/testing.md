# Testing Code that Uses MapSL

When building applications with a Service Locator, a common challenge is writing effective unit
tests. Using the production `ServiceLocator` directly in tests is not ideal, as it would pull in
real dependencies (like network clients or databases), making tests slow, flaky, and not properly
isolated.

MapSL solves this by providing dedicated testing modules that allow you to easily swap the
production service locator for a test-specific one that provides mocks or fakes.

## Testing with `simple-testing`

If your application uses `SimpleServiceLocator` (from the `simple` module), the `simple-testing`
module provides the tools you need.

> **Note for `simple-scaffold` users:** If you are using the `simple-scaffold` module, a
`TestServiceLocator` object has already been created for you. You can skip step 1 below. See
> the [scaffold guide](./scaffold.md "null") for instructions on how to use it by providing a
`MockFactory`.

### 1. Setting Up Your Test Locator

First, add the necessary dependencies to your `build.gradle.kts`:

```kotlin
dependencies {
  // ... other dependencies
  testImplementation("dev.keyboardr.mapsl:simple-testing:<latest_version>")
  testImplementation("org.mockito.kotlin:mockito-kotlin:<latest_version>") // Or io.mockk:mockk for MockK
}
```

Next, in your test source set (`src/test/kotlin`), create a test-specific service locator by
subclassing `SimpleTestingServiceLocator<S>`. This object should include a `register` function to
encapsulate the test setup logic.

```kotlin
// Use the same scope type as your production locator
object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {

  // Implement createMock using your chosen mocking library
  override fun <T : Any> createMock(clazz: KClass<T>): T {
    // Example using Mockito
    return mock(clazz.java)

    // Example using MockK
    // return mockkClass(clazz)
  }

  // Add a register function to handle test setup
  fun register(
    context: Context = ApplicationProvider.getApplicationContext(),
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {}
  ) {
    // This function calls the main register method, swapping the production
    // locator with this test instance.
    MainServiceLocator.register(this, context) {
      // If you have common fakes that all tests should use, pre-register them here.
      put<CoreDependency> { FakeCoreDependency() }

      // Then, call the registration block for test-suite-specific fakes.
      registrationBlock()
    }
  }
}

```

### 2. Using the Test Locator in Tests

In your test class, use a setup method (like one annotated with `@Before` in JUnit) to call your new
`TestServiceLocator.register()` function. This ensures that any code under test that accesses the
global `MainServiceLocator` will receive the testing instance.

Inside the registration block passed to `register`, you can use `put` to provide fakes or
pre-configured mocks specific to your test suite. Unlike production locators, which throw an error
if the same key is registered twice, testing locators allow re-registration. This means any
dependency you provide here will overwrite any common fakes that may have been pre-registered, as
well as the default mock behavior.

```kotlin
@RunWith(AndroidJUnit4::class) // Use appropriate test runner
class MyServiceTest {

  @Before
  fun setUp() {
    // Register the TestServiceLocator before each test
    TestServiceLocator.register(ApplicationProvider.getApplicationContext()) {
      // Register specific fakes or mocks needed for this test suite

      // Example: Provide a fake implementation of a dependency
      put<Dependency> { FakeDependency() }

      // Example: Provide a mock and stub its behavior
      put<AnotherService> {
        mock {
          on { someMethod() } doReturn "stubbed value"
        }
      }
    }

    // You can also configure mocks after calling the `register` function
    // In this example YetAnotherService.instance uses `getOrProvide()`
    YetAnotherService.instance.stub {
      on { fetchDependency() } doAnswer { Dependency.instance }
    }
  }

  @Test
  fun myServiceUsesDependencyCorrectly() {
    // Create the service under test. You want a real instance, so don't use MyService.instance
    val myService = MyService()

    myService.doSomethingThatUsesDependency()

    assertCorrectState(Dependency.instance as FakeDependency)
  }

  @Test
  fun myServiceUsesAnotherServiceCorrectly() {
    // Create the service under test. You want a real instance, so don't use MyService.instance
    val myService = MyService()

    myService.doSomethingThatUsesAnotherService()

    verify(AnotherService.instance).calledCorrectFunction()
  }
}
```

This pattern gives you complete control: you get default mocks for free for any dependency you don't
care about in a specific test, and you can easily provide specific fakes for the dependencies you do
care about.

## Testing with `scoped-testing`

If your application uses `ScopedServiceLocator` and explicit keys (e.g., `LazyKey`, `SingletonKey`),
you should use the `scoped-testing` module and its `TestingServiceLocator<S>` class.

The setup is very similar, but you will use explicit keys when registering fakes.

```kotlin
// In your test's setUp function
@Before
fun setUp() {
  TestScopedServiceLocator.register(/*...*/) {
    // Use an explicit key to register a fake implementation
    put(MyService.Key, FakeMyService())

    // You can still use ClassKey-based puts for simple cases
    put<AnotherService> { FakeAnotherService() }
  }
}
```

The `TestingServiceLocator` understands the different key kinds and their behaviors. For example, it
will provide a new mock each time you `get` from a `FactoryKey`, but will return the same mock for
subsequent `get` calls on a `LazyKey` or `SingletonKey`.

## Handling Android Application `onCreate` in Tests

If you use Robolectric for unit tests and your production `Application.onCreate()` initializes your
`MainServiceLocator`, this can cause the production locator to be registered before your tests run.

The best solution is to configure Robolectric to use a different `Application` class for tests.
Often, using the default `android.app.Application` is sufficient. You can do this by creating a
`src/test/resources/robolectric.properties` file with the following content:

```properties
application=android.app.Application
```

This configuration prevents the production `Application.onCreate()` from running during tests,
allowing your test setup to register the `TestServiceLocator` correctly. This step is not required
if your app does not initialize the service locator in `Application.onCreate()`. For
`simple-scaffold` users, this means the step is unnecessary if you are using the default automatic
initializer, as it is not run during tests.

## Summary

- **Mocks by Default**: If you request a service and no specific registration exists for it, the
  testing locator automatically creates and provides a default mock.

- **Override with `put`**: Use `put` in your test setup to register specific fake implementations or
  pre-configured mocks for the services you need to control.

- **Flexible Registration**: Unlike production locators, testing locators allow a key to be
  registered multiple times (the last one wins). This is useful for setting up different fakes for
  different test cases.

- **`FactoryKey` Behavior**: For a `FactoryKey`, the testing locator provides a *new* mock instance
  on every `get` call, mimicking the production factory behavior.