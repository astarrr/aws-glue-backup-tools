import com.monovore.decline.*
import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.services.glue.model.{
  Connection,
  Database,
  GetDatabasesRequest,
  Table
}
import cats.syntax.all.*

import scala.jdk.CollectionConverters.*
import upickle.default.*

object BackupCommand:

  val command: Command[Unit] =
    Command("backup", "Backup all AWS Glue resources") {
      (SharedOpts.catalogId, SharedOpts.region, SharedOpts.endpointOverride)
        .mapN { (catalogId, region, endpointOverride) =>
          val glueClient = SharedOpts.buildClient(region, endpointOverride)
          run(glueClient, catalogId)
        }
    }

  def save(database: Database): Unit =
    val content = write(
      model.Database(
        database.name,
        database.description,
        database.locationUri,
        database.parameters.asScala.toMap
      )
    )
    os.write.over(
      os.pwd / "databases" / s"${database.name}.json",
      content,
      createFolders = true
    )

  def save(table: Table): Unit =
    val content = write(
      model.Table(
        table.name,
        table.databaseName,
        Some(table.description)
      )
    )
    os.write.over(
      os.pwd / "tables" / s"${table.databaseName}.${table.name}.json",
      content,
      createFolders = true
    )

  def save(connection: Connection): Unit =
    val physReqs = Option(connection.physicalConnectionRequirements).map { p =>
      model.PhysicalConnectionRequirements(
        Option(p.subnetId),
        Option(p.availabilityZone),
        Option(p.securityGroupIdList).fold(List.empty[String])(_.asScala.toList)
      )
    }
    val content = write(
      model.Connection(
        connection.name,
        Option(connection.description),
        connection.connectionTypeAsString,
        Option(connection.connectionPropertiesAsStrings)
          .fold(Map.empty[String, String])(_.asScala.toMap),
        Option(connection.matchCriteria)
          .fold(List.empty[String])(_.asScala.toList),
        physReqs
      )
    )
    os.write.over(
      os.pwd / "connections" / s"${connection.name}.json",
      content,
      createFolders = true
    )

  def saveAllDatabases(glueClient: GlueClient, catalogId: String): Unit =
    val databases = glueClient
      .getDatabasesPaginator(
        GetDatabasesRequest.builder().catalogId(catalogId).build()
      )
      .asScala
      .flatMap(_.databaseList().asScala)
      .toList
    println(s"Found ${databases.size} databases")
    databases.foreach(save)

  def saveAllTables(glueClient: GlueClient, catalogId: String): Unit =
    val databases = glueClient
      .getDatabasesPaginator(
        GetDatabasesRequest.builder().catalogId(catalogId).build()
      )
      .asScala
      .flatMap(_.databaseList().asScala)
      .toList
    val tables = databases.flatMap { database =>
      glueClient
        .getTablesPaginator(_.catalogId(catalogId).databaseName(database.name))
        .asScala
        .flatMap(_.tableList().asScala)
        .toList
    }
    println(s"Found ${tables.size} tables")
    tables.foreach(save)

  def saveAllConnections(glueClient: GlueClient, catalogId: String): Unit =
    val connections = glueClient
      .getConnectionsPaginator(_.catalogId(catalogId))
      .asScala
      .flatMap(_.connectionList().asScala)
      .toList
    println(s"Found ${connections.size} connections")
    connections.foreach(save)

  def run(glueClient: GlueClient, catalogId: String): Unit =
    saveAllDatabases(glueClient, catalogId)
    saveAllTables(glueClient, catalogId)
    saveAllConnections(glueClient, catalogId)
