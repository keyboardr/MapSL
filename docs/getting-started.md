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
- **Easy migration:** Built on top of the `core` and `scoped` modules, so migration is easy if more
  power or flexibility is
  needed (see [Migration](migration.md)).

## 1. Add Dependencies

<a href="https://repo1.maven.org/maven2/dev/keyboardr/mapsl/"><img alt="Maven Central" src="https://img.shields.io/maven-metadata/v?metadataUrl=https%3A%2F%2Frepo1.maven.org%2Fmaven2%2Fdev%2Fkeyboardr%2Fmapsl%2Fcore%2Fmaven-metadata.xml"/></a>

To use the `simple` flavor, add the `simple` module dependency to your main source set and the
`simple-testing` dependency to your test source set in your project's `build.gradle.kts` file.
You'll also need a mocking library for testing.

> **Tip:** You can use the `simple-scaffold` module instead of `simple` if you would like a basic
`MainServiceLocator` already configured. This allows you to skip some of the setup steps below.
> See the [scaffold guide](./scaffold.md) for more details.

```kotlin
// build.gradle.kts

dependencies {
  // Add the simple module for core MapSL functionality in your main code
  implementation("dev.keyboardr.mapsl:simple:<latest_version>")

  // Add the simple-testing module for test utilities 
  testImplementation("dev.keyboardr.mapsl:simple-testing:<latest_version>")

  // You'll need a mocking library for testing your code, e.g., Mockito-Kotlin
  testImplementation("org.mockito.kotlin:mockito-kotlin:<latest_version>")

  // ... other dependencies
}
```

## 2. Set up Your Main Service Locator

For most applications, it's helpful to have a single, application-wide instance of
`SimpleServiceLocator`. A common pattern is to create a singleton object (e.g.,
`MainServiceLocator`) to manage this instance.

You'll also create a `ServiceLocatorScope` type (usually an enum or sealed hierarchy) to define the
different environments relevant to your project. See [Using Scopes](scopes.md) for more
detail.

```kotlin
// In a file like MainServiceLocator.kt

// Define the different possible scopes for your Service Locator instances
enum class ServiceLocatorScope { Production, Testing }

object MainServiceLocator {

  // The single instance of SimpleServiceLocator for this process
  lateinit var instance: SimpleServiceLocator<ServiceLocatorScope>
    private set // Prevent external code from replacing the instance directly

  /**
   * Initializes the MainServiceLocator.
   * This should be called once early in the application's lifecycle.
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
      // Use a private holder to store the context, keeping it an implementation detail
      put<ApplicationContextHolder> { ApplicationContextHolder(applicationContext) }

      // Run any other environment-specific registrations
      registrationBlock()
    }
  }

  private class ApplicationContextHolder(val context: Context)

  // Convenient property to access the application context
  val applicationContext: Context
    get() = instance.get<ApplicationContextHolder>().context
}
```

## 3. Initialize at Application Startup

Call `MainServiceLocator.register()` as early as possible in your application's entry
point. For an Android app, this is typically in the `onCreate()` method of a custom `Application`
class. For a pure JVM application, you would make this call within your `main` function.

```kotlin
// In your custom Application class

class SampleApplication : Application() {
  override fun onCreate() {
    super.onCreate()

    // Initialize MainServiceLocator with a Production scope
    MainServiceLocator.register(
      SimpleServiceLocator(ServiceLocatorScope.Production),
      this.applicationContext
    ) {
      // The registration block is often empty here, as most services
      // will be registered lazily upon first access.
    }
  }
}
```

## 4. Define and Access Your Services

The recommended way to define and access services is with a property delegate that lazily registers
the service on its first use. This is known as **late registration**.

First, define a reusable property delegate:

```kotlin
// In a shared file, e.g., ServiceLocatorDelegate.kt

inline fun <reified T : Any> serviceLocator(
  noinline allowedScopes: (ServiceLocatorScope) -> Boolean = { it == ServiceLocatorScope.Production },
  threadSafetyMode: LazyThreadSafetyMode = MainServiceLocator.instance.defaultThreadSafetyMode,
  noinline provider: (ServiceLocatorScope) -> T,
): ReadOnlyProperty<Any, T> = ReadOnlyProperty { _, _ ->
  MainServiceLocator.instance.getOrProvide(
    allowedScopes,
    threadSafetyMode,
    provider
  )
}
```

