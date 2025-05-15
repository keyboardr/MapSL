# MapSL

MapSL is a service locator library for managing runtime dependencies in Kotlin.

The goal of MapSL is to make it easy to reference the various services in your application while
keeping the overall mechanisms involved understandable. MapSL aims to have a much easier learning
curve than other dependency management frameworks. To that end, MapSL does not use annotation
processing or code generation.

At its core, MapSL is driven by a heterogeneous map. Keys maintain a record of what type of values
they reference, and storage and retrieval of values is ensured to be of that type.

## Declaring dependencies
MapSL is not yet available on Maven Central, but will be once its initial API is stable.

## Multiplatform

MapSL is built to be multiplatform-first. Currently Android and JVM are the most well-tested, but
other platforms are supported (e.g., iOS, macOS, Native). Most of MapSL is written against the
common Kotlin standard library. Feel free to submit a pull request to add support for other
platforms.

## Module structure

MapSL consists of the following library modules:

- `core` - This contains the main `ServiceLocator` and various core key types, forming the
  foundation of the library.
- `lifecycle` - Includes `LifecycleKey` for integrating service lifetimes with AndroidX lifecycles.
  Built separately since it requires a dependency on `androidx.lifecycle`.
- `scoped` - Provides `ScopedServiceLocator` and supporting classes.
- `scoped.testing` - Provides a `ScopedServiceLocator` subclass suitable for use in tests, offering
  features like mock creation for unregistered keys.
- `simple` - Provides a more streamlined, but less powerful API for covering the common cases.
- `simple.testing` - Similar to `scoped.testing`, but based on `simple`, offering testing utilities
  for the simplified API.

