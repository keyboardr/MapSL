# MapSL: Getting Started with the Simple Flavor

Welcome to MapSL! This guide will help you get started with the basics of using MapSL for dependency
management in your Kotlin projects. We'll focus on the `simple` module, which provides a streamlined
API recommended for most common use cases like managing application-wide singletons.

## What is MapSL (Simple Flavor)?

MapSL is a Service Locator library for Kotlin. The `simple` module offers an easy-to-understand
approach to managing dependencies. It allows you to register and retrieve services using their
Kotlin Class (`KClass`) as the identifier. The `SimpleServiceLocator` within this module is designed
primarily for managing single instances of your services, created lazily the first time they are
needed.

The key benefits of using MapSL's `simple` module include:

- **Simplicity:** A straightforward API focused on the common patterns.
- **No Code Generation:** Avoids build-time overhead and complexity associated with annotation
  processors.
- **Testability:** Provides built-in support for swapping out dependencies in tests.
- **Easy migration:** Built on top of the `core` and `scoped` modules, so migration is easy if more power or flexibility is
  needed (see [Migration](migration.md)).

## 1. Add Dependencies

To use the `simple` flavor, add the `simple` module dependency to your main source set and the
`simple-testing` dependency to your test source set in your project's `build.gradle.kts` file.
You'll also need a mocking library for testing.

You may use the `simple-scaffold` module instead of `simple` if you would like a basic
`MainServiceLocator` already configured with two scopes: `Production` and `Testing`. This
is more limited, but allows you to skip some of the steps below.
See [scaffold guidance](./scaffold.md) for more details.

```kotlin
// build.gradle.kts

dependencies {
  // Add the simple module for core MapSL functionality in your main code
  implementation("com.keyboardr.mapsl:simple:<latest_version>")

  // Use this instead if you want a MainServiceLocator already configured
  implementation("com.keyboardr.mapsl:simple-scaffold:<latest_version>")

  // Add the simple-testing module for test utilities 
  testImplementation("com.keyboardr.mapsl:simple-testing:<latest_version>")

  // You'll need a mocking library for testing your code, e.g., Mockito-Kotlin
  testImplementation("org.mockito.kotlin:mockito-kotlin:<latest_version>")

  // ... other dependencies like Kotlin standard library, etc.
}
```

## 2. Understand `SimpleServiceLocator` and Class Keys

The central component in the `simple` module is `SimpleServiceLocator<S>`. You create an instance of
this class to hold and manage your services. The `<S>` type parameter allows you to associate the
service locator instance with a specific **Scope**, which can be useful for distinguishing between
different parts or environments of your application (like production vs. testing).

In `SimpleServiceLocator`, you don't create explicit key objects. Instead, the library
uses the **`KClass<T>` of the service type (`T`)** as the implicit key. This means you interact with
the locator using the service's class directly in generic functions like `put<MyService>` or
`get<UserRepository>`.

By default, `SimpleServiceLocator` handles services as lazy singletons: when you register a service
using a provider lambda, the lambda is stored and only executed the first time you request that
service using its class. The created instance is then cached and returned for all subsequent
requests for the same class from that locator instance.

## 3. Set up Your Main Service Locator

If you are using the `simple-scaffold` module, this step is already done for you.

For most applications, it's helpful to have a single, application-wide instance of
`SimpleServiceLocator` that holds all your top-level dependencies. A common pattern is to create a
singleton object (e.g., `MainServiceLocator`) to manage this instance.

You'll also create a `ServiceLocatorScope` type (usually an enum or sealed hierarchy) to define the
different environments relevant to your project. See [Scopes](scopes.md) for more detail on how
scopes are used to track the context of a `ServiceLocator`.

