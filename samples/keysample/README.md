# Keys Sample

This sample app demonstrates how to use the various key types using a process-level service locator.
The services are located in the `domain` subpackage. The main production service locator is in the
`locator` package and is populated by the `SampleApplication` class. A `PreviewServiceLocator` and a
`TestServiceLocator` are provided as examples where the services can be loaded in other
environments.