# MapSL Flavors

MapSL comes in 3 flavors: `core`, `simple`, and `simple-scaffold`.

### Core

`Core` is the most flexible, but has a larger API surface. It has the ability to use different [key
kinds](./keys.md) to achieve different behaviors for each entry. It is a very powerful flavor, but
is probably overkill for most application needs.

### Simple

`Simple` is designed for the common case where lazy singleton initialization is the main behavior
needed, and where entries can be identified by their class type alone. It is built on top of `core`,
and completely encapsulates the `core` API.

The [getting started](./getting-started.md) guide mainly uses this flavor.

### Scaffold

`Simple-scaffold` includes extra components most applications will need when using the standard
architecture for `simple`. As such, the `simple` API is still exposed. While this reduces the
boilerplate needed to use `simple`, it loses the ability to define different [scopes](./scopes.md)
for your project. Instead it has only two: `Production`, and `Testing`. This still allows for the
most important functionality, such as late registration, automatic mocking, and multiplatform
support.

It is described in more detail in the [scaffold](./scaffold.md) guide.

## How to decide which to use

`Core` should only be used when the more complex behaviors of key kinds are needed. Even then, many
of the key kind behaviors can be replicated using `simple`. Unless you intend to make heavy use of
those other behaviors, `simple` is probably a better choice.

`Scaffold` can be used to get up and running with MapSL faster, but moderately sized projects may
wish to incorporate the use of scopes (especially multiplatform or multiprocess applications).
Additionally, by omitting the scaffold you have more flexibility in how your MainServiceLocator's
registration works. For example, you may wish to have some core classes (such as an Application
Context, Clock, or background Executor) as parameters to `register()` to ensure they are eagerly
available in all scopes, including tests.

| Flavor     | [Key Kinds](./keys.md) | [Scopes](./scopes.md)    | Late Registration | Pre-Registration | Automatic Mocks                                                                           |
|------------|------------------------|--------------------------|-------------------|------------------|-------------------------------------------------------------------------------------------|
| `Core`     | All Kinds              | App defined              | Supported         | Supported        | via `testing` module                                                                      |
| `Simple`   | Lazy Class keys only   | App defined              | Supported         | Supported        | via `simple-testing` module                                                               |
| `Scaffold` | Lazy Class keys only   | `Producation`, `Testing` | Supported         | Not recommended  | If `MockFactory` is provided to `TestServiceLocator`. No other module dependency required |

## Switching between flavors

Switching from `scaffold` to `simple` is fairly straightforward. You can copy most of the scaffold
into your project and then just change your imports to the new package. From there you can make
whatever modifications you need. On Android, the `ServiceLocatorInitializer` should only be copied
over if you intend to use the `App Startup library` from Jetpack. Otherwise perform the registration
in your `Application` class's `onCreate()`.

Switching from `simple` to `core` is also pretty easy, but has more steps since the APIs are
different. See the [migration guide](./migration.md) for detailed guidance.