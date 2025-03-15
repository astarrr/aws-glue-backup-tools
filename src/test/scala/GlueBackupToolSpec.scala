import com.dimafeng.testcontainers.GenericContainer
import com.dimafeng.testcontainers.scalatest.TestContainerForAll
import software.amazon.awssdk.auth.credentials.{
  AwsBasicCredentials,
  StaticCredentialsProvider
}
import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.services.glue.model.Database
import org.scalatest.{BeforeAndAfterEach, *}
import org.scalatest.flatspec.{AnyFlatSpec, *}
import org.scalatest.matchers.should.Matchers.*
import software.amazon.awssdk.regions.Region
import upickle.default.*

import java.net.URI
import scala.compiletime.uninitialized

class GlueBackupToolSpec
    extends AnyFlatSpec
    with TestContainerForAll
    with BeforeAndAfterEach
    with BeforeAndAfterAll {

  private val REGION = Region.US_EAST_1
  private var glueClient: GlueClient = uninitialized

  override val containerDef: GenericContainer.Def[GenericContainer] =
    GenericContainer.Def(
      "localstack/localstack-pro",
      Seq(4566),
      Map(
        "LOCALSTACK_AUTH_TOKEN" -> sys.env("LOCALSTACK_AUTH_TOKEN")
      )
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
      .name("database")
      .build()

    GlueBackupTool.save(database)

    val json = os.read(os.pwd / s"${database.name}.json")
    val actualDatabase = read[model.Database](json)

    actualDatabase.name shouldBe "database"
  }
}
