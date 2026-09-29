# BoxLang Azure Functions Runtime - AI Development Guide

## Project Overview

This is a **BoxLang Azure Functions Runtime** that enables running BoxLang code in Microsoft Azure Functions. The runtime acts as a bridge between the Azure Functions Java worker and BoxLang's dynamic language features.

### Core Architecture

- **Entry Point**: `ortus.boxlang.runtime.azure.AzureFunctionRunner` - a single `@FunctionName`-annotated method wired to a wildcard `@HttpTrigger`
- **Route Resolution**: Only files registered under `handlers/` (or listed in a build-time `manifest.json`) are ever eligible routing targets. Falls back to `Lambda.bx` when no route matches. See `AzureFunctionRunner#resolveRoute`.
- **Response Pattern**: Functions can return data directly OR populate a `response` struct with `statusCode`, `headers`, `body`, and `cookies`
- **BoxLang Integration**: Uses the BoxLang runtime for dynamic compilation and execution of `.bx` files

## Essential Development Patterns

### Azure Function Structure

BoxLang Azure functions follow this pattern in any `.bx` file:
```boxlang
class {
    function run( event, context, response ) {
        // Option 1: Use response struct
        response.statusCode              = 200;
        response.headers["Content-Type"] = "application/json";
        response.body = serializeJSON( { "message": "Hello World" } );

        // Option 2: Just return data (auto-populated in response.body)
        return "Hello World";
    }
}
```

### Route Resolution

The runtime builds a routing table once, at cold start, in this order:

1. `manifest.json` at the function root - the build-time-generated source of truth. No filesystem scanning when present and valid.
2. A one-time scan of `handlers/`, if the manifest is missing/invalid but the directory exists. Supports nested routes, matched case-insensitively.
3. A one-time scan of the function root itself, for backward compatibility. `Application.bx` and the default handler class are always excluded.

Examples: `handlers/Products.bx` → `/products`, `handlers/api/Test.bx` → `/api/test`. Hyphens in the URI map to PascalCase filenames (`/user-profiles` → `UserProfiles.bx`).

All `.bx` handler files live under the function root (`BOXLANG_AZURE_ROOT`, defaulting to the Azure-provided `AzureWebJobsScriptRoot`, else `/home/site/wwwroot`).

### Environment Variables

- `BOXLANG_AZURE_ROOT`: Root directory for `.bx` files (default: `AzureWebJobsScriptRoot`, else `/home/site/wwwroot`)
- `BOXLANG_AZURE_CLASS`: Override default `Lambda.bx` file path
- `BOXLANG_AZURE_DEBUGMODE`: Enable verbose debug logging (also disables handler-class caching)
- `BOXLANG_AZURE_CONFIG`: Custom BoxLang config path (defaults to `boxlang.json` in root)

### Build System (Gradle)

**Key Commands:**
- `./gradlew shadowJar` - Build the fat JAR published to Maven Central (bundles BoxLang core)
- `./gradlew buildMainZip` - Create a deployable zip with `.bx` files + runtime JAR
- `./gradlew buildTestZip` - Create test package
- `./gradlew azureFunctionsRun` - Start the local Azure Functions dev server (via Core Tools, official plugin task)
- `./gradlew azureFunctionsDeploy` - Deploy directly to Azure (reads `AZURE_SUBSCRIPTION_ID`, `AZURE_RESOURCE_GROUP`, `AZURE_FUNCTION_APP_NAME`, `AZURE_REGION` from the environment)

**Important Build Facts:**
- Uses the official `com.microsoft.azure.azurefunctions` Gradle plugin for local run/deploy; it stages `host.json` + the runtime classpath and generates `function.json` from the `@FunctionName`/`@HttpTrigger` annotations automatically - `function.json` is never hand-maintained.
- `host.json`'s `http.routePrefix` is set to `""` so `request.getUri().getPath()` has no runtime-specific prefix to strip, matching the AWS/GCF path shape exactly.
- Uses the shadow plugin separately for the single fat-jar artifact published to Maven Central (matches the AWS/GCF runtimes' packaging convention) - unrelated to the Azure deployment packaging above.
- Automatically handles BoxLang dependencies (local or downloaded)
- Branch-aware versioning: `development` branch appends `-snapshot`
- Generates checksums (SHA-256, MD5) for all artifacts

### Dependency Management

**Local Development Setup:**
- If `../boxlang/build/libs/boxlang-{version}.jar` exists, uses the local BoxLang build
- Otherwise downloads dependencies to `src/test/resources/libs/`
- Web support included via `boxlang-web-support` dependency

**Key Dependencies:**
- `com.microsoft.azure.functions:azure-functions-java-library` - Azure Functions annotations and types (`HttpRequestMessage`, `HttpResponseMessage`, `ExecutionContext`)
- `org.slf4j:slf4j-nop` - Logging (no-op)
- BoxLang runtime and web support JARs

### Testing Patterns

**Unit Tests Location:** `src/test/java/ortus/boxlang/runtime/azure/`
- `AzureFunctionRunnerTest` - Main integration tests
- `HandlerCacheTest` - Compiled class cache tests
- `AzureRequestMapperTest` / `AzureResponseMapperTest` - Mapper unit tests
- `RouteResolutionTest` - URI-to-handler routing tests
- Mock `.bx` handlers and fixtures live in `src/test/resources/`
- Mock Azure types (`MockHttpRequestMessage`, `MockHttpResponseMessage`, `MockExecutionContext`) live in `src/test/java/ortus/boxlang/runtime/azure/mocks/`
- Use Google Truth assertions: `assertThat(...)`

### BoxLang-Specific Considerations

**Runtime Initialization:**
- Static initialization of the BoxLang runtime for performance (once per worker process)
- Stateless execution model (uses the system temp directory, since the deployed wwwroot is read-only)
- Dynamic class loading and compilation of `.bx` files
- Compiled class caching via `ConcurrentHashMap` to avoid recompilation on warm invocations

**Method Resolution:**
- Default method: `run`
- Custom method via `x-bx-function` request header
- `event`, `context`, and `response` parameters are automatically injected

**Context Struct Fields (Azure-specific):**
- `context.functionName` - from `ExecutionContext.getFunctionName()`
- `context.invocationId` - from `ExecutionContext.getInvocationId()`
- `context.requestId` - alias of `invocationId`, for naming parity with the AWS/GCF context structs

## Common Workflows

1. **New Route Handler**: Create a new `.bx` file under `handlers/` (e.g., `handlers/Orders.bx` → accessible via `/orders`)
2. **Local Testing**: Run `./gradlew azureFunctionsRun`
3. **Building**: Always run `shadowJar` before `buildMainZip` for deployment
4. **Debugging**: Set `BOXLANG_AZURE_DEBUGMODE=true`

## Performance Best Practices

- **Class Caching**: Handler classes are automatically cached between invocations
- **Static Initialization**: Use `static {}` blocks for expensive one-time setup
- **Connection Reuse**: Store DB connections and HTTP clients as class variables
- **Early Returns**: Validate input early and return immediately on errors
- **Cold Start Optimization**: Keep initialization code minimal and cache aggressively

## File Locations to Remember

- Main runtime: `src/main/java/ortus/boxlang/runtime/azure/AzureFunctionRunner.java`
- Request/response mappers: `src/main/java/ortus/boxlang/runtime/azure/AzureRequestMapper.java`, `AzureResponseMapper.java`
- `host.json`: `src/main/resources/host.json`
- Build config: `build.gradle` (shadow plugin + `com.microsoft.azure.azurefunctions` plugin configuration)
- Version info: `gradle.properties`
