package com.avioconsulting.mule.deployment.api.models.deployment


import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import com.avioconsulting.mule.deployment.internal.models.CloudhubAppProperties
import groovy.json.JsonOutput
import groovy.transform.ToString

@ToString
class RuntimeFabricDeploymentRequest extends ExchangeAppDeploymentRequest {

    final MAX_SIZE_APPLICATION_NAME = 42
    /**
     * CloudHub specs
     */
    WorkerSpecRequest workerSpecRequest
    /**
     * Will be set in the 'crypto.key' CloudHub property unless cryptoKeyProperty is specified
     */
    String cryptoKey
    /**
     * will be set in the anypoint.platform.client_id CloudHub property
     */
    String anypointClientId
    /**
     * will be set in the anypoint.platform.client_secret CloudHub property
     */
    String anypointClientSecret
    /**
     * Mule app property overrides (the stuff in the properties tab)
     */
    Map<String, String> appProperties
    /**
     * Mule secure app property overrides (the stuff in the properties tab that will hide the value)
     */
    Map<String, String> appSecureProperties
    /**
     * CloudHub level property overrides (e.g. region type stuff)
     */
    Map<String, String> otherCloudHubProperties
    /**
     * Get only property, derived from app, environment, and prefix, this the real application name that will be used in CloudHub to ensure uniqueness.
     */
    String normalizedAppName

    /**
     * The Business group ID of the deployment
     * The Business group ID is a mandatory parameter when you have access only to a specific Business group but not to the parent organization
     */
    String groupId

    /**
     * The CloudHub 2.0 target name to deploy the app to.
     * Specify either a shared space or a private space available in your Deployment Target values in CloudHub 2.0
     */
    String target

    /**
     * As per the documentation, set to MC, for Runtime Fabric.
     */
    String provider = "MC"

    /**
     * Id of target based on the target name
     */
    String targetId

    protected CloudhubAppProperties cloudhubAppProperties

    /**
     * Construct a "standard" request. See properties for parameter info.
     */
    RuntimeFabricDeploymentRequest(String environment,
                                   String environmentProperty,
                                   WorkerSpecRequest workerSpecRequest,
                                   String cryptoKey,
                                   String cryptoKeyProperty,
                                   String anypointClientId,
                                   String anypointClientSecret,
                                   ApplicationName applicationName,
                                   String appVersion,
                                   String groupId,
                                   Map<String, String> appProperties = [:],
                                   Map<String, String> appSecureProperties = [:],
                                   Map<String, String> otherCloudHubProperties = [:]) {
        super(applicationName, appVersion, environment, environmentProperty)
        this.groupId = groupId
        this.target = workerSpecRequest.target
        this.workerSpecRequest = workerSpecRequest
        this.workerSpecRequest.muleVersion = workerSpecRequest.muleVersion
        this.cryptoKey = cryptoKey
        this.anypointClientId = anypointClientId
        this.anypointClientSecret = anypointClientSecret
        this.appProperties = appProperties
        this.appSecureProperties = appSecureProperties
        this.otherCloudHubProperties = otherCloudHubProperties

        if (!applicationName.baseAppName) {
            throw new Exception("Property applicationName.baseAppName is required for CloudHub 2.0 and RTF applications");
        }

        normalizedAppName = applicationName.getNormalizedAppName()
        this.cloudhubAppProperties = new CloudhubAppProperties(applicationName.baseAppName,
                environment.toLowerCase(),
                environmentProperty,
                cryptoKey,
                cryptoKeyProperty,
                anypointClientId,
                anypointClientSecret,
                null)
    }

    Map<String, String> getCloudhubBaseAppInfo() {
        def props = this.autoDiscoveries
        props[CloudhubAppProperties.ANYPOINT_PLATFORM_CLIENT_ID] = anypointClientId
        props[cloudhubAppProperties.environmentProperty] = environment.toLowerCase()
        props[CloudhubAppProperties.ANYPOINT_PLATFORM_VISUALIZATION_LAYER] = cloudhubAppProperties.apiVisualizerLayer

        if (appProperties) {
            props += appProperties
        }


        def secureProps = [:]
        secureProps[CloudhubAppProperties.ANYPOINT_PLATFORM_CLIENT_SECRET] = anypointClientSecret
        secureProps[cloudhubAppProperties.cryptoKeyProperty] = cryptoKey
        if (appSecureProperties) {
            secureProps += appSecureProperties
        }

        def result = [
                // CloudHub's v2 API calls the Mule application the 'domain'
                name       : normalizedAppName,
                application: [
                        ref          : [
                                groupId   : groupId,
                                artifactId: applicationName.baseAppName,
                                version   : appVersion,
                                packaging : "jar"
                        ],
                        desiredState : "STARTED",
                        configuration: [
                                "mule.agent.application.properties.service": [
                                        applicationName : applicationName.baseAppName,
                                        properties      : props,
                                        secureProperties: secureProps
                                ]
                        ],
                        integrations : [
                                services: [
                                        objectStoreV2: [
                                                enabled: workerSpecRequest.objectStoreV2
                                        ]
                                ]
                        ],
                        vCores       : workerSpecRequest.replicaSize
                ],
                target     : [
                        targetId          : targetId,
                        provider          : provider,
                        deploymentSettings: [
                                clustered                          : workerSpecRequest.clustered,
                                updateStrategy                     : workerSpecRequest.updateStrategy,
                                enforceDeployingReplicasAcrossNodes: workerSpecRequest.replicasAcrossNodes,
                                disableAmLogForwarding             : workerSpecRequest.disableAmLogForwarding,
                                generateDefaultPublicUrl           : workerSpecRequest.generateDefaultPublicUrl,
                                http                               : [
                                        inbound: [
                                                publicUrl        : workerSpecRequest.publicUrl,
                                                pathRewrite      : workerSpecRequest.pathRewrite,
                                                lastMileSecurity : workerSpecRequest.lastMileSecurity,
                                                forwardSslSession: workerSpecRequest.forwardSslSession
                                        ]
                                ],
                                jvm                                : [:],
                                outbound                           : [:],
                                runtime                            : [
                                        version       : workerSpecRequest.muleVersion,
                                        releaseChannel: workerSpecRequest.releaseChannel,
                                        java          : workerSpecRequest.javaVersion
                                ],
                                tracingEnabled                     : workerSpecRequest.tracingEnabled

                        ],
                        replicas          : workerSpecRequest.workerCount
                ]
        ] as Map<String, String>

        def appInfo = result + (this.otherCloudHubProperties ?: [:])
        appInfo

    }

    Map<String, String> getCloudhubAppInfo() {
        def result = cloudhubBaseAppInfo
        def resources = [
                resources: [
                        cpu   : [reserved: workerSpecRequest.cpuReserved],
                        memory: [reserved: workerSpecRequest.memoryReserved]
                ]
        ]
        result.target.deploymentSettings << resources
        result
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(cloudhubAppInfo)
    }

    void setTargetId(String targetId) {
        this.targetId = targetId
    }

}
