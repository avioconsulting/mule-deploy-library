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
                                String cryptoKey,
                                String anypointClientId,
                                String anypointClientSecret,
                                String cloudHubAppPrefix,
                                String appName,
                                String appVersion,
                                String groupId,
                                Map<String, String> appProperties = [:],
                                Map<String, String> otherCloudHubProperties = [:]) {
        super(environment, workerSpecRequest, cryptoKey, anypointClientId, anypointClientSecret, cloudHubAppPrefix, appName, appVersion, groupId, appProperties, otherCloudHubProperties)
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
