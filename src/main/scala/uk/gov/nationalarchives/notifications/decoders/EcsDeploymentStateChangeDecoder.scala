package uk.gov.nationalarchives.notifications.decoders

object EcsDeploymentStateChangeDecoder {
  case class EcsTaskContainer(
                               name: String,
                               exitCode: Option[Int],
                               reason: Option[String]
                             )

  case class EcsDeploymentDetail(
                                  taskArn: String,
                                  lastStatus: String,
                                  stoppedReason: Option[String],
                                  containers: List[EcsTaskContainer]
                                )

  case class EcsDeploymentStateChangeEvent(
                                            `detail-type`: String,
                                            detail: EcsDeploymentDetail
                                          ) extends IncomingEvent
}
