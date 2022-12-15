package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.internal.models.CloudhubAppProperties
import com.fasterxml.jackson.databind.ObjectMapper
import groovy.json.JsonOutput
import groovy.transform.ToString
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

@ToString
class CloudhubV2DeploymentRequest extends FileBasedAppDeploymentRequest {
    /**
     * environment name (e.g. DEV, not GUID)
     */
    final String environment
    /**
     * Actual name of your application WITHOUT any kind of customer/environment prefix or suffix. Spaces in the name are not allowed and will be rejected.
     * This parameter is optional. If you don't supply it, the <artifactId> from your app's POM will be used.
     */
    final String appName
    /**
     * CloudHub specs
     */
    final CloudhubV2WorkerSpecRequest workerSpecRequest
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
     * Your "DNS prefix" for Cloudhub app uniqueness, usually a 3 letter customer ID to ensure app uniqueness.
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
     * Get only property, derived from app, environment, and prefix, this the real application name that will be used in CloudHub to ensure uniqueness.
     */
    final String normalizedAppName
    /**
     * Version of the app you are deploying (e.g. <version> from the POM). This parameter is optional and if it's not supplied
     * then it will be derived from the <version> parameter in the project's POM based on the JAR/ZIP
     */
    final String appVersion

    /**
     * Version of the app you are deploying (e.g. <version> from the POM). This parameter is optional and if it's not supplied
     * then it will be derived from the <version> parameter in the project's POM based on the JAR/ZIP
     */
    final String groupId

    /**
     * The CloudHub 2.0 target name to deploy the app to.
     * Specify either a shared space or a private space available in your Deployment Target values in CloudHub 2.0
     */
    final String target

    /**
     * Id of target based on the target name
     */
    String targetId

    private CloudhubAppProperties cloudhubAppProperties

    /**
     * Construct a "standard" request. See properties for parameter info.
     */
    CloudhubV2DeploymentRequest(String environment,
                                CloudhubV2WorkerSpecRequest workerSpecRequest,
                                File file,
                                String cryptoKey,
                                String anypointClientId,
                                String anypointClientSecret,
                                String cloudHubAppPrefix,
                                String appName = null,
                                String appVersion = null,
                                Map<String, String> appProperties = [:],
                                Map<String, String> otherCloudHubProperties = [:]) {
        this.file = file
        this.environment = environment
        this.appName = appName ?: parsedPomProperties.artifactId
        this.appVersion = appVersion ?: parsedPomProperties.version
        this.groupId = groupId ?: parsedPomProperties.groupId
        this.target = workerSpecRequest.target
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
        this.cloudHubAppPrefix = cloudHubAppPrefix
        this.appProperties = appProperties
        this.otherCloudHubProperties = otherCloudHubProperties
        if (this.appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${this.appName}'!")
        }
        def newAppName = "${cloudHubAppPrefix}-${this.appName}-${environment}"
        def appNameLowerCase = newAppName.toLowerCase()
        if (appNameLowerCase != newAppName) {
            newAppName = appNameLowerCase
        }
        normalizedAppName = newAppName
        this.cloudhubAppProperties = new CloudhubAppProperties(this.appName,
                                                               environment.toLowerCase(),
                                                               cryptoKey,
                                                               anypointClientId,
                                                               anypointClientSecret,
                                                               null)
    }

    HttpEntity getHttpPayload() {
        MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                .addTextBody('appInfoJson',
                             cloudhubAppInfoAsJson,
                             ContentType.APPLICATION_JSON)
                .build()
    }

    Map<String, String> getCloudhubAppInfo() {
        def props = new ObjectMapper().convertValue(this.cloudhubAppProperties,
                                                    Map)
        props += this.autoDiscoveries
        def result = [
                // CloudHub's v2 API calls the Mule application the 'domain'
                name: appName,
                application: [
                        ref: [
                                groupId: groupId,
                                artifactId : appName,
                                version: appVersion,
                                packaging: "jar"
                        ],
                        desiredState: "STARTED",
                        configuration: [
                                "mule.agent.application.properties.service": [
                                        applicationName: appName
                                ]
                        ],
                        "vCores": workerSpecRequest.replicaSize.vCoresSize
                ],
                target: [
                        targetId: targetId,
                        provider: "MC",
                        deploymentSettings: [
                                runtimeVersion: workerSpecRequest.muleVersion,
                                lastMileSecurity: workerSpecRequest.lastMileSecurity,
                                persistentObjectStore: workerSpecRequest.persistentObjectStore,
                                clustered: workerSpecRequest.clustered,
                                updateStrategy: workerSpecRequest.updateStrategy,
                                enforceDeployingReplicasAcrossNodes: workerSpecRequest.replicasAcrossNodes,
                                http: [
                                    inbound: {}
                                ],
                                forwardSslSession: workerSpecRequest.forwardSslSession,
                                disableAmLogForwarding: workerSpecRequest.forwardSslSession,
                                generateDefaultPublicUrl: workerSpecRequest.publicURL
                        ],
                        replicas: workerSpecRequest.workerCount
                ]
        ] as Map<String, String>
        result
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(cloudhubAppInfo)
    }

    void setTargetId(String targetId) {
        this.targetId = targetId
    }
}
