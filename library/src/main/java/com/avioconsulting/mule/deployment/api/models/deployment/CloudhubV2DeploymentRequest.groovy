package com.avioconsulting.mule.deployment.api.models.deployment

import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import groovy.json.JsonOutput
import groovy.transform.ToString

@ToString
class CloudhubV2DeploymentRequest extends RuntimeFabricDeploymentRequest {

    /**
     * Construct a "standard" request. See properties for parameter info.
     */
    CloudhubV2DeploymentRequest(String environment,
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
        super(environment, environmentProperty, workerSpecRequest, cryptoKey, cryptoKeyProperty, anypointClientId, anypointClientSecret, applicationName, appVersion, groupId, appProperties, appSecureProperties, otherCloudHubProperties)
    }

    Map<String, String> getCloudhubAppInfo() {
        def result = super.getCloudhubBaseAppInfo()
        result.target.deploymentSettings.enforceDeployingReplicasAcrossNodes = true
        result.target.deploymentSettings.persistentObjectStore = false
        def vCores = ["vCores": workerSpecRequest.replicaSize.vCoresSize]
        result.application << vCores
        result
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(this.cloudhubAppInfo)
    }

}
