package com.avioconsulting.jenkins.mule.impl

import groovy.json.JsonOutput
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class CloudHubDeployer extends BaseDeployer {
    /***
     * Instantiate using default anypoint.mulesoft.com URL
     * @param anypointOrganizationId
     * @param username
     * @param password
     * @param logger - will be used for all your log messages
     */
    CloudHubDeployer(String anypointOrganizationId,
                     String username,
                     String password,
                     PrintStream logger) {
        this('https://anypoint.mulesoft.com/',
             anypointOrganizationId,
             username,
             password,
             60000,
             // for CloudHub, the deploy cycle is longer so we wait longer
             30,
             logger)
    }

    CloudHubDeployer(String baseUrl,
                     String anypointOrganizationId,
                     String username,
                     String password,
                     int retryIntervalInMs,
                     int maxTries,
                     PrintStream logger) {
        super(baseUrl,
              anypointOrganizationId,
              username,
              password,
              retryIntervalInMs,
              maxTries,
              logger)
    }

    /**
     * Perform a "GAV style" deployment where the app JAR has been previously pushed to Anypoint Exchange
     * @param environment - environment name (e.g. DEV, not GUID)
     * @param appName - Used for deployment AND for the artifact ID from Exchange
     * @param cloudHubAppPrefix - Your "DNS prefix" for Cloudhub app uniqueness, usually a 3 letter customer ID
     * @param groupId - Used to retrieve ZIP/JAR from Exchange
     * @param version - Used to retrieve ZIP/JAR from Exchange
     * @param cryptoKey - Will be set in the 'crypto.key' CloudHub property
     * @param muleVersion
     * @param usePersistentQueues
     * @param workerType
     * @param workerCount
     * @param anypointClientId - will be set in the anypoint.platform.client_id CloudHub property
     * @param anypointClientSecret - will be set in the anypoint.platform.client_secret CloudHub property
     * @param awsRegion - by default will use what's configured in Runtime Manager if you don't supply one
     * @param otherCloudHubProperties - CloudHub level property overrides (e.g. region type stuff)
     * @param appProperties - Mule app property overrides (the stuff in the properties tab)
     */
    def deployFromExchange(String environment,
                           String appName,
                           String cloudHubAppPrefix,
                           String groupId,
                           String version,
                           String cryptoKey,
                           String muleVersion,
                           boolean usePersistentQueues,
                           WorkerTypes workerType,
                           int workerCount,
                           String anypointClientId,
                           String anypointClientSecret,
                           AwsRegions awsRegion = null,
                           Map<String, String> otherCloudHubProperties = [:],
                           Map<String, String> appProperties = [:]) {
        def artifactId = appName
        appName = normalizeAppName(appName,
                                   cloudHubAppPrefix,
                                   environment)
        authenticate()
        def environmentId = locateEnvironment(environment)
        def existingAppStatus = getAppStatus(environmentId,
                                             appName)
        def fileName = MuleUtil.getFileName(artifactId,
                                            version,
                                            muleVersion)
        def request = getDeploymentHttpRequest(existingAppStatus,
                                               appName,
                                               fileName,
                                               environmentId)
        doDeploymentViaExchange(request,
                                environment,
                                environmentId,
                                appName,
                                artifactId,
                                groupId,
                                version,
                                fileName,
                                cryptoKey,
                                muleVersion,
                                awsRegion,
                                usePersistentQueues,
                                workerType,
                                workerCount,
                                otherCloudHubProperties,
                                anypointClientId,
                                anypointClientSecret,
                                appProperties)
        waitForAppToStart(environmentId,
                          appName)
    }

    /**
     * Deploy via a supplied file
     * @param environment - environment name (e.g. DEV, not GUID)
     * @param appName
     * @param cloudHubAppPrefix - Your "DNS prefix" for Cloudhub app uniqueness, usually a 3 letter customer ID
     * @param zipFile - stream of the app ZIP contents
     * @param fileName - The filename to display in the Runtime Manager app GUI. Often used as a version for a label
     * @param cryptoKey - Will be set in the 'crypto.key' CloudHub property
     * @param muleVersion
     * @param awsRegion
     * @param usePersistentQueues
     * @param workerType
     * @param workerCount
     * @param anypointClientId - will be set in the anypoint.platform.client_id CloudHub property
     * @param anypointClientSecret - will be set in the anypoint.platform.client_secret CloudHub property
     * @param awsRegion - by default will use what's configured in Runtime Manager if you don't supply one
     * @param appProperties - Mule app property overrides (the stuff in the properties tab)
     * @param otherCloudHubProperties - CloudHub level property overrides (e.g. region type stuff)
     * @param overrideByChangingFileInZip - VERY rare. If you have a weird situation where you need to be able to say that you "froze" an app ZIP/JAR for config management purposes and you want to change the properties inside a ZIP file, set this to the filename you want to drop new properties in inside the ZIP (e.g. api.dev.properties)
     * @return
     */
    def deployFromFile(String environment,
                       String appName,
                       String cloudHubAppPrefix,
                       InputStream zipFile,
                       String fileName,
                       String cryptoKey,
                       String muleVersion,
                       boolean usePersistentQueues,
                       WorkerTypes workerType,
                       int workerCount,
                       String anypointClientId,
                       String anypointClientSecret,
                       AwsRegions awsRegion = null,
                       Map<String, String> appProperties = [:],
                       Map<String, String> otherCloudHubProperties = [:],
                       String overrideByChangingFileInZip = null) {
        appName = normalizeAppName(appName,
                                   cloudHubAppPrefix,
                                   environment)
        authenticate()
        def environmentId = locateEnvironment(environment)
        def existingAppStatus = getAppStatus(environmentId,
                                             appName)
        def request = getDeploymentHttpRequest(existingAppStatus,
                                               appName,
                                               fileName,
                                               environmentId)
        doDeployment(request,
                     environment,
                     environmentId,
                     appName,
                     zipFile,
                     fileName,
                     cryptoKey,
                     muleVersion,
                     awsRegion,
                     usePersistentQueues,
                     workerType,
                     workerCount,
                     otherCloudHubProperties,
                     anypointClientId,
                     anypointClientSecret,
                     appProperties,
                     overrideByChangingFileInZip)
        waitForAppToStart(environmentId,
                          appName)
    }

    private HttpEntityEnclosingRequestBase getDeploymentHttpRequest(AppStatus existingAppStatus,
                                                                    String appName,
                                                                    String fileName,
                                                                    String environmentId) {
        HttpEntityEnclosingRequestBase request
        if (existingAppStatus == AppStatus.NotFound) {
            logger.println "Deploying '${appName}', ${fileName} as a NEW application"
            request = new HttpPost("${baseUrl}/cloudhub/api/v2/applications")
        } else if ([AppStatus.Undeployed, AppStatus.Failed].contains(existingAppStatus)) {
            // If you try and PUT a new version of an app over an existing failed deployment, CloudHub will reject it
            // so we delete first to clear out the failed deployment
            logger.println "Existing deployment of '${appName}' is in status '${existingAppStatus}' and is not currently running. Will remove first and then deploy a new copy"
            deleteApp(environmentId,
                      appName)
            waitForAppDeletion(environmentId,
                               appName)
            logger.println "App deleted, now deploying '${appName}', ${fileName} as a NEW application"
            request = new HttpPost("${baseUrl}/cloudhub/api/v2/applications")
        } else {
            logger.println "Deploying '${appName}', ${fileName} as an UPDATED application"
            request = new HttpPut("${baseUrl}/cloudhub/api/v2/applications/${appName}")
        }
        request
    }

    private String normalizeAppName(String appName,
                                    String cloudHubAppPrefix,
                                    String environment) {
        if (appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${appName}'!")
        }
        appName = "${cloudHubAppPrefix}-${appName}-${environment}"
        def appNameLowerCase = appName.toLowerCase()
        if (appNameLowerCase != appName) {
            logger.println "NOTE: Automatically lower casing app name of '${appName}' to avoid CloudHub mismatch problems"
            appName = appNameLowerCase
        }
        appName
    }

    private static Map<String, String> getCoreProperties(String appName,
                                                         String muleVersion,
                                                         AwsRegions awsRegion,
                                                         WorkerTypes workerType,
                                                         int workerCount,
                                                         boolean usePersistentQueues,
                                                         Map<String, String> props) {
        def result = [
                // CloudHub's API calls the Mule application the 'domain'
                domain               : appName,
                muleVersion          : [
                        version: muleVersion
                ],
                region               : awsRegion?.awsCode,
                monitoringAutoRestart: true,
                workers              : [
                        type  : [
                                name: workerType.toString()
                        ],
                        amount: workerCount
                ],
                persistentQueues     : usePersistentQueues,
                // these are the actual properties in the 'Settings' tab
                properties           : props
        ] as Map<String, String>
        if (!result.region) {
            // use default/runtime manager region
            result.remove('region')
        }
        result
    }

    private def doDeployment(HttpEntityEnclosingRequestBase request,
                             String environment,
                             String environmentId,
                             String appName,
                             InputStream zipFile,
                             String fileName,
                             String cryptoKey,
                             String muleVersion,
                             AwsRegions awsRegion,
                             boolean usePersistentQueues,
                             WorkerTypes workerType,
                             int workerCount,
                             Map otherProperties,
                             String anypointClientId,
                             String anypoingClientSecret,
                             Map<String, String> propertyOverrideMap,
                             String overrideByChangingFileInZip) {
        def props = [
                // env in on-prem environment is lower cased
                env                              : environment.toLowerCase(),
                'crypto.key'                     : cryptoKey,
                'anypoint.platform.client_id'    : anypointClientId,
                'anypoint.platform.client_secret': anypoingClientSecret
        ]
        if (otherProperties.containsKey('properties')) {
            otherProperties.properties = props + otherProperties.properties
        }
        def appInfo = getCoreProperties(appName,
                                        muleVersion,
                                        awsRegion,
                                        workerType,
                                        workerCount,
                                        usePersistentQueues,
                                        props) + otherProperties
        if (!overrideByChangingFileInZip) {
            appInfo.properties = appInfo.properties + propertyOverrideMap
        } else {
            zipFile = modifyZipFileWithNewProperties(zipFile,
                                                     fileName,
                                                     overrideByChangingFileInZip,
                                                     propertyOverrideMap)
        }
        def appInfoJson = JsonOutput.toJson(appInfo)
        logger.println "Deploying using settings: ${JsonOutput.prettyPrint(appInfoJson)}"
        request = request.with {
            addStandardStuff(it,
                             environmentId)
            def entity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
            // without autoStart, the app won't actually start after we push this request out
                    .addTextBody('autoStart',
                                 'true')
                    .addTextBody('appInfoJson',
                                 appInfoJson)
                    .addBinaryBody('file',
                                   zipFile,
                                   ContentType.APPLICATION_OCTET_STREAM,
                                   fileName)
                    .build()
            setEntity(entity)
            it
        }
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'deploy application')
            logger.println("Application '${appName}' has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
        }
        finally {
            response.close()
        }
    }

    private def doDeploymentViaExchange(HttpEntityEnclosingRequestBase request,
                                        String environment,
                                        String environmentId,
                                        String appName,
                                        String artifactId,
                                        String groupId,
                                        String version,
                                        String filename,
                                        String cryptoKey,
                                        String muleVersion,
                                        AwsRegions awsRegion,
                                        boolean usePersistentQueues,
                                        WorkerTypes workerType,
                                        int workerCount,
                                        Map otherProperties,
                                        String anypointClientId,
                                        String anypoingClientSecret,
                                        Map propertyOverrideMap) {
        def props = [
                // env in on-prem environment is lower cased
                env                              : environment.toLowerCase(),
                'crypto.key'                     : cryptoKey,
                'anypoint.platform.client_id'    : anypointClientId,
                'anypoint.platform.client_secret': anypoingClientSecret
        ]
        if (otherProperties.containsKey('properties')) {
            otherProperties.properties = props + otherProperties.properties
        }
        def appInfo = getCoreProperties(appName,
                                        muleVersion,
                                        awsRegion,
                                        workerType,
                                        workerCount,
                                        usePersistentQueues,
                                        props) + [
                // this way the version of the app shows up in Runtime Manager
                fileName: filename
        ] + otherProperties
        appInfo.properties = appInfo.properties + propertyOverrideMap
        def appSource = [
                groupId   : groupId,
                artifactId: artifactId,
                version   : version,
                source    : 'EXCHANGE'
        ]
        def payload = [
                applicationSource: appSource,
                applicationInfo  : appInfo,
                autoStart        : true
        ]
        def payloadJson = JsonOutput.toJson(payload)
        logger.println "Deploying using settings: ${JsonOutput.prettyPrint(payloadJson)}"
        request = request.with {
            addStandardStuff(it,
                             environmentId)
            def entity = new StringEntity(payloadJson,
                                          ContentType.APPLICATION_JSON)
            setEntity(entity)
            it
        }
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'deploy application')
            logger.println("Application '${appName}' has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
        }
        finally {
            response.close()
        }
    }

    def deleteApp(String environmentId,
                  String appName) {
        def request = new HttpDelete("${baseUrl}/cloudhub/api/v2/applications/${appName}").with {
            addStandardStuff(it,
                             environmentId)
            it
        } as HttpDelete
        def response = httpClient.execute(request)
        try {
            assertSuccessfulResponse(response,
                                     'Removing failed app deployment')
        }
        finally {
            response.close()
        }
    }

    def waitForAppDeletion(String environmentId,
                           String appName) {
        def tries = 0
        def deleted = false
        def failed = false
        logger.println 'Now checking to see if app has been deleted'
        while (!deleted && tries < this.maxTries) {
            tries++
            logger.println "*** Try ${tries} ***"
            AppStatus status = getAppStatus(environmentId,
                                            appName)
            logger.println "Received status of ${status}"
            if (status == AppStatus.NotFound) {
                logger.println 'App removed successfully!'
                deleted = true
                break
            }
            logger.println "Sleeping for ${this.retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(this.retryIntervalInMs)
        }
        if (!deleted && failed) {
            throw new Exception('Deletion failed on 1 or more nodes. Please see logs and messages as to why app did not start')
        }
        if (!deleted) {
            throw new Exception("Deletion has not completed after ${tries} tries!")
        }
    }

    def waitForAppToStart(String environmentId,
                          String appName) {
        logger.println 'Now will wait for application to start...'
        def tries = 0
        def deployed = false
        def failed = false
        while (!deployed && tries < this.maxTries) {
            tries++
            logger.println "*** Try ${tries} ***"
            Set<DeploymentStatus> status = getDeploymentStatus(environmentId,
                                                               appName)
            logger.println "Received statuses of ${status}"
            if (status.toList() == [DeploymentStatus.STARTED]) {
                logger.println 'App started successfully!'
                deployed = true
                break
            }
            if (status.contains(DeploymentStatus.FAILED)) {
                failed = true
                logger.println 'Deployment FAILED on 1 more nodes!'
                break
            }
            logger.println "Sleeping for ${this.retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(this.retryIntervalInMs)
        }
        if (!deployed && failed) {
            throw new Exception('Deployment failed on 1 or more workers. Please see logs and messages as to why app did not start')
        }
        if (!deployed) {
            throw new Exception("Deployment has not failed but app has not started after ${tries} tries!")
        }
    }

    AppStatus getAppStatus(String environmentId,
                           String appName) {
        def request = new HttpGet("${baseUrl}/cloudhub/api/v2/applications/${appName}").with {
            addStandardStuff(it,
                             environmentId)
            it
        } as HttpGet
        def response = httpClient.execute(request)
        try {
            if (response.statusLine.statusCode == 404) {
                return AppStatus.NotFound
            }
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'app status')
            def mapStatus = { String input ->
                switch (input) {
                    case 'STARTED':
                        return AppStatus.Started
                    case 'DEPLOY_FAILED':
                        return AppStatus.Failed
                    case 'UNDEPLOYED':
                        return AppStatus.Undeployed
                    default:
                        return AppStatus.Unknown
                }
            }
            return mapStatus(result.status)
        }
        finally {
            response.close()
        }
    }

    Set<DeploymentStatus> getDeploymentStatus(String environmentId,
                                              String appName) {
        def request = new HttpGet("${baseUrl}/cloudhub/api/v2/applications/${appName}/deployments?orderByDate=DESC").with {
            addStandardStuff(it,
                             environmentId)
            it
        } as HttpGet
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'deployment status')
            def data = result.data
            // we used order by desc date
            def lastChronologicalDeployment = data[0]
            def uniqueStatuses = lastChronologicalDeployment.instances.status.unique()
            def mapStatus = { String input ->
                switch (input) {
                    case 'STARTED':
                        return DeploymentStatus.STARTED
                    case 'TERMINATED':
                        return DeploymentStatus.FAILED
                    case 'DEPLOYING':
                        return DeploymentStatus.STARTING
                    default:
                        return DeploymentStatus.UNKNOWN
                }
            }
            return uniqueStatuses.collect { str -> mapStatus(str) }
        }
        finally {
            response.close()
        }
    }
}
