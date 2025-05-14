# Multimodule Sample

This project demonstrates how MapSL can be used in a multi-module environment. The project has the following modules:

- `app` - An Android application
- `desktop` - A desktop JVM application
- `shared` - Code shared between the applications
- `preview` - A special module to make it easier to make Compose previews
- `testing` - Testing utils to support unit tests

This project primarily uses `SimpleServiceLocator` and doesn't make use of advanced key types.