```kotlin
// In a file like MainServiceLocator.kt in your main source set

// Define the different possible scopes for your Service Locator instances
enum class ServiceLocatorScope { Production, Testing }

object MainServiceLocator {

  // The single instance of SimpleServiceLocator for this process
  lateinit var instance: SimpleServiceLocator<ServiceLocatorScope>
    private set // Prevent external code from replacing the instance directly

  /**
   * Initializes the MainServiceLocator with a SimpleServiceLocator instance.
   * This should be called once early in the application's lifecycle.
   *
   * @param serviceLocator The SimpleServiceLocator instance to use (typically Production or Testing).
   * @param applicationContext Application context, useful for registering context-dependent services.
   * @param registrationBlock A lambda to register services specific to this environment.
   */
  fun register(
    serviceLocator: SimpleServiceLocator<ServiceLocatorScope>,
    applicationContext: Context,
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {}
  ) {
    // Optional: Add a check to ensure registration only happens once in production
    if (serviceLocator.scope == ServiceLocatorScope.Production) {
      check(!::instance.isInitialized) { "MainServiceLocator is already initialized" }
    }

    // Store the provided locator instance
    instance = serviceLocator.apply {
      // You can register fundamental services here that are always needed
      // Example: Registering the application context
      put<ApplicationContextHolder> { ApplicationContextHolder(applicationContext) }

      // Run the environment-specific service registration provided by the caller
      registrationBlock()
    }
  }

  // Helper class to hold the application context
  private class ApplicationContextHolder(val context: Context)

  // Convenient property to access the application context via the locator
  val applicationContext: Context
    get() = instance.get<ApplicationContextHolder>().context
}
```

## 4. Initialize the Service Locator at Application Startup

If you are using the `simple-scaffold` module, this step is already done for Android. Other
platforms must still complete this step.

Call the `MainServiceLocator.register` function as early as possible in your application's entry
point to set up the main `SimpleServiceLocator` instance with the appropriate scope (e.g.,
`Production`).

For an Android application, this is typically done in the `onCreate` method of a custom
`Application` class registered in your `AndroidManifest.xml`:

```kotlin
// In your custom Application class (e.g., SampleApplication.kt)

class SampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    // Initialize MainServiceLocator with a Production scope
    // Most services will register themselves lazily upon first access
    // via the service locator delegate (see below).
    MainServiceLocator.register(
      SimpleServiceLocator(ServiceLocatorScope.Production),
      this.applicationContext, // Pass necessary initial context
    ) {
      // Exlicit registration block is often empty or minimal here.

      // Optional: register any pre-registered services
      put<ProductionApiService> { ProductionApiServiceImpl(NetworkClient.instance) }
    }
  }
}
```

For a pure JVM application, you would make this call within your `main` function.

## 5. Access and Lazily Register Your Services

### Late Registration (`getOrProvide`)

The most common way to use `SimpleServiceLocator` is to access services using a property delegate
that internally calls `getOrProvide`. This pattern allows services to **lazily register themselves**
the first time they are accessed via that property. This way you don't need to list every service in
the
`MainServiceLocator.register` block.

First, define the property delegate in a shared location (this is already done in the scaffold):

```kotlin
// In a file in your main source set, often the same file as MainServiceLocator

// Delegate for lazy service access using getOrProvide from the MainServiceLocator.
// This is the recommended delegate for typical services.
inline fun <reified T : Any> serviceLocator(
  // Define which scopes are allowed to *provide* the service via this delegate
  // if it's not already registered. Defaults to Production scope.
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it == ServiceLocatorScope.Production },
  // Configure thread safety for the lazy creation
  threadSafetyMode: LazyThreadSafetyMode = MainServiceLocator.instance.defaultThreadSafetyMode,
  // The provider lambda; receives the current scope if it runs.
  noinline provider: (ServiceLocatorScope) -> T
): ReadOnlyProperty<Any, T> = object : ReadOnlyProperty<Any, T> {
  override fun getValue(thisRef: Any, property: KProperty<*>): T =
    MainServiceLocator.instance.getOrProvide(
      allowedScopes,
      threadSafetyMode,
      provider
    )
}
```

Then, in the companion object of your service class, define a property using the `serviceLocator`
delegate. This property will be the primary way other parts of your application access the service.

