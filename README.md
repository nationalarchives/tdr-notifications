# TDR Notifications

This project is for sending slack/email messages in response to cloudwatch events. It supports several types of event:

* ECR scan results. Each time there is an ECR repository scan, the scan results are checked. If there are any errors, a
  Slack and email message is sent.
* Maintenance window results. When the Jenkins backup maintenance window runs, the results are checked and if it fails,
  a notification is sent.
* Consignment export results. When the consignment export task finishes, a Slack message is sent with details of whether
  the export succeeded or failed. A message is also sent, if it is a `judgment` export, to a SQS queue belonging to the transformation engine.

## Run locally

Set these environment variables, either on the command line or in IntelliJ depending on how you want to run the app:

* `SLACK_WEBHOOK`: the webhook URL of a Slack app. You can [create a new app][Slack-app] (steps 1-3) in Slack for testing purposes.
  Use the `#bot-testing` channel rather than a team channel to avoid confusion and spam.
* `TO_EMAIL`: the email address that alerts should be sent to. For testing purposes, this should normally be your own
  email address rather than a team one.
* `TRANSFORM_ENGINE_OUTPUT_SQS`: set this it the SQS queue where the message should be sent. Need to ensure have permissions to send to the SQS queue

The app uses AWS services like Simple Email Service (SES) in the management account, so you will also need to update
your AWS credentials file with temporary mgmt credentials.

Register your email address with SES, then click the link in the verification email:

```
aws ses verify-email-identity --email-address "your.name@nationalarchives.gov.uk"
```

Then run the LambdaRunner app from IntelliJ, or run `sbt run` on the command line.

## Adding new environment variables to the tests
The environment variables in the deployed lambda are encrypted using KMS and then base64 encoded. These are then decoded in the lambda. Because of this, any variables in `src/test/resources/application.conf` which come from environment variables in `src/main/resources/application.conf` need to be stored base64 encoded. There are comments next to each variable to say what the base64 string decodes to. If you want to add a new variable you can run `echo -n "value of variable" | base64 -w 0` and paste the output into the test application.conf

[Slack-app]: https://api.slack.com/messaging/webhooks
