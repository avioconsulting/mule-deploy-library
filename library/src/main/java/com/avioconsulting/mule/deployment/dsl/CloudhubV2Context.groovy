package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest

class CloudhubV2Context extends RuntimeFabricContext {

    CloudhubV2DeploymentRequest createDeploymentRequest() {
        validateContext()
        new CloudhubV2DeploymentRequest(this.environment,
                environmentProperty,
                workerSpecs.createRequest(),
                cryptoKey,
                cryptoKeyProperty,
                autoDiscovery.clientId,
                autoDiscovery.clientSecret,
                applicationName.createApplicationName(),
                appVersion,
                businessGroupId,
                appProperties,
                appSecureProperties,
                otherCloudHubProperties)
    }

}
