# AWS Glue Backup Tool

A CLI tool to back up and restore AWS Glue catalog resources such as Databases, Tables, and Connections as JSON files. Useful when you don't have AWS Backup or versioning enabled in your account.

## Build

Requires Java 17+ and [sbt](https://www.scala-sbt.org/).

```bash
sbt stage
```

This produces a self-contained script at `target/universal/stage/bin/glue-backup-tool`.

## Usage

### Backup

Writes each resource as a JSON file under `--output-dir` (defaults to the current directory):

```bash
AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... \
  glue-backup-tool backup \
  --catalog-id 123456789012 \
  --region eu-west-1
```

Output layout:

```
./databases/<name>.json
./tables/<database>.<table>.json
./connections/<name>.json
```

### Restore

Reads JSON files from `--input-dir` (defaults to the current directory) and recreates resources in Glue:

```bash
AWS_ACCESS_KEY_ID=... AWS_SECRET_ACCESS_KEY=... \
  glue-backup-tool restore \
  --catalog-id 123456789012 \
  --region eu-west-1 \
  --input-dir /path/to/backup
```

If any resource already exists, restore reports all failures at the end and exits non-zero. Resources that don't conflict are still restored.

### Targeting a local or custom endpoint

Both commands accept `--endpoint-override` for use with local compatible endpoints:

```bash
AWS_ACCESS_KEY_ID=test AWS_SECRET_ACCESS_KEY=test \
  glue-backup-tool backup \
  --endpoint-override http://localhost:4566 \
  --catalog-id 000000000000
```

### All options

```
glue-backup-tool backup
  --catalog-id   AWS Account Catalog ID (default: "default")
  --region       AWS region (default: us-east-1)
  --endpoint-override  Override the Glue endpoint URL

glue-backup-tool restore
  --catalog-id   AWS Account Catalog ID (default: "default")
  --region       AWS region (default: us-east-1)
  --endpoint-override  Override the Glue endpoint URL
  --input-dir    Directory containing backup JSON files (default: current directory)
```

## Running tests

```bash
sbt test
```
