package uk.gov.nationalarchives.notifications.decoders

object UsersDisabledEventDecoder {
  case class UsersDisabledEvent(environment: String, disabledUsersCount: Int, logInfo: LogInfo, dryRun: Boolean) extends IncomingEvent
  case class LogInfo(logGroupName: String, logStreamName: String)
}
