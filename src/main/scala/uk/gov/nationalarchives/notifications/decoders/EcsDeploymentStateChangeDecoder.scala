package uk.gov.nationalarchives.notifications.decoders

object EcsDeploymentStateChangeDecoder {
  case class EcsTaskContainer(
                               name: String,
                               image: String,
                               exitCode: Int
                             )

  case class EcsDeploymentDetail(
                                  taskArn: String,
                                  containers: List[EcsTaskContainer]
                                )

  case class EcsDeploymentStateChangeEvent(
                                            `detail-type`: String,
                                            detail: EcsDeploymentDetail
                                          ) extends IncomingEvent
}