Then, use this delegate in the companion object of your service class. This property becomes the
single, clear access point for your service.

```kotlin
// In your service class, e.g., MyService.kt

class MyService private constructor(private val networkClient: NetworkClient) {
  fun fetchData() {
    println("Fetching data using: ${networkClient.getEndpoint()}")
  }

  companion object {
    // The provider lambda runs only when `MyService.instance` is first accessed.
    // The created instance is then stored in the locator and returned on subsequent calls.
    val instance by serviceLocator {
      // Dependencies can be retrieved from the locator here
      val networkClient = NetworkClient.instance
      MyService(networkClient)
    }
  }
}

// In another part of your app
fun doSomething() {
  // Access the service via its instance property.
  // This triggers the lazy creation and registration if it hasn't happened yet.
  MyService.instance.fetchData()
}
```

> Services written in Java can expose a static accessor function as follows:
> ```java
> public static MyService getInstance() {
>   return MainServiceLocator.getInstance().getOrProvide(MyService.class, () -> new MyService());
> }
> ```

### Pre-Registration

While late registration is recommended, you can still pre-register services directly in the
`MainServiceLocator.register` block. This is useful for services that must be created eagerly at
startup.

```kotlin
// In your Application.onCreate()
MainServiceLocator.register(...) {
  // Eagerly register a service
  put<AnalyticsService> { ProductionAnalyticsService() }
}
```

Even when pre-registering, it is best practice to define an `instance` property in the service's
companion object to provide a single, consistent access point.

```kotlin
class AnalyticsService {
  companion object {
    val instance: AnalyticsService
      get() = MainServiceLocator.instance.get()
  }
}
```

## 5. Testing Your Code

The `simple-testing` module provides `SimpleTestingServiceLocator<S>`, which automatically provides
mocks for any service that hasn't been explicitly registered in your test setup.

For a comprehensive guide, see [Testing Code that Uses MapSL](./testing.md).

Here is a brief overview:

1. **Create a `TestServiceLocator`**: In your test source set, create a test locator that knows how
   to create mocks.
   ```kotlin
   // In src/test/kotlin
   object TestServiceLocator :
     SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {
     override fun <T : Any> createMock(clazz: KClass<T>): T {
       // Use your preferred mocking library
       return mock(clazz.java)
     }
   }
   ```

2. **Register it in Test Setup**: In your test class's setup method (e.g., annotated with
   `@Before`), register your `TestServiceLocator`.

   ```kotlin
   @RunWith(AndroidJUnit4::class)
   class MyServiceTest {
     @Before
     fun setUp() {
       // Swap the production locator with the test one
       MainServiceLocator.register(
         TestServiceLocator,
         ApplicationProvider.getApplicationContext()
       ) {
         // You can `put` specific fakes or pre-configured mocks here
         put<NetworkClient>(mock {
           on { getEndpoint() } doReturn "fake-endpoint"
         })
       }
     }
   
     @Test
     fun testMyService() {
       // MyService.instance will now get the fake NetworkClient
       MyService.instance.fetchData()
       // ...
     }
   }
   ```

## SimpleServiceLocator Tips

- **Type Specification**: When calling `put`, `get`, or `getOrProvide`, it's best to explicitly
  specify the type (e.g., `get<MyService>()`) even if it seems it can be inferred, to avoid
  ambiguity.

- **Type Erasure**: Avoid using generic types like `List<String>` as keys, as type erasure means
  `get<List<Int>>()` could resolve to a `List<String>`. Instead, wrap them in a non-generic class (
  e.g., `StringListHolder(val list: List<String>)`).

- **Factories**: The `simple` module does not have a built-in factory mechanism. Instead, you can
  register a service that acts as a factory.

   ```kotlin
   class ItemFactory {
     fun createItem(): Item = Item()
     companion object {
       val instance by serviceLocator { ItemFactory() }
     }
   }
   
   // Usage: ItemFactory.instance.createItem()
   ```

## Summary

You've now seen the fundamentals of setting up and using MapSL's `simple` module. By creating a
central `MainServiceLocator` and using property delegates for lazy registration, you can manage your
dependencies in a clean, testable, and easy-to-understand way.
