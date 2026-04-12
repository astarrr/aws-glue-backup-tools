import com.monovore.decline.*
import software.amazon.awssdk.services.glue.GlueClient
import software.amazon.awssdk.regions.Region

import java.net.URI

object SharedOpts:
  val catalogId: Opts[String] =
    Opts.option[String]("catalog-id", help = "AWS Account Catalog ID.").withDefault("default")

  val region: Opts[String] =
    Opts
      .option[String]("region", help = "AWS Region to connect to.")
      .withDefault("us-east-1")

  val endpointOverride: Opts[Option[URI]] =
    Opts.option[URI]("endpoint-override", help = "Overrides the AWS Glue endpoint.").orNone

  def buildClient(region: String, endpointOverride: Option[URI]): GlueClient =
    val builder = GlueClient.builder().region(Region.of(region))
    endpointOverride.fold(builder)(builder.endpointOverride(_))
    builder.build()