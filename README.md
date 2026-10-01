# ⚡︎ BoxLang Azure Functions Runtime

```
|:------------------------------------------------------:|
| ⚡︎ B o x L a n g ⚡︎
| Dynamic : Modular : Productive
|:------------------------------------------------------:|
```

<blockquote>
	Copyright Since 2023 by Ortus Solutions, Corp
	<br>
	<a href="https://www.boxlang.io">www.boxlang.io</a> |
	<a href="https://www.ortussolutions.com">www.ortussolutions.com</a>
</blockquote>

<p>&nbsp;</p>

## 🚀 Welcome to the BoxLang Azure Functions Runtime

This repository contains the **core Azure Functions Runtime** for the BoxLang language. This runtime acts as a bridge between Azure's Java 21 worker and BoxLang's dynamic language features, enabling BoxLang code execution in serverless environments on Microsoft Azure.

**✨ Key Features:**

- **🎯 Convention-Based URI Routing** - Route requests to registered handler classes based on URI paths
- **⚡ Performance Optimized** - Class compilation caching for warm invocations
- **☁️ Azure Native** - Built on the official Azure Functions Java library, with the official Gradle plugin for local run and deploy
- **🧪 Developer Friendly** - Live reloading in debug mode
- **🔄 Cloud-Agnostic Code** - `.bx` handler files written for this runtime run unmodified on the AWS Lambda and Google Cloud Functions runtimes too

