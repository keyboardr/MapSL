# Testing Code that Uses MapSL

When building applications with a Service Locator pattern like MapSL, a common concern is how to
write effective unit tests for components that retrieve their dependencies from the locator.
Directly using the production `ServiceLocator` in tests would often lead to:

1. **Reliance on Global State:** Tests become coupled to the global state of the production
   `ServiceLocator`, making them fragile and order-dependent.
2. **Use of Real Dependencies:** Tests would operate on real service implementations (databases,
   network clients, etc.), making them slow, unpredictable, and difficult to isolate.

MapSL provides dedicated testing modules and classes designed to make testing easier by allowing you
to swap out the production service locator for one specifically configured for testing.

## Testing with the Simple Module (`simple-testing`)

If your application uses `SimpleServiceLocator` (which is the recommended starting point), the
`simple.testing` module provides the tools you need for testing.

If your application uses the `simple-scaffold` module, a `TestServiceLocator` class has already been
created for you, and you can skip step 2. Instead, you will just need to create a `MockFactory` to
provide when registering (its implementation is the same as the `createMock()` function in the
`TestServiceLocator` in step 2).

### 1. Add Dependencies

Include the `simple-testing` dependency and your preferred mocking library (like Mockito or MockK)
in your test dependencies.

```kotlin
// build.gradle.kts (or build.gradle)
dependencies {
  // ... other dependencies
  testImplementation("com.keyboardr.mapsl:simple-testing:<version>")
  testImplementation("org.mockito:mockito-core:<version>") // Example for Mockito
  testImplementation("org.mockito.kotlin:mockito-kotlin:<version>") // Example for Mockito-Kotlin
  // or testImplementation("io.mockk:mockk:<version>") // Example for MockK
}
```

### 2. Create a Testing Service Locator

The `simple.testing` module provides `SimpleTestingServiceLocator<S>`. You should create a subclass
of this in your test source set (e.g., `src/test/kotlin`). This testing locator automatically
provides mock instances for any service that hasn't been explicitly registered.

The key requirement for your subclass is to implement the abstract `createMock(clazz: KClass<T>): T`
function. This function is responsible for generating a mock or fake instance for a given class
type.

```kotlin
// In your test source set (e.g., com.yourcompany.yourapp.testing)

object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {

  // Implement createMock using your chosen mocking library
  override fun <T : Any> createMock(clazz: KClass<T>): T {
    // Example using Mockito
    return mock(clazz.java)

    // Using MockK
    // return mockkClass<T>(clazz)
  }

  // Optional: Add a register function to easily set this locator as the process singleton
  fun register(
    applicationContext: Context, // Pass any necessary test setup context
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {}
  ) {
    // Assuming you have a MainServiceLocator singleton in your app
    MainServiceLocator.register(this, applicationContext) {
      // If you have common fakes that test classes should use, you can pre-register them here.
      put<CoreDependency> { FakeCoreDependency() }

      // Call the registration block for the specific test suite
      registrationBlock()
    }
  }
}
```

### 3. Set Up the Test Environment

In your test class, use a setup method (like one annotated with `@Before` in JUnit) to register your
`TestServiceLocator` as the main service locator for the process. This ensures that any code under
test that accesses the global `MainServiceLocator` will receive the testing instance instead of
the real production one.

In this setup block, you can also register specific fake implementations or pre-configured mocks for
services that you need to control or inspect during the test.

```kotlin
// In your test class (e.g., MyServiceTest.kt)

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

In this setup:

- When code requests a dependency that *has not* been registered in the
  `TestServiceLocator.register` block, the `SimpleTestingServiceLocator`'s overridden `onMiss`
  function is called, which in turn calls your `createMock` function to provide a mock instance.
- When code requests a dependency that *has* been registered in the `TestServiceLocator.register`
  block using `put`, the provided fake or mock instance is returned instead of creating a default
  mock.

This allows you to easily control the dependencies of the code you are testing by selectively
providing fakes or mocks in your test setup.

## Application configuration with Robolectric (Android only)

If you have `testOptions.unitTests.includeAndroidResources` set to `true` for your project,
Robolectric will run your `Application` class at the start of testing. For projects that initialize
their `MainServiceLocator` in `Application.onCreate()`, this will cause the production
service locator to be registered. There are two main workarounds:

- Use a different `Application` class for tests. This can either be done using a `@Config`
  annotation on the test class, or by setting it in a properties file.
    - Typically, using `android.app.Application` is appropriate for most unit tests. This can be
      done by creating a `src/test/resources/robolectric.properties` file with the contents
      `application=android.app.Application`. See https://robolectric.org/configuring/ for more
      details.
- Ensure service locators with a `Production` scope don't overwrite testing scopes in the `register`
  function. Rather than the `check` function asserting that `instance` is not initialized, just
  assert that one production scope is not being replaced by another production scope. This check
  becomes a bit awkward, so the other workaround is recommended.

#### Scaffold

The initializer provided by the scaffold is not run during tests, so if your `MainServiceLocator` is
initialized automatically, this doesn't apply.

## Testing with Scoped Service Locators (`scoped.testing`)

If your application uses `ScopedServiceLocator` and leverages different key types explicitly (like
`LazyKey`, `SingletonKey`, `FactoryKey`, `LifecycleKey`), you should use the `scoped.testing` module
instead of `simple.testing`. You will use `TestingServiceLocator<S>` instead of
`SimpleTestingServiceLocator<S>`

The rest will mostly be the same, except using explicit keys for some services. For most key types,
`TestingServiceLocator` will return the same mock instance for a key once one has been created. The
exception is `FactoryKey`, which will return a new mock each time (to mirror its production
behavior). If you have custom key types, you can override `TestingServiceLocator.createMockEntry()`
to specify the correct behavior.

## Summary

- **Mocks by Default:** If you request a service via `get` or `getOrProvide` and no registration
  exists for that key or class, the testing locator will automatically create and provide a mock
  using your `createMock` implementation.
- **Override with `put`:** Use the `put` method in your test setup to register specific fake
  implementations or carefully configured mocks for the services you need to control for a
  particular test.
- **`allowReregister = true`:** Testing locators are initialized with `allowReregister = true`,
  which means you can call `put` for the same key multiple times in your test setup, with the last
  one overwriting previous registrations. This is useful for setting up different fakes/mocks for
  different test cases if needed.
- **FactoryKey Mock Behavior:** For `FactoryKey`, the testing locator provides a *new* mock instance
  on every `get` call by default, mimicking the factory behavior. For other key types (Lazy,
  Singleton, ClassKey), it provides the *same* mock instance for repeated `get` calls if no
  registration is present.

By using `SimpleTestingServiceLocator` or `TestingServiceLocator` and strategically registering
fakes and mocks in your test setup, you can effectively isolate the code you are testing from its
real dependencies and ensure predictable test results.