If you're just getting started with MapSL, it is recommended to start with the `simple` module for
production and `simple.testing` in your tests. See [Simple setup](#simple-setup).

## Core concepts

### Keys and Entries

When a `Key` is registered with a `ServiceLocator`, it creates an `Entry` that is specific to that
kind of `Key`. Different kinds of `Keys` may have different behaviors for their `Entries`. For
example, the `Entry` for a `LazyKey` waits to instantiate its value until it has been requested,
while the `Entry` for a `SingletonKey` is initialized with its value already instantiated.

> [!NOTE]
> In this documentation a key's "kind" will refer to the class of the key itself and the behavior
> its entries have.
> 
> A key's "type" will refer to the type of values its entry produces/stores.

#### Included key kinds

| Key Kind                         | Value creation                                                                                          | Value lifetime                                   | Key equivalence    |   GetParams    |  Main PutParams  |
|----------------------------------|---------------------------------------------------------------------------------------------------------|--------------------------------------------------|--------------------|:--------------:|:----------------:|
| `LazyKey<T>`                     | Created the first time it is requested                                                                  | Stored indefinitely                              | Key instance       |       -        |    `() -> T`     |
| `SingletonKey<T>`                | Stored when key is registered                                                                           | Stored indefinitely                              | Key instance       |       -        |       `T`        |
| `ClassKey<T>`                    | `Lazy` or `Singleton`                                                                                   | Stored indefinitely                              | Same reifiable `T` |       -        | `() -> T` or `T` |
| `FactoryKey<T>` (experimental)   | New value for every request                                                                             | Not stored                                       | Key instance       |       -        |    `() -> T`     |
| `LifecycleKey<T>` (experimental) | Created the first time it is requested and the first time after all its requests' lifecycles have ended | Stored until all requests' lifecycles have ended | Key instance       | LifecycleOwner |    `() -> T`     |

#### GetParams and PutParams

Each `Key` kind requires specific parameters when registering a key (a "put") or fetching its
associated value (a "get"). Most keys do not require parameters when fetching, which is indicated by
a `GetParams` type of `Unit`. The `PutParams` define the mechanism for producing service values,
either by specifying the value itself or a producer function. The `PutParams` may also include
additional information governing the behavior of the entry (e.g. `LazyKey`'s includes a
`LazyThreadSafetyMode` to specify its multi-threading behavior)

The `put()` function in `ServiceLocator` takes a key and the key's corresponding `PutParams` type.
Likewise, the `get()` function in `ServiceLocator` takes a key and the key's corresponding
`GetParams`.

```kotlin
val key = LazyKey<MyService>()
serviceLocator.put(key) { MyServiceIml() }
```

> [!Note]
> in reality `LazyKey`'s `PutParams` is a class to allow for additional parameters, but we'll
> assume it's just the lambda here for simplicity. `LazyKey` provides an extension function on
> `ServiceLocator` to allow for this syntax.

There is an extension function for the common case where the key's `GetParams` are `Unit`, so the
service can be obtained without explicitly passing `Unit`.

```kotlin
val service = serviceLocator.get(key)
```

### Scopes

There is nothing intrinsic to `ServiceLocator` that requires it to be a singleton or top-level
component. Applications may find it useful to pass `ServiceLocator` instances around for use in
smaller scopes. However, when used as a top-level component, it is frequently useful to determine
the environment in which the ServiceLocator was created. This could include distinguishing between
testing and production environments, or identifying the specific process in a multi-process
application.

For this reason, `ScopedServiceLocator<S>` takes a `scope: S` argument during creation to track this
information. It is common for the `scope` type `S` to be part of a `sealed` hierarchy (i.e., a `sealed
class` or `sealed interface`) so that all possible scope instances are known and can be handled
exhaustively. This allows for creating provider functions whose behavior depends on the `scope`.

The `getOrProvide()` function is one such example. It accepts a predicate `(S) -> Boolean` to
determine whether to allow registering a new entry if one is not found, or to instead invoke the
`ScopedServiceLocator`'s `onInvalidScope()` function. For example, this allows for normal service
registration in production environments while automatically providing mocks in testing environments.

#### Test ServiceLocators

Testing `ServiceLocator` classes are provided that are configured for use during unit tests. If a
value is requested of them for a key that has not yet been registered, they will return a mock
rather than throwing an exception. These classes are designed to be independent of any specific
mocking framework. They provide an abstract `createMock(KClass<T>)` function you must implement to
provide the desired mock or fake instances.

Example implementations:

```kotlin
// Mockito
object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {
  override fun <T : Any> createMock(clazz: KClass<T>): T = mock<T>(clazz.java)

  fun register() {
    ProcessServiceLocator.register(this, ApplicationProvider.getApplicationContext()) {
      // common fakes go here
    }
  }
}

// MockK
object TestServiceLocator :
  SimpleTestingServiceLocator<ServiceLocatorScope>(ServiceLocatorScope.Testing) {
  override fun <T : Any> createMock(clazz: KClass<T>): T = mockkClass<T>(clazz)

  fun register() {
    ProcessServiceLocator.register(this, ApplicationProvider.getApplicationContext()) {
      // common fakes go here
    }
  }
}
```

## Simple setup (recommended)

While the core library's support for multiple key kinds provides flexibility, it can introduce API
complexity. To address this, a simplified module (`simple`) is provided. It offers a streamlined API
that should cover the vast majority of common use cases, while still allowing for a straightforward
migration to the full core library if more advanced features are required.

In `SimpleServiceLocator`, key types are not exposed in the API and `Lazy` `ClassKeys` are used
under the hood. Scoping is included in `SimpleServiceLocator`, as it is typically used as a
top-level, application-wide component.

### Recommended pattern

The following is the recommended architecture for using MapSL. See the [basic sample](samples/basic)
for a complete example.

#### ProcessServiceLocator

The recommended use of MapSL is to have a centralized singleton to hold the process's top-level
`SimpleServiceLocator`. It should include a `register()` function to set this value. The
`register()` function may include parameters necessary in all environments to pre-register common
services. It may also include a `SimpleServiceLocator.() -> Unit` lambda parameter to perform
environment specific pre-registration.

#### ServiceLocatorScopes

Define a scope type (either as an `enum` or as a `sealed interface`) for the top-level application
scope. Include a `Testing` scope to be used in tests, and a `Production` object or interface for the
production scope(s).

#### TestServiceLocator

A `TestServiceLocator` should be created that extends `TestingSimpleServiceLocator` to provide mocks
and fakes in tests. This should be in a separate compilation unit from your production classes
(either in `commonTest` or in a separate `testing` module). It should include a `register()`
function to register it as the `ProcessServiceLocator`. The `register()` function may take
additional arguments, but keep the number of required arguments low since this method will be called
in most test classes. It may also include a `SimpleServiceLocator.() -> Unit` lambda parameter to
perform test-specific pre-registration.

#### ServiceLocator Registration

When the application starts (e.g. at the start of `main()` (jvm) or in `Application.onCreate()` (
Android)), call the `ProcessServiceLocator.register()` function to populate `ProcessServiceLocator`.

For tests, call `TestServiceLocator.register()` within a test setup function (e.g., annotated with
`@Before` in JUnit) to ensure the `TestServiceLocator` is configured to provide the necessary
services (including mocks).

#### Services

There are two common approaches for providing service access: pre-registration, and late
registration.

##### Pre-registration

With pre-registration, you register the service's provider during `ProcessServiceLocator.register()`
using `SimpleServiceLocator.put()`. The service's companion object may then expose an `instance`
property that retrieves the service via `SimpleServiceLocator.get()`. This approach is suitable when
service instance creation requires tight coordination with other services, as registration provides
a centralized point for this coordination. It is also suitable for eagerly loading the service, as
the instance can be created directly and passed to `put()` instead of using a lazy provider lambda.
This is also the recommended approach if there are multiple environments (e.g. different
applications) which will provide different implementations.
> [!Tip]
> By adding your pre-registration in the same commit as declaring the `instance` property, you can
> ensure that there is no way to call `get()` for a key that has not been registered. As a best
> practice, these `instance` properties should be the only place you ever call `get()`.

##### Late registration

Late registration is simpler and should be used when pre-registration is not necessary. In the
service's companion object, expose a `val instance` property that utilizes
`SimpleServiceLocator.getOrProvide()` to retrieve the service. This will register and instantiate
the service the first time it is requested. It is common practice to create a property delegate to
encapsulate this behavior.
See [this example](samples/basic/src/main/java/com/keyboardr/mapsl/sample/basic/ServiceLocatorDelegate.kt).

Alternatively you can use interface delegation to reduce the amount of boilerplate further, but this
approach is a little less flexible and intuitive.
See [this example](samples/multimodule/shared/src/commonMain/kotlin/com/keyboardr/mapsl/sample/multimodule/services/BarManager.kt).

##### Type specification

When calling `SimpleServiceLocator`'s `put()`, `get()`, and `getOrProvide()` functions, it is
advisable to explicitly specify the type, even if it can be inferred. This is because a more
specific type may be inferred than was intended. For example,
`locator.getOrProvide<MyService> { MyServiceImpl() }` would register with the wrong class key if
`<MyService>` was not explicitly stated.

##### Type erasure

Due to type erasure, service keys should generally not be generic types, since only the erased
type is used to determine key identity. For example, `locator.get<List<Int>>()` could potentially
return a value stored by `locator.put<List<Boolean>> { listOf(true) }`. Instead, it is recommended
to wrap these values in a reifiable service type (e.g. `ListHolder(val list: List<Int>)`). It's also
recommended to prefer storing _services_ (i.e. classes which manage state) rather than values, so
storing a List like this is of questionable utility.

##### Factories

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

## Samples

The following sample projects are provided:

- [Basic](samples/basic): A minimal sample project that shows the fundamentals of using
  `SimpleServiceLocator`.
- [Multi-module](samples/multimodule): A more robust sample project that shows using
  `SimpleServiceLocator` across a desktop and Android application, with a shared library between
  them.
- [Key sample](samples/keysample): Shows how the various keys in `core` and `lifecycle` can be used.

## Service Locator vs Dependency Injection

MapSL is designed as a Service Locator, an architectural pattern that provides a central registry of
services available to an application. While Service Locators and Dependency Injection (DI)
frameworks both address the problem of managing dependencies, they do so through different
mechanisms. DI frameworks typically invert control, injecting dependencies into components rather
than the component explicitly requesting them. This often involves complex setup using annotations,
code generation, or extensive configuration. MapSL, on the other hand, allows components to
explicitly request their dependencies from the service locator. This approach simplifies the setup
process, makes the flow of dependencies more explicit within the code, and avoids the need for
external tools like annotation processors, leading to a faster build times and a shallower learning
curve compared to many DI solutions. MapSL aims to strike a balance, offering the ease of use and
explicitness of a Service Locator while still providing the benefits of centralized dependency
management often associated with DI.

### Main drawbacks compared to DI (and how MapSL addresses them)

- A class's dependencies are not immediately obvious from its constructor. This is intrinsic to any
  Service Locator approach, and MapSL doesn't fundamentally alter this. However, there are other
  places where a class's dependencies are listed (e.g imports or module dependencies), and these are
  often sufficient for identifying what a class depends on. More commonly, however, it is important
  for consumers of a class to know how to obtain an instance. The patterns recommended by MapSL make
  this clear and explicit.
    - If a clear list of dependencies is still desired, your team can establish a best practice of
      only accessing the `instance` properties from the default arguments of a constructor. This
      would provide some of the same benefits as constructor-based dependency injection without
      exposing transitive dependencies.
- Service Locators often make testing difficult by relying on global state. MapSL addresses this by
  allowing the top-level ServiceLocator to be swapped out for a test instance that is already
  configured with your project's common fakes, and provides mocks when no fake has been registered.
  Additionally, smaller scoped ServiceLocators may be passed in to classes as needed, resulting in a
  hybrid approach (hierarchical `ServiceLocators` are possible, which would help here. These would
  behave similarly to Context objects. See [Future Plans](#future-plans))).
- Service Locators sometimes suffer from a lack of runtime safety due to separation between the
  declaration and the resolution of dependencies. The [late registration](#late-registration)
  significantly reduces this risk. It is still possible to fail resolution
  with [Pre-registration](#pre-registration), but the best practice of coupling the `put()` and the
  `get()` functions in the same commit mitigates this. Both approaches can still potentially run
  into problems with circular dependencies when using lazy keys, but these are rare in practice (and
  are mitigated in build systems that prefer tightly scoped modules, e.g. Bazel).

## Contributing

We welcome contributions to MapSL! Whether it's reporting a bug, suggesting a new feature, or
submitting a pull request, your input is valuable.

Here are a few guidelines to help you get started:

1. Reporting Bugs: If you find a bug, please open an issue on the GitHub repository. Provide a clear
   description of the problem, steps to reproduce it, and the expected behavior.

2. Suggesting Features: Have an idea for a new feature or improvement? Open an issue to discuss your
   proposal. Explain the problem it solves and how you envision the solution.

3. Submitting Pull Requests:

    - Fork the repository and create a new branch for your changes.

    - Ensure your code adheres to the project's coding style (if any is defined).

    - Write clear and concise commit messages.

    - Include tests for your changes.

    - Submit your pull request with a description of the changes and the issue it addresses.

We appreciate your help in making MapSL better!

## Future Plans

MapSL is actively being developed, and here are some areas we plan to explore in the future:

- **Expanded Platform Support**: Further testing and refinement of support for additional Kotlin
  Multiplatform targets (e.g., iOS, macOS, JS, Native).

- **Hierarchical ServiceLocators**: Provide explicit support for smaller-scoped service locators
  which may fall back to parent locators for missing keys. These will become more useful once
  [context parameters](https://github.com/Kotlin/KEEP/blob/master/proposals/context-parameters.md)
  are stable in Kotlin.

- **Lint checks**: Provide warnings and errors when recommended practices are not followed.
    - `put()` outside of normal pre-registration
    - `get()` with no corresponding `put()`
    - Compile-time circular dependency detection

- **Service materialization**: Add option to eagerly instantiate all services at the end of
  registration. This would only work for pre-registered services, but would identify circular
  dependencies and other instantiation issues quicker. This would make application start up slower,
  so we would recommend against this in release builds.

- **New Key Kinds**: Exploring the possibility of adding new built-in `Key` kinds to support
  different dependency management patterns.

- **Documentation and Examples**: Creating more in-depth documentation and additional sample
  projects to demonstrate various use cases and advanced features.

- **Community Feedback**: Incorporating feedback from users to guide future development priorities.

This is not an exhaustive list, and the roadmap may evolve based on community needs and
contributions.

## License

This project is licensed under the [MIT License](LICENSE).