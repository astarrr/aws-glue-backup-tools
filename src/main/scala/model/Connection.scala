package model

import upickle.default.*

case class PhysicalConnectionRequirements(
    subnetId: Option[String] = None,
    availabilityZone: Option[String] = None,
    securityGroupIdList: List[String] = List.empty
) derives ReadWriter

case class Connection(
    name: String,
    description: Option[String] = None,
    connectionType: String,
    connectionProperties: Map[String, String] = Map.empty,
    matchCriteria: List[String] = List.empty,
    physicalConnectionRequirements: Option[PhysicalConnectionRequirements] = None
) derives ReadWriter
