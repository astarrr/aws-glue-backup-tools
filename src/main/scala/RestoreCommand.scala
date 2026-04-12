import com.monovore.decline.*
import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.services.glue.model.{
  PhysicalConnectionRequirements as AwsPhysReqs
}
import cats.syntax.all.*

import scala.util.Try
import scala.jdk.CollectionConverters.*
import upickle.default.*
import java.nio.file.{Path as JPath}
import model.*

object RestoreCommand:

  val command: Command[Unit] =
    Command("restore", "Restore AWS Glue resources from a backup") {
      val inputDir = Opts
        .option[JPath]("input-dir", help = "Directory containing backup files.")
        .map(p => os.Path(p.toAbsolutePath))
        .withDefault(os.pwd)

      (
        SharedOpts.catalogId,
        SharedOpts.region,
        SharedOpts.endpointOverride,
        inputDir
      ).mapN { (catalogId, region, endpointOverride, inputDir) =>
        val glueClient = SharedOpts.buildClient(region, endpointOverride)
        run(glueClient, catalogId, inputDir)
      }
    }

  def restore(
      db: model.Database,
      glueClient: GlueClient,
      catalogId: String
  ): Unit =
    glueClient.createDatabase(
      _.catalogId(catalogId).databaseInput(
        _.name(db.name)
          .description(db.description)
          .locationUri(db.location)
          .parameters(db.parameters.asJava)
      )
    )

  def restore(
      table: model.Table,
      glueClient: GlueClient,
      catalogId: String
  ): Unit =
    glueClient.createTable(
      _.catalogId(catalogId)
        .databaseName(table.databaseName)
        .tableInput(_.name(table.name).description(table.description.orNull))
    )

  def restore(
      connection: model.Connection,
      glueClient: GlueClient,
      catalogId: String
  ): Unit =
    glueClient.createConnection { r =>
      r.catalogId(catalogId).connectionInput { b =>
        b.name(connection.name)
          .connectionType(connection.connectionType)
          .connectionPropertiesWithStrings(
            connection.connectionProperties.asJava
          )
          .matchCriteria(connection.matchCriteria.asJava)
        connection.physicalConnectionRequirements.foreach { p =>
          b.physicalConnectionRequirements(
            AwsPhysReqs
              .builder()
              .subnetId(p.subnetId.orNull)
              .availabilityZone(p.availabilityZone.orNull)
              .securityGroupIdList(p.securityGroupIdList.asJava)
              .build()
          )
        }
      }
    }

  def restoreAllDatabases(
      glueClient: GlueClient,
      catalogId: String,
      databases: List[Database]
  ): Unit =
    databases.foreach(
      restore(_, glueClient, catalogId)
    )

  def restoreAllTables(
      glueClient: GlueClient,
      catalogId: String,
      tables: List[model.Table]
  ): Unit =
    tables.foreach(
      restore(_, glueClient, catalogId)
    )

  def restoreAllConnections(
      glueClient: GlueClient,
      catalogId: String,
      connections: List[model.Connection]
  ): Unit =
    connections.foreach(
      restore(_, glueClient, catalogId)
    )

  def run(
      glueClient: GlueClient,
      catalogId: String,
      inputDir: os.Path
  ): Unit = {
    val databases: List[Database] = os
      .list(inputDir / "databases")
      .filter(_.ext == "json")
      .toList
      .map { file =>
        read[model.Database](os.read(file))
      }

    restoreAllDatabases(glueClient, catalogId, databases)

    val tables: List[Table] = os
      .list(inputDir / "tables")
      .filter(_.ext == "json")
      .toList
      .map { file =>
        read[model.Table](os.read(file))
      }

    restoreAllTables(glueClient, catalogId, tables)

    val connections: List[model.Connection] = os
      .list(inputDir / "connections")
      .filter(_.ext == "json")
      .toList
      .map { file =>
        read[model.Connection](os.read(file))
      }

    restoreAllConnections(glueClient, catalogId, connections)

  }
