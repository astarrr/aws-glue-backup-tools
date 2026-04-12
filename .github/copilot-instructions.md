# Copilot Instructions

## Build & Test Commands

```bash
# Compile
sbt compile

# Run all tests (requires LocalStack Pro auth token)
LOCALSTACK_AUTH_TOKEN=ls-... sbt test

# Run a single test class
LOCALSTACK_AUTH_TOKEN=ls-... sbt "testOnly GlueBackupToolSpec"

# Package the application
sbt stage        # creates standalone script under target/universal/stage/bin/
sbt universal:packageBin  # creates a zip distribution
```

## Architecture

This is a Scala 3 CLI tool that backs up AWS Glue catalog objects (Databases, Tables, Partitions, Connections) to JSON files in the current working directory.

- **`GlueBackupTool`** — extends `decline`'s `CommandApp`, making it both the CLI entry point and the core logic container. Accepts `--catalog-id` and `--endpoint-override` flags.
- **`model/`** — Plain Scala case classes that mirror AWS SDK types but are serializable. Each model derives `ReadWriter` from upickle for JSON I/O.
- Each backed-up resource is written as `{name}.json` in `os.pwd` (current working directory at runtime).

## Key Conventions

- **JSON serialization**: Uses `upickle` (pulled in via `org.scala-lang:toolkit`). Model classes derive `ReadWriter` automatically with `derives ReadWriter` — no manual codecs.
- **File I/O**: Uses `os-lib` (`os.write.over`, `os.pwd`, `os.read`) — also part of the toolkit dependency.
- **AWS SDK**: AWS SDK v2 (`software.amazon.awssdk`). SDK responses are validated with `.ensuring(_.sdkHttpResponse().isSuccessful)`. Java collections are converted with `.asScala`.
- **CLI parsing**: `com.monovore.decline` — options are defined as `Opts[T]` and composed with `mapN`.
- **Tests**: ScalaTest `AnyFlatSpec` style. Integration tests use `testcontainers-scala` with `localstack/localstack-pro` Docker image — no unit mocking of AWS. The `LOCALSTACK_AUTH_TOKEN` env var must be set to run tests.
- **Model pattern**: When adding support for a new Glue resource (Tables, Partitions, etc.), add a case class in `model/` with `derives ReadWriter`, then add a corresponding `save(...)` method and list/fetch call in `GlueBackupTool`.
