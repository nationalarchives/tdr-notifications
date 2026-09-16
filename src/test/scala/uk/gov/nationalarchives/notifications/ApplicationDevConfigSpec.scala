package uk.gov.nationalarchives.notifications

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

import scala.jdk.CollectionConverters._

class ApplicationDevConfigSpec extends AnyFlatSpec with Matchers {
  private val devConfigValues = Map(
    "GOV_UK_NOTIFY_API_KEY" -> "dev-gov-uk-notify-api-key",
    "TRANSFER_COMPLETE_DTA_TEMPLATE_ID" -> "transfer-complete-dta-template-id",
    "TRANSFER_COMPLETE_TB_TEMPLATE_ID" -> "transfer-complete-tb-template-id",
    "METADATA_REVIEW_REQUESTED_DTA_TEMPLATE_ID" -> "metadata-review-requested-dta-template-id",
    "METADATA_REVIEW_REQUESTED_TB_TEMPLATE_ID" -> "metadata-review-requested-tb-template-id",
    "METADATA_REVIEW_REJECTED_TEMPLATE_ID" -> "metadata-review-rejected-template-id",
    "METADATA_REVIEW_APPROVED_TEMPLATE_ID" -> "metadata-review-approved-template-id",
    "UPLOAD_FAILED_TEMPLATE_ID" -> "upload-failed-template-id",
    "UPLOAD_COMPLETE_TEMPLATE_ID" -> "upload-complete-template-id",
    "FILE_CHECK_FAILURE_TEMPLATE_ID" -> "file-check-failure-template-id"
  )

  "application.dev.conf" should "require the file check failure template id" in {
    val exception = intercept[ConfigException.UnresolvedSubstitution] {
      resolveDevConfig(devConfigValues - "FILE_CHECK_FAILURE_TEMPLATE_ID")
    }

    exception.getMessage should include("FILE_CHECK_FAILURE_TEMPLATE_ID")
  }

  it should "resolve when all dev template ids are provided" in {
    val config = resolveDevConfig(devConfigValues)

    config.getString("gov_uk_notify.api_key") should be(devConfigValues("GOV_UK_NOTIFY_API_KEY"))
    config.getString("gov_uk_notify.transfer_complete_dta_template_id") should be(devConfigValues("TRANSFER_COMPLETE_DTA_TEMPLATE_ID"))
    config.getString("gov_uk_notify.transfer_complete_tb_template_id") should be(devConfigValues("TRANSFER_COMPLETE_TB_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_requested_dta_template_id") should be(devConfigValues("METADATA_REVIEW_REQUESTED_DTA_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_requested_tb_template_id") should be(devConfigValues("METADATA_REVIEW_REQUESTED_TB_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_rejected_template_id") should be(devConfigValues("METADATA_REVIEW_REJECTED_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_approved_template_id") should be(devConfigValues("METADATA_REVIEW_APPROVED_TEMPLATE_ID"))
    config.getString("gov_uk_notify.upload_failed_template_id") should be(devConfigValues("UPLOAD_FAILED_TEMPLATE_ID"))
    config.getString("gov_uk_notify.upload_complete_template_id") should be(devConfigValues("UPLOAD_COMPLETE_TEMPLATE_ID"))
    config.getString("gov_uk_notify.file_check_failure_template_id") should be(devConfigValues("FILE_CHECK_FAILURE_TEMPLATE_ID"))
  }

  private def resolveDevConfig(values: Map[String, String] = Map.empty): Config = {
    val providedValues = ConfigFactory.parseMap(values.asJava)

    providedValues
      .withFallback(ConfigFactory.parseResources("application.dev.conf"))
      .withFallback(ConfigFactory.parseResources("application.conf"))
      .resolve()
  }
}
