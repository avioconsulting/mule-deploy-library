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

## enabledFeatures

In order to keep the plugin customizable for each scenario, the application offers a configuration to identify which feature is allowed to execute.
Currently, the application supports the following features:

### AppDeployment
This feature tells the library to perform the application deployment into CloudHub/On-Prem/CloubHub 2.0/RTF instance.

For CloudHub version 1 and On-premise deployment, is necessary to specify where is the file for the application, that is used to deploy into the server. 

For CloudHub version 2 and RTF deployment, the application only sends the GAV in the request, because the deployment process is done doing the download of the artifact from the Exchange. With that being said, is required to have the application available in Exchange before this process.

In all cases, the library is capable of either doing a new deployment or updating an existing deployed application. 

### DesignCenterSync
Using this flag, the library will extract all RAML files from the source directory (provided in `apiSpec`), find the project in the design center by the name to be able to retrieve the current content in the server,
then will perform a comparison in the files, to find new, changed, and deleted files locally, and finally will do the synchronization with the server for all changes.

The last step is pushing the latest version to Exchange the RAML files, using the version provided in the API specification.

**Limitations**
- Not supported when API is SOAP
- Not supported when using CloudHub 2.0 and RTF deployment

### ApiManagerDefinitions
Based on `exchangeAssetId` the library searches for the API definition in the API manager, in case of already exists one API definition
for the version, the application will perform a synchronization on the API manager server using the configuration provided in the `apiSpec` section,
updating the settings to match the definition from the file. 

In case the API definition still not exists in the API manager server, the library will create it
using the provided configuration.

**Limitations**
- Not supported when using CloudHub 2.0 and RTF deployment

### PolicySync
Enabling this flag, the library will read all specification from the `policies` section, where the list of policies is detailed. 
With that the application retrieves all current policies in the API manager, then do a loop deleting everything in the server
and creating the specified policies in the Groovy file.

**Limitations**
- Not supported when using CloudHub 2.0 and RTF deployment

### All
Instead of specifying all features in the Groovy file, the library offers an `all` option to let the plugin knows that should try to perform all features.
Also, if no one feature is specified in the enabledFeatures section, the library will assume the `all` option.

*Important: When specifying `all`, is not permitted to add any other option, because the application is understanding that should do all operations.*

Note: DesignCenterSync, ApiManagerDefinitions, and PolicySync all are dependent on `apiSpec`, so any of these options only executes if there is `apiSpec`

### Examples
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

## apiSpec
This is the section necessary to specify attributes about the API to be used in Design Center, API Manager.

### name
The name of your Design Center project and also the desired API definition name.

### exchangeAssetId
The exchange-asset-id to be used. By default, it will be derived from the `name`, converted to lower case and replaced all spaces by dash (-).

Examples:
* Product API --> product-api
* SystemStuff API --> systemstuff-api

### mainRamlFile
What is the "main RAML" file in your app? If not specified, it will be the first RAML round at the "root" of your `sourceDirectory`.

### endpoint
The endpoint to show in the API Manager definition

### autoDiscoveryPropertyName
The application property that will be set by the deployer (after upserting the API definition) so that the app's autodiscovery element knows what the API ID is. Defaults to 'auto-discovery.api-id'.

### designCenterBranchName
Which design center branch should be updated from app (and published to Exchange). Default is master.

### sourceDirectory
Which source directory in your app code should be used to sync to Design Center? By default, this is the '/api' directory which ends up inside the JAR from being in src/main/resources.

### soapEndpointWithVersion
Major API Version for SOAP definition, used to synchronize with Design Center. When defined value, will assume it's a SOAP API instead of REST.