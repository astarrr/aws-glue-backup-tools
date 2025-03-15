import com.monovore.decline.*
import software.amazon.awssdk.services.glue.GlueClient
import cats.syntax.all.*

import java.net.URI
import GlueBackupTool.run
import software.amazon.awssdk.services.glue.model.{
  Database,
  GetDatabasesRequest
}

import scala.jdk.CollectionConverters.*
import upickle.default.*

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

  /**
   * Saves a Glue Database under pwd/database.name().json
   * @param database Glue Database to save
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
    os.write.over(os.pwd / s"${database.name}.json", content)
  }

  def run(
      glueClient: GlueClient,
      endpointOverride: Option[URI] = None,
      catalogId: String
  ): Unit = {
    val databases: List[Database] = glueClient
      .getDatabases(
        GetDatabasesRequest
          .builder()
          .catalogId(catalogId)
          .build()
      )
      .ensuring(_.sdkHttpResponse().isSuccessful)
      .databaseList()
      .asScala
      .toList

    databases.foreach(save)
  }
}
