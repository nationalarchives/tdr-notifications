## TDR Notifications

This project is for sending slack/email messages in response to cloudwatch events. It currently supports two events.

* ECR scan results. Each time there is an ECR repository scan, the scan results are checked. If there are any errors, a slack and email message is sent.
* Maintenance window results. When the Jenkins backup maintenance window runs, the results are checked and if it fails, a notification is sent.    