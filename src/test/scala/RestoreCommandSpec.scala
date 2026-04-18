import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.services.glue.model.Database
import software.amazon.awssdk.services.glue.model.Table
import software.amazon.awssdk.services.glue.model.{
  Connection as AwsConnection,
  ConnectionType,
  ConnectionPropertyKey
}
import org.scalatest.{BeforeAndAfterEach, *}
import org.scalatest.flatspec.{AnyFlatSpec, *}
import org.scalatest.matchers.should.Matchers.*
import software.amazon.awssdk.regions.Region
import upickle.default.*

import java.net.URI
import scala.compiletime.uninitialized
import scala.jdk.CollectionConverters.*
import model.*

class RestoreCommandSpec
    extends AnyFlatSpec
    with TestContainerForAll
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val REGION = Region.US_EAST_1
  private var glueClient: GlueClient = uninitialized

  override val containerDef: GenericContainer.Def[GenericContainer] =
    GenericContainer.Def(
      "ministackorg/ministack",
      Seq(4566)
    )

  override protected def beforeAll(): Unit = {
    val container: GenericContainer = containerDef.start()

    glueClient = GlueClient
      .builder()
      .credentialsProvider(
        StaticCredentialsProvider.create(
          AwsBasicCredentials.create("accesskey", "secretkey")
        )
      )
      .endpointOverride(
        new URI(
          s"http://${container.containerIpAddress}:${container.container.getFirstMappedPort}"
        )
      )
      .region(REGION)
      .build()
  }

  it should "restore a database correctly" in {
    val database = model.Database("test-database", null, null, Map.empty)
    RestoreCommand.restore(database, glueClient, "default")

    glueClient
      .getDatabase(_.catalogId("default").name("test-database"))
      .database
      .name shouldBe "test-database"

    glueClient.deleteDatabase(_.catalogId("default").name("test-database"))
  }

  it should "restore a table correctly" in {

    glueClient.createDatabase(
      _.catalogId("default").databaseInput(_.name("test-database-1"))
    )

    val table =
      model.Table("test-table", "test-database-1", Some("A test table"))

    RestoreCommand.restore(table, glueClient, "default")

    glueClient
      .getTable(
        _.catalogId("default")
          .databaseName("test-database-1")
          .name("test-table")
      )
      .table
      .name shouldBe "test-table"

    glueClient.deleteTable(
      _.catalogId("default")
        .databaseName("test-database-1")
        .name("test-table")
    )
  }

  it should "restore a connection correctly" in {
    val connection = model.Connection(
      "test-connection",
      None,
      "JDBC",
      Map.empty,
      List.empty,
      None
    )

    RestoreCommand.restore(connection, glueClient, "default")

    glueClient
      .getConnection(_.catalogId("default").name("test-connection"))
      .connection
      .name shouldBe "test-connection"
  }
}
