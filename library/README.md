# Overview

A Java/Groovy library that handles CloudHub/On-Prem Mule app deployment and Design Center sync'ing.

Automatically handles failed/undeployed app cases and also waits for app to start before returning control.

# Usage

## "Install"

Just include this dependency in your POM. See [AVIO Nexus](https://devops.avioconsulting.com/nexus/#browse/browse:avio-releases:com%2Favioconsulting%2Fmule%2Fmule-deploy-library) for the latest version.

## Create an Anypoint user with the right permissions

Roles:
* Cloudhub Admin (DEV/TST/PRD) - to deploy the actual app
* Exchange Administrators - to publish Exchange assets

Permissions:
* Design Center Developer permission - to update Design Center
* API Manager For each environment (DEV/TST/PRD):
    * API Manager Environment Administrator
    * Manage APIs Configuration
    * Manage Contracts
    * Manage Policies
    * View APIs Configuration
    * View Contracts
    * View Policies

## In your code

You'll want to start by instantiating the `Deployer` class and then calling the appropriate methods.

Javadocs are published on [AVIO Jenkins](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/).

NOTE: This library assumes that whatever logger mechanism you provide via The `ILogger logger` parameter hides ANY credentials or secrets you provide this. If you use Jenkins with its credentials plugin, it will handle this but make sure SOMETHING handles this.

There is also a DSL framework included in the library. You can see the CLI and Maven plugin modules, which are siblings to this one, for how to use it.

# Development/building/maintenance

## Required development environment
1. JDK - 1.8.0_191 works OK for running the tests as does JDK11. JDK ~> 1.8.0_232 seems to cause issues with the test web server and `CompleteableFuture` (`SocketClosed` exceptions). It's recommended to either use JDK 1.8 <= 191 or use >= 11 to run the unit tests.
2. Maven
3. In IntelliJ, make sure you disable the embedded Maven installation and use a system one. Otherwise the Maven invoker in the tests may not work.

## Maintenance

This is largely just a standard Maven project. There are a couple ENUMs that should be kept up to date.
1. `com.avioconsulting.mule.deployment.api.models.AwsRegions` - [Javadoc](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/groovydocs/com/avioconsulting/mule/deployment/api/models/AwsRegions.html)
2. `com.avioconsulting.mule.deployment.api.models.WorkerTypes` - [Javadoc](https://devops.avioconsulting.com/jenkins/job/Mulesoft%20Deployment/job/mule-deploy-library/job/master/Maven_20site/groovydocs/com/avioconsulting/mule/deployment/api/models/WorkerTypes.html)

# Features

# enabledFeatures

In order to keep the plugin customizable for each scenario, the application offers a configuration to identify which feature is allowed to execute.
Currently, the application supports the following features:

1. AppDeployment: This feature tells the library to perform the application deployment into CloudHub/On-Prem instance 
2. DesignCenterSync: Used to publish the local RAML files to Design Center. Needed to set the data of API on `apiSpec` section 
3. ApiManagerDefinitions: Perform the synchronization for the API Manager specification. The details are retrieved within `apiSpec` section
4. PolicySync: Apply the policies specified in the Groovy file into the API manager. The specification of rules must be placed in the `policies` section

Note: DesignCenterSync, ApiManagerDefinitions and PolicySync all are dependent on `apiSpec`, so any of these options only executes if there is `apiSpec` 

Instead of specify all features in the Groovy file, the library offers `all` option to let the plugin knows that should try to perform all features.
Also, if no one features is specified in the enabledFeatures section, the library will assume `all` option.

*Important: When specify `all`, is not permitted to add any other option, because the application is understanding that should do all operations.*

Below are some examples of how to specify enabledFeatures section in the Groovy file.
1. Using all option
```Groovy
    enabledFeatures {
        all
    }
```
2. Using only appDeployment and designCenterSync feature
```Groovy
    enabledFeatures {
        all
        // or you can do this and specify a list of what you want enabled
        appDeployment
        designCenterSync
    }
```
3. Explicitly all features by name
```Groovy
    enabledFeatures {
        appDeployment
        designCenterSync
        apiManagerDefinitions
        policySync
    }
```

More detailed examples of how to specify these tags are available on [examples folder](./examples).