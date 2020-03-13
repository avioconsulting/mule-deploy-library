# Overview

A Java/Groovy library that handles CloudHub/On-Prem Mule app deployment and Design Center sync'ing.

Automatically handles failed/undeployed app cases and also waits for app to start before returning control.

# Usage

## "Install"

Just include this dependency in your POM. See [AVIO Nexus](https://devops.avioconsulting.com/nexus/#browse/browse:avio-releases:com%2Favioconsulting%2Fmule%2Fmule-deploy-library) for the latest version.

## Create an Anypoint user with the right permissions

* Apps - Cloudhub Admin (ENV) Role
* Design Center/Exchange - Design Center Developer permission and Exchange Administrators role

## In your code

You'll want to start by instantiating the `Deployer` class and then calling the appropriate methods.

Javadocs are published on [AVIO Jenkins](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/).

NOTE: This library assumes that whatever logger mechanism you provide via The `PrintStream logger` parameter hides ANY credentials or secrets you provide this. If you use Jenkins with its credentials plugin, it will handle this but make sure SOMETHING handles this.

# Development/building/maintenance

## Required development environment
1. JDK - 1.8.0_191 works OK for running the tests as does JDK11. JDK ~> 1.8.0_232 seems to cause issues with the test web server and `CompleteableFuture` (`SocketClosed` exceptions). It's recommended to either use JDK 1.8 <= 191 or use >= 11 to run the unit tests.
1. Maven

## Maintenance

This is largely just a standard Maven project. There are a couple ENUMs that should be kept up to date.
1. `com.avioconsulting.mule.deployment.api.models.AwsRegions` - [Javadoc](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/groovydocs/com/avioconsulting/mule/deployment/api/models/AwsRegions.html)
2. `com.avioconsulting.mule.deployment.api.models.WorkerTypes` - [Javadoc](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/groovydocs/com/avioconsulting/mule/deployment/api/models/WorkerTypes.html)
