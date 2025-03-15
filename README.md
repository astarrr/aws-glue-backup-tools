# AWS Glue Backup Tool

This tool enables users to back up and restore Glue objects such as Databases, Tables, Partitions, Connections, etc. in
JSON format.
It can be useful when you do not have versioning enabled in your account.

## Environment Variables

To run tests I leverage LocalStack Pro, you will need to add the following environment variables

`LOCALSTACK_AUTH_TOKEN=ls-...`

You can get a license at https://www.localstack.cloud/

## Running Tests

To run tests, run the following command

```bash
  LOCALSTACK_AUTH_TOKEN=ls-... sbt test
```
