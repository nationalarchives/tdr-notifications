package uk.gov.nationalarchives.scannotifications.decoders

import io.circe.{Decoder, HCursor}

object ScanDecoder {

  case class ScanEvent(detail: ScanDetail) extends IncomingEvent

  case class ScanDetail(repositoryName: String, findingSeverityCounts: ScanFindingCounts)

  case class ScanFindingCounts(critical: Option[Int], high: Option[Int], medium: Option[Int], low: Option[Int])

  implicit val decodeScanFindingCounts: Decoder[ScanFindingCounts] = (c: HCursor) => for {
    critical <- c.downField("CRITICAL").as[Option[Int]]
    high <- c.downField("HIGH").as[Option[Int]]
    medium <- c.downField("MEDIUM").as[Option[Int]]
    low <- c.downField("LOW").as[Option[Int]]
  } yield ScanFindingCounts(critical, high, medium, low)

  implicit val decodeScanDetail: Decoder[ScanDetail] = (c: HCursor) => for {
    repositoryName <- c.downField("repository-name").as[String]
    findingSeverityCounts <- c.downField("finding-severity-counts").as[ScanFindingCounts]
  } yield ScanDetail(repositoryName, findingSeverityCounts)

  val decodeScanEvent: Decoder[IncomingEvent] = (c: HCursor) => for {
    detail <- c.downField("detail").as[ScanDetail]
  } yield ScanEvent(detail)
}