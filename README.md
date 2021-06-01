# Threema plugin for Jenkins

Based on a fork of the Mattermost plugin:

https://github.com/jenkinsci/mattermost-plugin/

Which was a a fork of the Slack plugin:

https://github.com/jenkinsci/slack-plugin/

Which was, in turn, a fork of the HipChat plugin:

https://github.com/jlewallen/jenkins-hipchat-plugin

Which was, in turn, a fork of the Campfire plugin.

Includes [Jenkins Pipeline](https://github.com/jenkinsci/workflow-plugin) support as of version 2.0:

```
mattermostSend color: 'good', message: 'Message from Jenkins Pipeline', text: 'optional for @here mentions and searchable text'
```

# Jenkins Instructions

1. Setup an account with https://gateway.threema.ch
2. Install this plugin on your Jenkins server
3. Setup a credentials cotaining your Threema id and API secret   
4. **Add it as a Post-build action** in your Jenkins job.

# Developer instructions

Install Maven and JDK.

Run unit tests

    mvn test

Create an HPI file to install in Jenkins (HPI file will be in `target/threema-notification.hpi`).

    mvn package
