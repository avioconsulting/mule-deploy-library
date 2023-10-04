package com.avioconsulting.mule.deployment.api.models.deployment

import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest
import com.avioconsulting.mule.deployment.internal.models.CloudhubAppProperties
import com.avioconsulting.mule.deployment.secure.PropertiesObfuscator
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import groovy.transform.ToString
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

@ToString
class CloudhubDeploymentRequest extends FileBasedAppDeploymentRequest {
    /**
     * CloudHub specs
     */
    final CloudhubWorkerSpecRequest workerSpecRequest
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
     * Mule app property overrides (the stuff in the properties tab)
     */
    final Map<String, String> appProperties
    /**
     * CloudHub level property overrides (e.g. region type stuff)
     */
    final Map<String, String> otherCloudHubProperties

    /***
     * Sets anypoint.platform.config.analytics.agent.enabled to true in CH props
     * False by default
     */
    final boolean analyticsAgentEnabled

    private CloudhubAppProperties cloudhubAppProperties

    /**
     * Construct a "standard" request. See properties for parameter info.
     */
    CloudhubDeploymentRequest(String environment,
                              CloudhubWorkerSpecRequest workerSpecRequest,
                              File file,
                              String cryptoKey,
                              String anypointClientId,
                              String anypointClientSecret,
                              ApplicationName applicationName,
                              String appVersion = null,
                              Map<String, String> appProperties = [:],
                              Map<String, String> otherCloudHubProperties = [:],
                              boolean analyticsAgentEnabled = true) {
        super(file, applicationName, appVersion, environment)
        if (!workerSpecRequest.muleVersion) {
            def propertyToUse = mule4Request ? 'app.runtime' : 'mule.version'
            def rawVersion = parsedPomProperties.props[propertyToUse]
            // Studio will modify some projects with 4.2.2-hf2. The -hf2 part is meaningless to CloudHub
            // because it's done in the form of update IDs. It's better to just remove it
            rawVersion = rawVersion.split('-')[0]
            this.workerSpecRequest = workerSpecRequest.withNewMuleVersion(rawVersion)
        } else {
            this.workerSpecRequest = workerSpecRequest
        }
        this.cryptoKey = cryptoKey
        this.anypointClientId = anypointClientId
        this.anypointClientSecret = anypointClientSecret
        this.appProperties = appProperties
        this.otherCloudHubProperties = otherCloudHubProperties

        //normalize name

        //
        this.cloudhubAppProperties = new CloudhubAppProperties(applicationName.baseAppName,
                                                               environment.toLowerCase(),
                                                               cryptoKey,
                                                               anypointClientId,
                                                               anypointClientSecret,
                                                               // only include prop if it's true
                                                               analyticsAgentEnabled ? true : null)
        this.analyticsAgentEnabled = analyticsAgentEnabled
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

    Map<String, String> getCloudhubAppInfo() {
        def props = new ObjectMapper().convertValue(this.cloudhubAppProperties,
                                                    Map)
        props += this.autoDiscoveries
        def result = [
                // CloudHub's API calls the Mule application the 'domain'
                domain                   : this.applicationName.normalizedAppName,
                muleVersion              : workerSpecRequest.versionInfo,
                region                   : workerSpecRequest.awsRegion?.awsCode,
                monitoringAutoRestart    : true,
                workers                  : [
                        type  : [
                                name: workerSpecRequest.workerType.toString()
                        ],
                        amount: workerSpecRequest.workerCount
                ],
                staticIPsEnabled         : workerSpecRequest.staticIpEnabled,
                loggingCustomLog4JEnabled: workerSpecRequest.customLog4j2Enabled,
                objectStoreV1            : !workerSpecRequest.objectStoreV2Enabled,
                persistentQueues         : workerSpecRequest.usePersistentQueues,
                // these are the actual properties in the 'Settings' tab
                properties               : props
        ] as Map<String, String>
        if (!result.region) {
            // use default/runtime manager region
            result.remove('region')
        }
        if (otherCloudHubProperties.containsKey('properties')) {
            otherCloudHubProperties.properties = props + otherCloudHubProperties.properties
        }
        def appInfo = result + otherCloudHubProperties
        appInfo.properties = appInfo.properties + appProperties
        appInfo
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(cloudhubAppInfo)
    }

    Map<String,String> getCloudAppInfoAsObfuscatedJson() {
        PropertiesObfuscator.obfuscateMap(cloudhubAppInfo,"properties")
    }

}
