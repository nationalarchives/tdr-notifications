package uk.gov.nationalarchives.notifications.decoders

object EcsDeploymentStateChangeDecoder {
  case class EcsDeploymentDetail(
                                  eventType: String,
                                  eventName: String,
                                  deploymentId: String,
                                  reason: String,
                                  createdAt: String,
                                  updatedAt: String
                                )

  case class EcsDeploymentStateChangeEvent(
                                            version: String,
                                            id: String,
                                            `detail-type`: String,
                                            source: String,
                                            account: String,
                                            time: String,
                                            region: String,
                                            resources: List[String],
                                            detail: EcsDeploymentDetail
                                          ) extends IncomingEvent
}

