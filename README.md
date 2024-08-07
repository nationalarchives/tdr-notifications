# TDR Notifications

This project is for sending slack/email/SQS messages in response to events. It supports several types of event:

* ECR scan results. Each time there is an ECR repository scan, the scan results are checked. If there are any errors, a
  Slack and email message is sent.
* Consignment export results. When the consignment export task finishes, a Slack message is sent with details of whether
  the export succeeded or failed. A message is also sent, to a Digital Archiving SNS topic for other Digital Archiving services to pick up and act upon.
* Generic message event. This allows a service to send message text which is sent directly to Slack with no other processing.

## Run locally

Set these environment variables, either on the command line or in IntelliJ depending on how you want to run the app:

* `SLACK_WEBHOOK`: the webhook URL of a Slack app. You can [create a new app][Slack-app] (steps 1-3) in Slack for testing purposes.
  Use the `#bot-testing` channel rather than a team channel to avoid confusion and spam. This webhook goes to the #da-tdr-notifications channel.
* `SLACK_JUDGMENT_WEBHOOK` This webhook publishes to the #da-tdr-prod-exports-judgments channel. This should only be used to send production judgment transfer notifications.
* `SLACK_TDR_WEBHOOK` This webhook publishes to the #da-tdr channel. This is for priority notifications which need to be acted on quickly.
* `SLACK_EXPORT_WEBHOOK` This webhook publishes to the #da-tdr-export-notifications channel. This is for all non-judgment export notifications for staging and production.
* `TO_EMAIL`: the email address that alerts should be sent to. For testing purposes, this should normally be your own
  email address rather than a team one.
* `DA_EVENT_BUS`: this is the SNS topic where successful export messages are published. Need to ensure have permissions to publish to the SNS topic
* `ENVIRONMENT`: the environment to which to send GOVUK emails

The app uses AWS services like Simple Email Service (SES) in the management account, so you will also need to update
your AWS credentials file with temporary mgmt credentials.

Register your email address with SES, then click the link in the verification email:

```
aws ses verify-email-identity --email-address "your.name@nationalarchives.gov.uk"
```

Then run the LambdaRunner app from IntelliJ, or run `sbt run` on the command line.

## Adding new environment variables to the tests
The environment variables in the deployed lambda are encrypted using KMS and then base64 encoded. These are then decoded in the lambda. Because of this, any variables in `src/test/resources/application.conf` which come from environment variables in `src/main/resources/application.conf` need to be stored base64 encoded. There are comments next to each variable to say what the base64 string decodes to. If you want to add a new variable you can run `echo -n "value of variable" | base64 -w 0` and paste the output into the test application.conf

## GOVUK Notify API Key Rotation

These messages rely on AWS SSM parameter policies to trigger AWS EventBridge.

Terraform does not support AWS SSM parameter policies currently. There these policies need to be manually set against the relevant SSM parameters:
* `/intg/keycloak/govuk_notify/api_key`
* `/staging/keycloak/govuk_notify/api_key`
* `/prod/keycloak/govuk_notify/api_key`

The policy to set is:
* Policy Type: "Notify If No Change Happened" (`ǸoChangeNotification`)
* Policy Value: "90"
* Value Type: "Days"

[Slack-app]: https://api.slack.com/messaging/webhooks
