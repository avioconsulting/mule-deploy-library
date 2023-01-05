package com.avioconsulting.mule.deployment.api.models

import groovy.json.JsonOutput
import groovy.transform.ToString

@ToString
class CloudhubV2DeploymentRequest extends RuntimeFabricDeploymentRequest {

    /**
     * Construct a "standard" request. See properties for parameter info.
     */
    CloudhubV2DeploymentRequest(String environment,
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
        super(environment, workerSpecRequest, file, cryptoKey, anypointClientId, anypointClientSecret, cloudHubAppPrefix, appName, appVersion, groupId, appProperties, otherCloudHubProperties)
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
                        ],
                        "vCores": workerSpecRequest.replicaSize.vCoresSize
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

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(this.cloudhubAppInfo)
    }

}