> 💡 **For creating Azure Functions projects**: Use our [BoxLang Azure Functions Template](https://github.com/ortus-boxlang/boxlang-starter-azure-functions) to quickly bootstrap new serverless applications.

## 🏗️ Architecture Overview

The runtime consists of:

- **`ortus.boxlang.runtime.azure.AzureFunctionRunner`** - The single Azure Function entry point (`@FunctionName` + wildcard `@HttpTrigger`)
- **`AzureRequestMapper`** - Converts `HttpRequestMessage` → BoxLang event struct
- **`AzureResponseMapper`** - Builds the Azure `HttpResponseMessage` from a BoxLang response struct
- **Dynamic Class Compilation** - Compiles `.bx` files on-demand with intelligent caching
- **Convention-based Routing** - Routes requests to a `handlers/`-registered `.bx` file, falling back to `Lambda.bx`

### Runtime Flow

1. **Static Initialization** - BoxLang runtime loads once per worker process
2. **URI-based Class Resolution** - Routes requests against a routing table built once at cold start
3. **Class Compilation** - `.bx` files are compiled and cached for warm invocations (skipped in debug mode)
4. **Method Resolution** - Discovers target method via convention or `x-bx-function` header
5. **Application Lifecycle** - Full `Application.bx` lifecycle with `onRequestStart`/`onRequestEnd`
6. **Response Marshalling** - Converts the BoxLang response struct to Azure's `HttpResponseMessage`

## 🎯 URI Routing with `handlers/`

Only files under a `handlers/` directory (or listed in a build-time `manifest.json`) are ever eligible routing targets. `Application.bx` and the default `Lambda.bx` are never routable, regardless of what's on disk.

### Resolution order (built once, at cold start)

1. **`manifest.json`** at the function root, if present and valid — the build-time-generated source of truth. No filesystem scanning happens when this is present.
2. **A one-time scan of `handlers/`**, if the manifest is missing or invalid but the directory exists. Supports nested routes, matched case-insensitively.
3. **A one-time scan of the function root itself**, for backward compatibility with deployments that predate the `handlers/` convention — gated behind `BOXLANG_ENABLE_ROOT_SCAN` (default `true`; set to `false` to disable this fallback and restrict routing to the default `Lambda.bx` handler only). `Application.bx` and the default handler class are always excluded.

Tiers 2 and 3 log a `WARNING` listing every handler they registered, so a missing or corrupt manifest is never a silent surprise.

### Application Lifecycle

The project's root `Application.bx` fires for **every** invocation — `onApplicationStart()` once per cold start, `onRequestStart()` before each request — regardless of whether `Lambda.bx` or a routed handler under `handlers/` ends up serving it. There's a single `Application.bx` per deployment, at the function root, never under `handlers/`.

`run()`, `onRequestEnd` and `onError` all receive the same `response` struct as their last argument. A returned value is stored in `response.body` before `onRequestEnd` runs, so a hook can wrap it, and a handled error defaults to status `500` unless `onError` sets one:

```js
class {

    function onRequestEnd( target, event, context, response ) {
        response.body = { ok: true, data: response.body }
    }

    function onError( exception, eventName, event, context, response ) {
        response.body = { ok: false, error: exception.message }
    }

}
```

If `Application.bx` defines `onError`, the error counts as handled; rethrow from the hook to fail the invocation.

### URI to Class Mapping Examples

| Incoming URI | Handler File | Description |
|---|---|---|
| `/products` | `handlers/Products.bx` | Product management endpoints |
| `/api/test` | `handlers/api/Test.bx` | Nested route (`api/test`) |
| `/user-profiles` | `handlers/UserProfiles.bx` | Handles hyphenated URIs |
| `/orders/123` | `handlers/Orders.bx` | Routes based on first segment when no more specific route matches |
| `/` | `Lambda.bx` | Root requests use the default handler |
| `/unknown/path` | `Lambda.bx` | Falls back to the default when no route matches |

### Creating Route Classes

Create a `.bx` file under `handlers/` with the PascalCase name of your resource:

```java
// handlers/Products.bx - Handles all /products/* requests
class {
    function run( event, context, response ) {
        switch( event.method ?: "GET" ) {
            case "GET":
                return getAllProducts()
                break
            case "POST":
                return createProduct( event.body )
                break
            default:
                response.statusCode = 405
                return { "error": "Method not allowed" }
        }
    }

    private function getAllProducts() {
        return {
            "message": "Fetching all products",
            "data": [
                { "id": 1, "name": "Product 1", "price": 29.99 }
            ]
        }
    }
}
```

### Multi-Method Support

Use the `x-bx-function` header to call a specific method within a route class:

```bash
# Call the default 'run' method
curl -X GET https://<your-app>.azurewebsites.net/products

# Call a custom method
curl -X GET https://<your-app>.azurewebsites.net/products -H "x-bx-function: getActiveProducts"
```

### `manifest.json`

The starter template's `generateManifest` Gradle task scans `handlers/` at build time and writes `manifest.json` so the runtime never scans the filesystem for routable handlers at cold start:

```json
{
  "manifestVersion": 1,
  "defaultHandler": { "file": "Lambda.bx", "method": "run" },
  "handlers": {
    "products": { "file": "handlers/Products.bx" },
    "api/test": { "file": "handlers/api/Test.bx" }
  },
  "reserved": [ "Application.bx", "Lambda.bx" ]
}
```

**`reserved` and `defaultHandler` are enforced, not just documentation**: the runtime actively rejects any `handlers` entry whose target file matches a name in `reserved` (merged with the built-in `Application.bx` and default-handler names), and `defaultHandler.file`/`method` is honored as the fallback handler for unmatched routes — falling back to `Lambda.bx`/`run()` when absent, invalid, or pointing at a file that doesn't exist. Every `handlers` entry's `file` is also checked for existence at cold start.

## 🛠️ Development Setup

### Prerequisites

- **Java 21+** - Required for BoxLang runtime
- **Azure Functions Core Tools** - For local run (`azureFunctionsRun`) and the `func` CLI
- **Azure CLI** - For deployment and subscription management

### Local Development

```bash
# Clone the runtime repository
git clone https://github.com/ortus-boxlang/boxlang-azure-functions.git
cd boxlang-azure-functions

# Download BoxLang dependencies
./gradlew downloadBoxLang

# Build the runtime
./gradlew build shadowJar

# Create deployment packages
./gradlew buildMainZip buildTestZip

# Run tests
./gradlew test
```

### Running Locally

The official Azure Functions Gradle plugin provides local execution via Azure Functions Core Tools:

```bash
./gradlew azureFunctionsRun
```

### Deploying to Azure

```bash
export AZURE_SUBSCRIPTION_ID=<your-subscription-id>
export AZURE_RESOURCE_GROUP=<your-resource-group>
export AZURE_FUNCTION_APP_NAME=<your-function-app-name>
export AZURE_REGION=eastus

./gradlew azureFunctionsDeploy
```

## 🧩 Core Components

### AzureFunctionRunner.java

The single Azure Function entry point:

- **Static Initialization** - BoxLang runtime loads once per worker process
- **Class Caching** - Compiled BoxLang classes cached via `ConcurrentHashMap` (disabled in debug mode for live reloading)
- **URI Routing** - Resolves incoming paths against the `handlers/`/`manifest.json` routing table
- **Application Lifecycle** - Integrates with BoxLang's `Application.bx` lifecycle

### Environment Variables

Runtime behavior is controlled via environment variables:

| Variable | Description | Default |
|---|---|---|
| `BOXLANG_AZURE_ROOT` | Root directory for `.bx` handler files | `AzureWebJobsScriptRoot` (Azure-managed), else `/home/site/wwwroot` |
| `BOXLANG_AZURE_CLASS` | Override the default `Lambda.bx` path | *(unset)* |
| `BOXLANG_AZURE_DEBUGMODE` | Enable verbose logging and disable class caching | `false` |
| `BOXLANG_AZURE_CONFIG` | Path to a custom `boxlang.json` config | `boxlang.json` in root |
| `BOXLANG_ENABLE_ROOT_SCAN` | Allow the legacy function-root routing fallback described in URI Routing above | `true`. Shared across every BoxLang serverless runtime (AWS/GCP/Azure). |

### Build System (Gradle)

Key build tasks:

- `build` - Builds, tests, and packages
- `shadowJar` - Creates the fat JAR published to Maven Central (bundles BoxLang core)
- `buildMainZip` - Packages the deployable runtime + all `.bx` route classes
- `buildTestZip` - Creates test package for validation
- `azureFunctionsRun` - Starts the local Azure Functions dev server (via Core Tools)
- `azureFunctionsDeploy` - Deploys directly to Azure
- `downloadBoxLang` - Downloads BoxLang JARs for local development
- `spotlessApply` - Code formatting and linting

## 🔬 Testing Infrastructure

Tests are located in `src/test/java/ortus/boxlang/runtime/azure/`:

- **`AzureFunctionRunnerTest`** - Core runtime functionality and integration tests
- **`HandlerCacheTest`** - Class caching and warm invocation validation
- **`AzureRequestMapperTest`** - HTTP request → event struct mapping
- **`AzureResponseMapperTest`** - Response struct → HTTP response serialization
- **`RouteResolutionTest`** - URI-to-handler routing logic

## ⚡ Performance Optimizations

### Class Compilation Caching

The runtime caches compiled handler classes between invocations to minimize cold start overhead:

```java
private static final ConcurrentHashMap<String, IClassRunnable> classCache = new ConcurrentHashMap<>();
```

In **debug mode** (`BOXLANG_AZURE_DEBUGMODE=true`), caching is intentionally bypassed so that changes to `.bx` files are picked up immediately without restarting the host.

## 🧑‍💻 Usage

By convention the runtime executes a `Lambda.bx` file located at the root of the deployed package via the `run()` method:

```boxlang
// Lambda.bx
class {

    function run( event, context, response ) {
        // Your code here
    }

}
```

- The `event` parameter is the HTTP request data mapped to a BoxLang `Struct` (`method`, `path`, `headers`, `body`, `queryStringParameters`, `requestContext.http`, etc.) — the same shape used by the AWS Lambda and Google Cloud Functions runtimes.
- The `context` parameter is Azure runtime metadata: `functionName`, `invocationId`, `requestId`.
- The `response` parameter is a mutable `Struct` you populate to control the HTTP response.

### Response Struct

The `response` struct supports the following keys:

| Key | Description |
|---|---|
| `statusCode` | HTTP status code (default: `200`) |
| `headers` | A `Struct` of response headers |
| `body` | Response body — any type; serialized to JSON automatically |
| `cookies` | An `Array` of cookie strings |

If you return a value directly from `run()`, it is placed in `response.body` automatically.

### Example Function

```java
// Lambda.bx
class {

    function run( event, context, response ) {
        response.statusCode              = 200;
        response.headers["Content-Type"] = "application/json";
        response.body = serializeJSON( {
            "message"      : "Hello from BoxLang on Azure!",
            "method"       : event.method,
            "path"         : event.path,
            "functionName" : context.functionName,
            "invocationId" : context.invocationId
        } );
    }

}
```

### Custom Handler Class

If you don't want to use the `Lambda.bx` convention, set the `BOXLANG_AZURE_CLASS` environment variable to the full path of your BoxLang class file.

### Debug Mode

Set `BOXLANG_AZURE_DEBUGMODE=true` to enable verbose logging and disable class caching (so `.bx` file changes are reflected immediately without a restart).

## 📚 Additional Resources

### BoxLang Documentation

- **Main Documentation** - [boxlang.ortusbooks.com](https://boxlang.ortusbooks.com)
- **IDE Tooling** - [Development Tools](https://boxlang.ortusbooks.com/getting-started/ide-tooling)

### Related Projects

- **BoxLang Core** - [boxlang](https://github.com/ortus-boxlang/boxlang)
- **Web Support** - [boxlang-web-support](https://github.com/ortus-boxlang/boxlang-web-support)
- **AWS Lambda Runtime** - [boxlang-aws-lambda](https://github.com/ortus-boxlang/boxlang-aws-lambda)
- **Google Cloud Functions Runtime** - [boxlang-google-functions](https://github.com/ortus-boxlang/boxlang-google-functions)

## License

Apache License, Version 2.0.

## Open-Source & Professional Support

This project is a professional open source project and is available as FREE and open source to use.  Ortus Solutions, Corp provides commercial support, training and commercial subscriptions which include the following:

- Professional Support and Priority Queuing
- Remote Assistance and Troubleshooting
- New Feature Requests and Custom Development
- Custom SLAs
- Application Modernization and Migration Services
- Performance Audits
- Enterprise Modules and Integrations
- Much More

https://www.boxlang.io/plans

<p>&nbsp;</p>

<blockquote>
"We ❤️ Open Source and BoxLang" - Luis Majano
</blockquote>

## ⭐ Star Us

Please star us if this runtime helps you build amazing serverless applications with BoxLang!

[![GitHub Stars](https://img.shields.io/github/stars/ortus-boxlang/boxlang-azure-functions?style=social)](https://github.com/ortus-boxlang/boxlang-azure-functions)

## Ortus Sponsors

BoxLang is a professional open-source project and it is completely funded by the [community](https://patreon.com/ortussolutions) and [Ortus Solutions, Corp](https://www.ortussolutions.com). Ortus Patreons get many benefits like a cfcasts account, a FORGEBOX Pro account and so much more. If you are interested in becoming a sponsor, please visit our patronage page: [https://patreon.com/ortussolutions](https://patreon.com/ortussolutions)

### THE DAILY BREAD

> "I am the way, and the truth, and the life; no one comes to the Father, but by me (JESUS)" Jn 14:1-12
