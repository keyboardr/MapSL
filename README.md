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
- `simple.scaffold` - Includes a basic `MainServiceLocator` when detailed scoping is not required.
- `simple.testing` - Similar to `scoped.testing`, but based on `simple`, offering testing utilities
  for the simplified API.

If you're just getting started with MapSL, it is recommended to start with the `simple` or
`simple-scaffold` module for production and `simple.testing` in your tests.
See [Simple setup](#simple-setup).

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

Learn more about the different key kinds: [Key Kinds](./docs/keys.md).

#### Parameters (`GetParams` and `PutParams`)

Each key defines types for parameters used when registering the service (`PutParams`) and retrieving
it (`GetParams`). `GetParams` is often `Unit` if no parameters are needed for retrieval. The
`PutParams` typically includes the service provider. For details on parameters for each key kind,
see [Key Kinds](./docs/keys.md).

#### Experimental keys

Some key kinds are listed as experimental. Unless otherwise noted, this is not due to any known
problems, but rather because their usage patterns haven't been fully evaluated. They are generally
safe to use in production applications. As more experience is gained with them and more feedback is
reported about them, they are expected to graduate to stable.

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

## Simple setup

For most common use cases, the recommended approach is to start with the `simple` module. This
module provides a streamlined API centered around the `SimpleServiceLocator`. In
`SimpleServiceLocator`, key types are not exposed in the API and `Lazy` `ClassKeys` are used
under the hood. Scoping is included in `SimpleServiceLocator`, as it is typically used as a
top-level, application-wide component.

In the case where multiple scopes are not needed (e.g. a single-module Android app), the
`simple-scaffold` module provides a preconfigured `MainServiceLocator`. On Android, this is also
initialized automatically on app startup. See [Scaffold](./docs/scaffold.md) for more details.

The typical architecture involves:

1. A central application-wide `MainServiceLocator` singleton instance, often a
   `SimpleServiceLocator` configured with an appropriate environment [scope](#scopes).
2. Services are primarily accessed using property delegates (like `by serviceLocator { ... }`) in
   their companion objects. This pattern leverages `SimpleServiceLocator.getOrProvide()` to lazily
   register and provide the service instance the first time it's accessed.
3. Scopes are used to differentiate environments (e.g., production vs. testing), allowing for
   conditional service provisioning.
4. Dedicated testing locators ([Test ServiceLocators](./docs/testing.md)) are used in tests to swap
   out production dependencies with mocks or fakes.

For a detailed step-by-step guide on setting up and using the `simple` module, including code
examples, see the [Getting Started Guide](docs/getting-started.md).
The [migration guide](docs/migration.md) details steps for migrating from `SimpleServiceLocator` to
the full api, including what the differences are and things to consider when deciding whether or not
to migrate.

## Testing

MapSL provides dedicated modules to simplify testing components that retrieve dependencies from a
service locator. Instead of relying on the production `ServiceLocator` with its real dependencies
and potential global state issues, you can swap it out for a test-specific locator.

Use the `simple-testing` module for applications built with `SimpleServiceLocator`, or
`scoped-testing` if you are using `ScopedServiceLocator` and explicit key types. These modules
provide testing `ServiceLocator` implementations that automatically provide mock instances for
unregistered services, making it easy to isolate the code you are testing.

In your test setup (e.g., using a `@Before` method in JUnit), you'll register a testing service
locator. You can then use `put` to register specific fake implementations or carefully configured
mocks for the dependencies you need to control for a given test.

For detailed instructions on adding the necessary dependencies, creating a testing service locator,
setting up your test environment, and working with mocks and fakes, please refer to
the [Testing Code that Uses MapSL guide](docs/testing.md).

## Samples

The following sample projects are provided:

- [Basic](samples/basic): A minimal sample project that shows the fundamentals of using
  `SimpleServiceLocator`.
- [Multi-module](samples/multimodule): A more robust sample project that shows using
  `SimpleServiceLocator` across a desktop and Android application, with a shared library between
  them.
- [Key sample](samples/keysample): Shows how the various keys in `core` and `lifecycle` can be used.

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