```kotlin
// In a service class like MyService.kt in your main source set

// Assume NetworkClient is another service accessible via the locator.

class MyService {
  private val networkClient: NetworkClient = NetworkClient.instance

  fun fetchData() {
    println("Fetching data using: ${networkClient.getEndpoint()}")
  }

  companion object {
    // Define the single instance of MyService accessed via this property.
    // Using the serviceLocator delegate means:
    // 1. The provider lambda is NOT run until MyService.instance is FIRST accessed.
    // 2. When first accessed, getOrProvide checks if MyService is registered.
    // 3. If not registered AND the scope is allowed (Production by default),
    //    the provider lambda runs, its result (a MyService instance) is registered
    //    for MyService::class, and returned.
    // 4. Subsequent access to MyService.instance returns the same registered instance.
    val instance by serviceLocator { scope -> // The provider lambda
      println("DEBUG: Creating MyService instance in scope: $scope")
      MyService() // Create the service instance here
    }
  }
}

// In another part of your app code that needs MyService
fun performAction() {
  // Access the MyService instance via its companion object property.
  // This is where the lazy creation (if not already created) happens.
  val myService = MyService.instance
  myService.fetchData() // Use the service
}
```

This pattern makes your service declarations self-contained and promotes lazy initialization by
default. The `MainServiceLocator.register` block becomes simpler, primarily handling the
initialization of the locator itself and registration of any fundamental, eagerly needed, or
context-dependent services.

#### Interface delegation

Alternatively you can use interface delegation to reduce the amount of boilerplate further, but
this approach is a little less flexible and intuitive.
See [this example](samples/multimodule/shared/src/commonMain/kotlin/com/keyboardr/mapsl/sample/multimodule/services/BarManager.kt).

### Pre-Registration (`put`)

While the `getOrProvide` delegate pattern is common, you can still use `put` directly in the
`MainServiceLocator.register` block. This is suitable for:

- Services that **must** be created eagerly at application startup.
- Fundamental services like the application context (as shown above).
- Services that have vastly different implementations in different environments.
- Services whose creation requires complex coordination only possible during the centralized
  registration phase.
- Providing a specific pre-existing instance.

```kotlin
// Example of put used in the registration block (typically for specific cases)
MainServiceLocator.register(SimpleServiceLocator(ServiceLocatorScope.Production), this) {
  // Example: Eagerly instantiate a Configuration instance
  val settings = AppSettings(apiUrl = "[https://api.myapp.com](https://api.myapp.com)")
  put<AppSettings> { settings }

  // Example: Explicitly register a service lazily (less common than the delegate pattern)
  put<EventLogger> { DefaultEventLogger() }
}
```

> [!IMPORTANT]
> Even when a service is registered using `put`, it is a best practice to encapsulate the access
> to that service within an `instance` property (or similar getter) in the service's companion
> object. **Avoid scattering direct calls to `MainServiceLocator.instance.get<ServiceType>()`
> throughout your codebase.** This maintains a single, clear access point for the service. Ideally,
> the `put` call in the registration block and the `get` call should be introduced in the same
> commit.

```kotlin
class AppSettings(val apiUrl: String) {
  companion object {
    val instance: AppSettings
      get() = MainServiceLocator.instance.get<AppSettings>()
  }
}
```

## 7. Basic Testing Setup

The `simple-testing` module simplifies testing code that uses `SimpleServiceLocator`. It provides
`SimpleTestingServiceLocator<S>`, which automatically provides mocks for any service class that
hasn't been explicitly registered in your test setup.

1. Add `simple-testing` and a mocking library as `testImplementation` dependencies (as shown in Step
   1).
2. Create a `TestServiceLocator` subclass of `SimpleTestingServiceLocator` in your test source set.
   Implement the abstract `createMock(clazz: KClass<T>): T` function using your chosen mocking
   library (e.g., Mockito or MockK) to define how default mocks are created.
3. In your test class's setup method (annotated with `@Before` in JUnit), register your
   `TestServiceLocator` instance using `MainServiceLocator.register`. This swaps out the
   production locator for the testing one for the duration of the test. You can use the optional
   registration block here to `put` specific fake implementations or pre-configured mocks for
   services you need to control in your tests.

