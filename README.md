> [!CAUTION]
> This extension was created for educational purposes and has undergone no testing. It is neither supported nor endorsed by PTC.

# ThingWorx Service Executor extension

This extension allows to define synchonous Script Services that will be executed in their own thread. It was created to run a Script Service within its own transaction and workaround ThingWorx automatic transaction handling.

### Key capabilities

- The Service execution can time out and be interrupted
- Every invocation of the Service operates within its own transaction

### Usage

1. Import the thingworx-executor-extension Extension
2. Create a Thing that extends `ServiceExecutorTemplate`
3. Define a synchronous service on that thing (with no restrictions)
4. Invoke that service like any other Service (from REST, WS , another Service Script, ...)
  * Each ServiceExecutor thing starts an FixedThreadPool Executor. Limit the number of ServiceExecutor things if you don't want to spawn too many threads. 
      * the thread pool count is configurable from the thing's Configuration tab (default=3).
  * The services execution is blocking with timeout. The same timeout is used for all the services on that thing.
    * the timeout is configurable from the thing's Configuration tab (default=30sec).

### Implementation info

`ServiceExecutorTemplate` is just overriding the `processServiceRequest()` and `processAPIServiceRequest()` methods
