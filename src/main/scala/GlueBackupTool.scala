import com.monovore.decline.*
import software.amazon.awssdk.services.glue.GlueClient
import cats.syntax.all.*

import java.net.URI
import GlueBackupTool.run
import software.amazon.awssdk.services.glue.model.{
  Table,
  Database,
  GetDatabasesRequest
}

import scala.jdk.CollectionConverters.*
import upickle.default.*
import software.amazon.awssdk.services.glue.internal.GlueClientOption

object GlueBackupTool
    extends CommandApp(
      name = "glue-backup-tool",
      header = "Backups all your AWS Glue stuff!",
      main = {
        val endpointOverride = Opts
          .option[URI](
            "endpoint-override",
            help = "Overrides the AWS Glue endpoint."
          )
          .orNone

        val catalogId = Opts
          .option[String]("catalog-id", help = "AWS Account Catalog ID.")
          .withDefault("default")

        (catalogId, endpointOverride).mapN { (catalogId, endpointOverride) =>
          {
            val glueClient: GlueClient = GlueClient.builder.build()
            run(glueClient, endpointOverride, catalogId)
          }
        }
      }
    ) {

  /** Saves a Glue Database under pwd/database.name().json
    * @param database
    *   Glue Database to save
    */
  def save(database: Database): Unit = {
    val content = write(
      model.Database(
        database.name,
        database.description,
        database.locationUri,
        database.parameters.asScala.toMap
      )
    )

    os.write.over(os.pwd / "databases" / s"${database.name}.json", content)
  }

  def save(table: Table): Unit = {
    val content = write(
      model.Table(
        table.name,
        table.databaseName,
        Some(table.description)
      )
    )

    os.write.over(
      os.pwd / "tables" / s"${table.databaseName}.${table.name}.json",
      content
    )
  }

  def saveAllDatabases(glueClient: GlueClient, catalogId: String): Unit = {
    val databases: List[Database] = glueClient
      .getDatabasesPaginator(
        GetDatabasesRequest
          .builder()
          .catalogId(catalogId)
          .build()
      )
      .asScala
      .flatMap(_.databaseList().asScala)
      .toList

    databases.foreach(save)
  }

  def saveAllTables(glueClient: GlueClient, catalogId: String): Unit = {
    val databases: List[Database] = glueClient
      .getDatabasesPaginator(
        GetDatabasesRequest
          .builder()
          .catalogId(catalogId)
          .build()
      )
      .asScala
      .flatMap(_.databaseList().asScala)
      .toList

    val tables: List[Table] = databases
      .flatMap { database =>
        glueClient
          .getTablesPaginator(
            _.catalogId(catalogId).databaseName(database.name)
          )
          .asScala
          .flatMap(_.tableList().asScala)
          .toList
      }

    tables.foreach(save)
  }

  def run(
      glueClient: GlueClient,
      endpointOverride: Option[URI] = None,
      catalogId: String
  ): Unit = {
    saveAllDatabases(glueClient, catalogId)
    saveAllTables(glueClient, catalogId)
  }
}
