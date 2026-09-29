# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

* * *

## [Unreleased]

### Added

- Initial release: BoxLang Azure Functions Runtime, structurally identical to `boxlang-aws-lambda` and `boxlang-google-functions` — same `handlers/` directory convention, `manifest.json` build-time routing manifest with a 3-tier resolution order (manifest → `handlers/` scan → legacy root scan), `x-bx-function` header method dispatch, and `run(event, context, response)` handler contract. `.bx` handler files run unmodified across all three runtimes.

[unreleased]: https://github.com/ortus-boxlang/boxlang-azure-functions/compare/v1.0.0...HEAD
