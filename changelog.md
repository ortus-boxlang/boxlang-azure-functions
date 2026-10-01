# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### Added

- Initial release: BoxLang Azure Functions Runtime, structurally identical to `boxlang-aws-lambda` and `boxlang-google-functions` — same `handlers/` directory convention, `manifest.json` build-time routing manifest with a 3-tier resolution order (manifest → `handlers/` scan → legacy root scan), `x-bx-function` header method dispatch, and `run(event, context, response)` handler contract. `.bx` handler files run unmodified across all three runtimes.
- `BOXLANG_ENABLE_ROOT_SCAN` (shared across all serverless runtimes, default `true`): set to `false` to opt out of the legacy root-directory scan used when neither `manifest.json` nor `handlers/` is present, restricting routing to the default handler only.

### Changed

- The `response` struct is now passed as the last argument to the `Application.bx` `onRequestEnd` and `onError` hooks, and the handler's return value is assigned to `response.body` before `onRequestEnd`, so hooks can wrap or replace the body and set the status. Hooks that do not declare the extra argument are unaffected.
- A handled error now defaults the response status to `500` unless `onError` sets one (it was `200`).
- A present-but-corrupt `manifest.json` now restricts routing to the default handler only, instead of falling back to a `handlers/` or root-directory scan.

### Fixed

- `Application.bx` is now loaded for requests routed to a class under `handlers/`, so `onRequestStart`, datasources and every other `Application.bx` setting apply to routed handlers (previously only the default handler saw them).

### Security

- `manifest.json` `reserved` and `defaultHandler` are now enforced, not just documented: reserved files can never be routed, `defaultHandler.file` and `method` are honored, and handler files that do not exist are skipped.
- `manifest.json` handler and `defaultHandler` paths are normalized and confined to the deployment root, so `../` can no longer route to files outside it.
- Setting `defaultHandler.file` to `Application.bx` now aborts cold start with a clear error instead of crashing with `duplicate element` and leaving the reserved file in effect as the default handler.

[unreleased]: https://github.com/ortus-boxlang/boxlang-azure-functions/compare/v1.0.0...HEAD