```kotlin
// In your test source set (e.g., com.yourcompany.yourapp.testing)
// TestServiceLocator.kt

object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {

  override fun <T : Any> createMock(clazz: KClass<T>): T {
    // Implement this to return a mock using your mocking library
    return mock(clazz.java) // Example with Mockito
  }

  // Helper function to register this testing locator
  fun register(
    applicationContext: Context = ApplicationProvider.getApplicationContext(), // Use Android test context by default
    registrationBlock: SimpleServiceLocator<ServiceLocatorScope>.() -> Unit = {}
  ) {
    // Assuming MainServiceLocator is accessible and has a public register function
    // like shown in Step 3, which allows overriding for testing.
    MainServiceLocator.register(this, applicationContext, registrationBlock)
  }
}

// In your test class (e.g., MyServiceTest.kt)
// Located in your test source set


// Assume MyService uses the serviceLocator delegate (calling getOrProvide).
// Assume MyService depends on NetworkClient, also accessed via the locator (e.g., via serviceLocator).

@RunWith(AndroidJUnit4::class) // Example test runner for Android tests
class MyServiceTest {

  @Before
  fun setUp() {
    // Register the TestServiceLocator before each test runs
    TestServiceLocator.register {
      // Use 'put' here to register specific fakes or mocks needed for THIS test suite.
      // Services accessed via their instance properties (using delegates like serviceLocator)
      // will use getOrProvide, which will check for registrations here.

      // Example: Register a mock NetworkClient for MyService to use
      put<NetworkClient>(mock {
        on { getEndpoint() } doReturn "stubbed endpoint" // Stubbing the mock's behavior
      })
    }
  }

  @Test
  fun myServiceUsesNetworkClientEndpoint() {
    // Create an instance of the service to test
    val myService = MyService()

    // Test the behavior of myService that uses NetworkClient
    myService.fetchData()

    // Verify interactions if needed
    verify(MainServiceLocator.instance.get<NetworkClient>()).getEndpoint()
  }
}
```

For a more detailed guide on testing strategies and interacting with mocks/fakes using
`simple.testing`, please see [Testing Code that Uses MapSL](./testing.md).

## SimpleServiceLocator Tips

### Type specification

When calling `SimpleServiceLocator`'s `put()`, `get()`, and `getOrProvide()` functions, it is
advisable to explicitly specify the type, even if it can be inferred. This is because a more
specific type may be inferred than was intended. For example,
`locator.getOrProvide<MyService> { MyServiceImpl() }` would register with the wrong class key if
`<MyService>` was not explicitly stated.

### Type erasure

Due to type erasure, service keys should generally not be generic types, since only the erased
type is used to determine key identity. For example, `locator.get<List<Int>>()` could potentially
return a value stored by `locator.put<List<Boolean>> { listOf(true) }`. Instead, it is recommended
to wrap these values in a reifiable service type (e.g. `ListHolder(val list: List<Int>)`). It's also
recommended to prefer storing _services_ (i.e. classes which manage state) rather than values, so
storing a List like this is of questionable utility.

### Factories

Unlike the `core` module's `ServiceLocator`, `SimpleServiceLocator` does not have a built-in
mechanism for
acting as a factory. Instead, you can register services which act as factories.

For example:

```kotlin
class FooFactory private constructor() {
  fun createFoo(): Foo = FooImpl()

  companion object {
    val instance by serviceLocator<FooFactory> { FooFactory() }
  }
}

fun getFooFromFactory() = FooFactory.instance.createFoo()
```

## Summary

You've successfully set up and used MapSL's `simple` module!

1. Add `simple` (or `simple-scaffold`) and `simple-testing` dependencies.
2. Create a `MainServiceLocator` singleton holding `SimpleServiceLocator<S>`.
3. Initialize `MainServiceLocator` at application startup with a scope, using `put` for
   fundamental services.
4. For typical services, define a `companion object val instance by serviceLocator { ... }` property
   delegate for lazy self-registration via `getOrProvide`.
5. Access services via their `instance` properties for explicitly `put` services.
6. For testing, create a `TestServiceLocator` subclassing `SimpleTestingServiceLocator` and register
   it in `@Before` methods, using `put` for specific fakes/mocks.

This covers the fundamental usage of MapSL's `simple` module, which is sufficient for many common
dependency management needs. For complete code examples, check out the
[ `samples/basic`](../samples/basic) project.
