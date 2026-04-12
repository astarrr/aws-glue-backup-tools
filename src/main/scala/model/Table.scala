package model

import upickle.default.*

sealed case class Column(
    name: String,
    `type`: String,
    comment: Option[String] = None,
    parameters: Option[Map[String, String]] = None
) derives ReadWriter

sealed case class StorageDescriptor(
    location: String,
    columns: Seq[Column],
    inputFormat: Option[String] = None,
    outputFormat: Option[String] = None,
    compressed: Option[Boolean] = None,
    numberOfBuckets: Option[Int] = None,
    bucketColumns: Option[Seq[String]] = None,
    parameters: Option[Map[String, String]] = None
) derives ReadWriter

case class Table(
    name: String,
    databaseName: String,
    description: Option[String] = None
) derives ReadWriter
