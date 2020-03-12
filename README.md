# Overview

A Java/Groovy library that handles CloudHub/On-Prem Mule app deployment and Design Center sync'ing.

Automatically handles failed/undeployed app cases and also waits for app to start before returning control.

# Usage

## "Install"

Just include this dependency in your POM. See [AVIO Nexus](https://devops.avioconsulting.com/nexus/#browse/browse:avio-releases:com%2Favioconsulting%2Fmule%2Fmule-deploy-library) for the latest version.

## Create an Anypoint user with the right permissions

* Apps - Cloudhub Admin (ENV) Role
* Design Center/Exchange - Design Center Developer permission and Exchange Administrators role

## Code

You'll want to start by instantiating the `Deployer` class and then calling the appropriate methods.

Javadocs are published on [AVIO Jenkins](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/).

# Development/building/maintenance

This is largely just a standard Maven project. There are a couple ENUMs that should be kept up to date.
1. `com.avioconsulting.mule.deployment.models.AwsRegions` - [Javadoc](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/groovydocs/com/avioconsulting/jenkins/mule/impl/AwsRegions.html)
2. `com.avioconsulting.mule.deployment.models.WorkerTypes` - [Javadoc](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/groovydocs/com/avioconsulting/jenkins/mule/impl/WorkerTypes.html)
