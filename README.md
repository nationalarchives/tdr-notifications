## TDR Notifications

This project is for sending slack/email messages in response to cloudwatch events. It supports several types of event:

* ECR scan results. Each time there is an ECR repository scan, the scan results are checked. If there are any errors, a
  Slack and email message is sent.
* Maintenance window results. When the Jenkins backup maintenance window runs, the results are checked and if it fails,
  a notification is sent.
* Consignment export results. When the consignment export task finishes, a Slack message is sent with details of whether
  the export succeeded or failed.
