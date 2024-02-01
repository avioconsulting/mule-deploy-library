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
                                WorkerSpecRequest workerSpecRequest,
                                String cryptoKey,
                                String anypointClientId,
                                String anypointClientSecret,
                                ApplicationName applicationName,
                                String appVersion,
                                String groupId,
                                Map<String, String> appProperties = [:],
                                Map<String, String> appSecureProperties = [:],
                                Map<String, String> otherCloudHubProperties = [:]) {
        super(environment, workerSpecRequest, cryptoKey, anypointClientId, anypointClientSecret, applicationName, appVersion, groupId, appProperties, appSecureProperties, otherCloudHubProperties)
    }

    Map<String, String> getCloudhubAppInfo() {
        def result = super.getCloudhubBaseAppInfo()
        def vCores = ["vCores": workerSpecRequest.replicaSize.vCoresSize]
        result.application << vCores
        result
    }

    String getCloudhubAppInfoAsJson() {
        JsonOutput.toJson(this.cloudhubAppInfo)
    }

}
