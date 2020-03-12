package com.avioconsulting.mule.deployment.models

import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class CloudhubDeploymentRequest implements FileBasedAppDeploymentRequest {
    /**
     * environment name (e.g. DEV, not GUID)
     */
    final String environment
    final String appName
    final CloudhubWorkerSpecRequest workerSpecRequest
    /**
     * The file to deploy. The name of this file will also be used for the Runtime Manager settings pane
     */
    final File file
    /**
     * Will be set in the 'crypto.key' CloudHub property
     */
    final String cryptoKey
    /**
     * will be set in the anypoint.platform.client_id CloudHub property
     */
    final String anypointClientId
    /**
     * will be set in the anypoint.platform.client_secret CloudHub property
     */
    final String anypointClientSecret
    /**
     * Your "DNS prefix" for Cloudhub app uniqueness, usually a 3 letter customer ID
     */
    final String cloudHubAppPrefix
    /**
     * Mule app property overrides (the stuff in the properties tab)
     */
    final Map<String, String> appProperties
    /**
     * CloudHub level property overrides (e.g. region type stuff)
     */
    final Map<String, String> otherCloudHubProperties
    /**
     * VERY rare. If you have a weird situation where you need to be able to say that you "froze" an app ZIP/JAR for config management purposes and you want to change the properties inside a ZIP file, set this to the filename you want to drop new properties in inside the ZIP (e.g. api.dev.properties)
     */
    final String overrideByChangingFileInZip
    /**
     * Derived from app, environment, and prefix, the real name we'll use in CH
     */
    final String normalizedAppName

    CloudhubDeploymentRequest(String environment,
                              String appName,
                              CloudhubWorkerSpecRequest workerSpecRequest,
                              File file,
                              String cryptoKey,
                              String anypointClientId,
                              String anypointClientSecret,
                              String cloudHubAppPrefix,
                              Map<String, String> appProperties = [:],
                              Map<String, String> otherCloudHubProperties = [:]) {
        this(environment,
             appName,
             workerSpecRequest,
             file,
             cryptoKey,
             anypointClientId,
             anypointClientSecret,
             cloudHubAppPrefix,
             appProperties,
             null,
             otherCloudHubProperties)
    }

    CloudhubDeploymentRequest(String environment,
                              String appName,
                              CloudhubWorkerSpecRequest workerSpecRequest,
                              File file,
                              String cryptoKey,
                              String anypointClientId,
                              String anypointClientSecret,
                              String cloudHubAppPrefix,
                              Map<String, String> appProperties,
                              String overrideByChangingFileInZip,
                              Map<String, String> otherCloudHubProperties = [:]) {
        this.environment = environment
        this.appName = appName
        this.workerSpecRequest = workerSpecRequest
        this.file = file
        this.cryptoKey = cryptoKey
        this.anypointClientId = anypointClientId
        this.anypointClientSecret = anypointClientSecret
        this.cloudHubAppPrefix = cloudHubAppPrefix
        this.appProperties = appProperties
        this.otherCloudHubProperties = otherCloudHubProperties
        this.overrideByChangingFileInZip = overrideByChangingFileInZip
        if (appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${appName}'!")
        }
        def newAppName = "${cloudHubAppPrefix}-${appName}-${environment}"
        def appNameLowerCase = newAppName.toLowerCase()
        if (appNameLowerCase != newAppName) {
            newAppName = appNameLowerCase
        }
        normalizedAppName = newAppName
//        this.app = overrideByChangingFileInZip ? getPropertyModifiedStream(overrideByChangingFileInZip,
//                                                                           appProperties,
//                                                                           app,
//                                                                           fileName) : app
    }

    HttpEntity getHttpPayload() {
        MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        // without autoStart, the app won't actually start after we push this request out
                .addTextBody('autoStart',
                             'true')
                .addTextBody('appInfoJson',
                             cloudhubAppInfoAsJson)
                .addBinaryBody('file',
                               this.file,
                               ContentType.APPLICATION_OCTET_STREAM,
                               this.file.name)
                .build()
    }

    private Map<String, String> getCloudhubProperties() {
        [
                // env in on-prem environment is lower cased
                env                              : environment.toLowerCase(),
                'crypto.key'                     : cryptoKey,
                'anypoint.platform.client_id'    : anypointClientId,
                'anypoint.platform.client_secret': anypointClientSecret
        ]
    }

    Map<String, String> getCloudhubAppInfo() {
        def props = getCloudhubProperties()
        def result = [
                // CloudHub's API calls the Mule application the 'domain'
                domain               : normalizedAppName,
                muleVersion          : [
                        version: workerSpecRequest.muleVersion
                ],
                region               : workerSpecRequest.awsRegion?.awsCode,
                monitoringAutoRestart: true,
                workers              : [
                        type  : [
                                name: workerSpecRequest.workerType.toString()
                        ],
                        amount: workerSpecRequest.workerCount
                ],
                persistentQueues     : workerSpecRequest.usePersistentQueues,
                // these are the actual properties in the 'Settings' tab
                properties           : props
        ] as Map<String, String>
        if (!result.region) {
            // use default/runtime manager region
            result.remove('region')
        }
        if (otherCloudHubProperties.containsKey('properties')) {
            otherCloudHubProperties.properties = props + otherCloudHubProperties.properties
        }
        def appInfo = result + otherCloudHubProperties
        if (!overrideByChangingFileInZip) {
            appInfo.properties = appInfo.properties + appProperties
        }
        appInfo
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(cloudhubAppInfo)
    }
}
