package uk.gov.nationalarchives.notifications

import com.typesafe.config.{Config, ConfigException, ConfigFactory}
import org.scalatest.flatspec.AnyFlatSpec
import org.scalatest.matchers.should.Matchers

class ApplicationDevConfigSpec extends AnyFlatSpec with Matchers {
  private val devTemplateIds = Map(
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
      resolveDevConfig(devTemplateIds - "FILE_CHECK_FAILURE_TEMPLATE_ID")
    }

    exception.getMessage should include("FILE_CHECK_FAILURE_TEMPLATE_ID")
  }

  it should "resolve when all dev template ids are provided" in {
    val config = resolveDevConfig(devTemplateIds)

    config.getString("gov_uk_notify.transfer_complete_dta_template_id") should be(devTemplateIds("TRANSFER_COMPLETE_DTA_TEMPLATE_ID"))
    config.getString("gov_uk_notify.transfer_complete_tb_template_id") should be(devTemplateIds("TRANSFER_COMPLETE_TB_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_requested_dta_template_id") should be(devTemplateIds("METADATA_REVIEW_REQUESTED_DTA_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_requested_tb_template_id") should be(devTemplateIds("METADATA_REVIEW_REQUESTED_TB_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_rejected_template_id") should be(devTemplateIds("METADATA_REVIEW_REJECTED_TEMPLATE_ID"))
    config.getString("gov_uk_notify.metadata_review_approved_template_id") should be(devTemplateIds("METADATA_REVIEW_APPROVED_TEMPLATE_ID"))
    config.getString("gov_uk_notify.upload_failed_template_id") should be(devTemplateIds("UPLOAD_FAILED_TEMPLATE_ID"))
    config.getString("gov_uk_notify.upload_complete_template_id") should be(devTemplateIds("UPLOAD_COMPLETE_TEMPLATE_ID"))
    config.getString("gov_uk_notify.file_check_failure_template_id") should be(devTemplateIds("FILE_CHECK_FAILURE_TEMPLATE_ID"))
  }

  private def resolveDevConfig(values: Map[String, String] = Map.empty): Config = {
    val providedValues = ConfigFactory.parseString(values.map { case (key, value) => s"""$key = "$value"""" }.mkString("\n"))

    ConfigFactory.parseResources("application.dev.conf")
      .withFallback(providedValues)
      .withFallback(ConfigFactory.parseResources("application.conf"))
      .resolve()
  }
}
