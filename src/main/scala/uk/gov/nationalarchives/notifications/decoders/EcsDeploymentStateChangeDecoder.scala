package uk.gov.nationalarchives.notifications.decoders

object EcsDeploymentStateChangeDecoder {
  case class EcsTaskContainer(
                               name: Option[String],
                               exitCode: Option[Int],
                               reason: Option[String]
                             )

  case class EcsDeploymentDetail(
                                  clusterArn: Option[String],
                                  taskArn: Option[String],
                                  lastStatus: Option[String],
                                  stopCode: Option[String],
                                  stoppedReason: Option[String],
                                  containers: Option[List[EcsTaskContainer]]
                                )

  case class EcsDeploymentStateChangeEvent(
                                            version: Option[String],
                                            id: Option[String],
                                            `detail-type`: Option[String],
                                            source: Option[String],
                                            account: Option[String],
                                            time: Option[String],
                                            region: Option[String],
                                            detail: Option[EcsDeploymentDetail]
                                          ) extends IncomingEvent
}
