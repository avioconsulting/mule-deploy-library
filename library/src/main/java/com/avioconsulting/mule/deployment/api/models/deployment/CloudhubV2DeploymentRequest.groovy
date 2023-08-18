package com.avioconsulting.mule.deployment.api.models.deployment

import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import com.avioconsulting.mule.deployment.dsl.WorkerSpecContext
import groovy.json.JsonOutput
import groovy.transform.ToString

@ToString
class CloudhubV2DeploymentRequest extends FileBasedAppDeploymentRequest {

    /**
     * The Business group ID of the deployment
     * The Business group ID is a mandatory parameter when you have access only to a specific Business group but not to the parent organization
     */
    private String groupId

    /**
     * The CloudHub 2.0 target name to deploy the app to.
     * Specify either a shared space or a private space available in your Deployment Target values in CloudHub 2.0
     */
    private final String target

    private String targetId

    private final WorkerSpecRequest workerSpecRequest

    /**
     * Construct a "standard" request. See properties for parameter info.
     */
    CloudhubV2DeploymentRequest(String environment,
                                WorkerSpecRequest workerSpecRequest,
                                File filetoDeploy,
                                String cryptoKey,
                                String anypointClientId,
                                String anypointClientSecret,
                                String cloudHubAppPrefix,
                                String appName,
                                String appVersion,
                                String groupId,
                                Map<String, String> appProperties = [:],
                                Map<String, String> otherCloudHubProperties = [:]) {
        super(filetoDeploy, appName, appVersion, environment)
        this.groupId=groupId
        this.target=workerSpecRequest.target
        this.workerSpecRequest = workerSpecRequest
    }

    Map<String, String> getCloudhubAppInfo() {
        def result = getCloudhubBaseAppInfo()
        def vCores = ["vCores": workerSpecRequest.replicaSize.vCoresSize]
        result.application << vCores
        result
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(this.cloudhubAppInfo)
    }

    void setTargetId(String targetId) {
        this.targetId = targetId
    }

    void setGroupId(String groupId) {
        this.groupId = groupId
    }

    Map<String, String> getCloudhubBaseAppInfo() {
        def props = this.autoDiscoveries

        def result = [
                // CloudHub's v2 API calls the Mule application the 'domain'
                //TODO change to normalized app name
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
                                        applicationName: appName,
                                        properties: props
                                ]
                        ]
                ],
                target: [
                        targetId: targetId,
                        //TODO check this provider
                        provider: "MC",
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

}
