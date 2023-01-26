package com.avioconsulting.mule.deployment.api.models

import com.avioconsulting.mule.deployment.internal.models.CloudhubAppProperties
import groovy.json.JsonOutput
import groovy.transform.ToString

@ToString
class RuntimeFabricDeploymentRequest extends ExchangeAppDeploymentRequest {
    /**
     * CloudHub specs
     */
    final WorkerSpecRequest workerSpecRequest
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
     * The Business group ID of the deployment
     * The Business group ID is a mandatory parameter when you have access only to a specific Business group but not to the parent organization
     */
    final String groupId

    /**
     * The CloudHub 2.0 target name to deploy the app to.
     * Specify either a shared space or a private space available in your Deployment Target values in CloudHub 2.0
     */
    final String target

    /**
     * As per the documentation, set to MC, for Runtime Fabric.
     */
    final String provider = "MC"

    /**
     * Id of target based on the target name
     */
    String targetId

    protected CloudhubAppProperties cloudhubAppProperties

    /**
     * Construct a "standard" request. See properties for parameter info.
     */
    RuntimeFabricDeploymentRequest(String environment,
                                   WorkerSpecRequest workerSpecRequest,
                                   File file,
                                   String cryptoKey,
                                   String anypointClientId,
                                   String anypointClientSecret,
                                   String cloudHubAppPrefix,
                                   String appName = null,
                                   String appVersion = null,
                                   String groupId = null,
                                   Map<String, String> appProperties = [:],
                                   Map<String, String> otherCloudHubProperties = [:]) {
        super(appName, appVersion, environment)
        this.groupId = groupId ?: parsedPomProperties.groupId
        this.target = workerSpecRequest.target
        this.workerSpecRequest = workerSpecRequest
        if (!workerSpecRequest.muleVersion) {
            def propertyToUse = mule4Request ? 'app.runtime' : 'mule.version'
            def rawVersion = parsedPomProperties.props[propertyToUse]
            // Studio will modify some projects with 4.2.2-hf2. The -hf2 part is meaningless to CloudHub
            // because it's done in the form of update IDs. It's better to just remove it
            rawVersion = rawVersion.split('-')[0]
            this.workerSpecRequest.muleVersion = rawVersion
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

    Map<String, String> getCloudhubAppInfo() {
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
                        ]
                ],
                target: [
                        targetId: targetId,
                        provider: provider,
                        deploymentSettings: [
                                runtimeVersion: workerSpecRequest.muleVersion,
                                lastMileSecurity: workerSpecRequest.lastMileSecurity,
                                persistentObjectStore: workerSpecRequest.persistentObjectStore,
                                clustered: workerSpecRequest.clustered,
                                updateStrategy: workerSpecRequest.updateStrategy,
                                enforceDeployingReplicasAcrossNodes: workerSpecRequest.replicasAcrossNodes,
                                forwardSslSession: workerSpecRequest.forwardSslSession,
                                disableAmLogForwarding: workerSpecRequest.disableAmLogForwarding,
                                generateDefaultPublicUrl: workerSpecRequest.publicURL
                        ],
                        replicas: workerSpecRequest.workerCount
                ]
        ] as Map<String, String>
        result
    }

    Map<String, String> getCloudhubAppInfoWithResources() {
        def result = cloudhubAppInfo
        def resources = [
                resources: [
                    cpu: [ reserved: workerSpecRequest.cpuReserved ],
                    memory: [ reserved: workerSpecRequest.memoryReserved ]
                ]
        ]
        result.target.deploymentSettings << resources
        result
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(cloudhubAppInfoWithResources)
    }

    void setTargetId(String targetId) {
        this.targetId = targetId
    }
}
