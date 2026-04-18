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

class GlueBackupToolSpec
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

  it should "backup correctly a Glue Database" in {
    val database = Database.builder
      .catalogId("default")
      .name("test-database")
      .build()

    BackupCommand.save(database)

    val json = os.read(os.pwd / "databases" / s"${database.name}.json")
    val actualDatabase = read[model.Database](json)

    actualDatabase.name shouldBe "test-database"
  }

  it should "backup correctly more then 100s of databases" in {
    (1 to 150).foreach { i =>
      val database = Database.builder
        .catalogId("default")
        .name(s"test-database-$i")
        .build()

      glueClient.createDatabase(
        _.catalogId("default").databaseInput(_.name(database.name))
      )
    }

    BackupCommand.saveAllDatabases(glueClient, "default")

    (1 to 150).foreach { i =>
      val json = os.read(os.pwd / "databases" / s"test-database-$i.json")
      val actualDatabase = read[model.Database](json)

      actualDatabase.name shouldBe s"test-database-$i"
    }

    (1 to 150).foreach { i =>
      glueClient.deleteDatabase(
        _.catalogId("default").name(s"test-database-$i")
      )
    }
  }

  it should "backup a table correctly" in {
    val table = Table.builder
      .catalogId("default")
      .databaseName("test-database-1")
      .name("test-table")
      .description("test description")
      .build()

    BackupCommand.save(table)

    val json =
      os.read(os.pwd / "tables" / s"${table.databaseName}.${table.name}.json")
    val actualTable = read[model.Table](json)

    actualTable.name shouldBe "test-table"
  }

  it should "backup correctly more then 100s of tables" in {
    // create 150 tables in test-database-1
    glueClient.createDatabase(
      _.catalogId("default").databaseInput(_.name("test-database-1"))
    )

    (1 to 150)
      .foreach { i =>
        val table = Table.builder
          .catalogId("default")
          .databaseName("test-database-1")
          .name(s"test-table-$i")
          .build()

        glueClient.createTable(
          _.catalogId("default")
            .databaseName("test-database-1")
            .tableInput(
              _.name(table.name)
            )
        )
      }

    // create 150 tables in test-database-2

    glueClient.createDatabase(
      _.catalogId("default").databaseInput(_.name("test-database-2"))
    )

    (1 to 150)
      .foreach { i =>
        val table = Table.builder
          .catalogId("default")
          .databaseName("test-database-2")
          .name(s"test-table-$i")
          .build()

        glueClient.createTable(
          _.catalogId("default")
            .databaseName("test-database-2")
            .tableInput(
              _.name(table.name)
            )
        )
      }

    BackupCommand.saveAllTables(glueClient, "default")

    var json: String = ""
    var actualTable: model.Table = null

    (1 to 150).foreach { i =>
      json = os.read(os.pwd / "tables" / s"test-database-1.test-table-$i.json")
      actualTable = read[model.Table](json)

      actualTable.name shouldBe s"test-table-$i"

      json = os.read(os.pwd / "tables" / s"test-database-2.test-table-$i.json")
      actualTable = read[model.Table](json)

      actualTable.name shouldBe s"test-table-$i"
    }
  }

  it should "backup a connection correctly" in {
    val connection = AwsConnection.builder
      .name("test-connection")
      .connectionType(ConnectionType.JDBC)
      .build()

    BackupCommand.save(connection)

    val json = os.read(os.pwd / "connections" / "test-connection.json")
    val actualConnection = read[model.Connection](json)

    actualConnection.name shouldBe "test-connection"
  }

  it should "backup correctly more than 100 connections" in {
    (1 to 150).foreach { i =>
      glueClient.createConnection(
        _.catalogId("default").connectionInput(
          _.name(s"test-connection-$i")
            .connectionType(ConnectionType.JDBC)
            .connectionProperties(
              Map(
                ConnectionPropertyKey.JDBC_CONNECTION_URL -> "jdbc:test://localhost",
                ConnectionPropertyKey.USERNAME -> "user",
                ConnectionPropertyKey.PASSWORD -> "pass"
              ).asJava
            )
        )
      )
    }

    BackupCommand.saveAllConnections(glueClient, "default")

    (1 to 150).foreach { i =>
      val json = os.read(os.pwd / "connections" / s"test-connection-$i.json")
      val actualConnection = read[model.Connection](json)

      actualConnection.name shouldBe s"test-connection-$i"
    }
  }

}
