package com.avioconsulting.mule.deployment.api.models.deployment

import com.avioconsulting.mule.deployment.api.models.WorkerSpecRequest
import groovy.json.JsonOutput
import groovy.transform.ToString

@ToString
class CloudhubV2DeploymentRequest extends RuntimeFabricDeploymentRequest {

    final Boolean enableLinkedUpgrade
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
                                Map<String, String> otherCloudHubProperties = [:],
                                Boolean enableLinkedUpgrade = false) {
        super(environment, environmentProperty, workerSpecRequest, cryptoKey, cryptoKeyProperty, anypointClientId, anypointClientSecret, applicationName, appVersion, groupId, appProperties, appSecureProperties, otherCloudHubProperties)
        this.enableLinkedUpgrade = enableLinkedUpgrade
    }

    Map<String, String> getCloudhubAppInfo() {
        def result = super.getCloudhubBaseAppInfo()
        result.target.deploymentSettings.enforceDeployingReplicasAcrossNodes = true
        result.target.deploymentSettings.persistentObjectStore = false
        def vCores = ["vCores": workerSpecRequest.replicaSize.vCoresSize]
        result.application << vCores
                
        // Add upgradeData when enableLinkedUpgrade is true for CloudHub v1 to v2 migration
        if (enableLinkedUpgrade) {
            result.upgradeData = [
                linkedCloudhubDeployment: [
                    applicationName: normalizedAppName
                ]
            ]
        }
        
        result
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(this.cloudhubAppInfo)
    }

}
