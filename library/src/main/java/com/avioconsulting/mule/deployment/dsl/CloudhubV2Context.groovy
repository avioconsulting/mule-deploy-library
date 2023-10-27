package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest

class CloudhubV2Context extends RuntimeFabricContext {

    CloudhubV2DeploymentRequest createDeploymentRequest() {
        validateContext()
        new CloudhubV2DeploymentRequest(this.environment,
                                      this.workerSpecs.createRequest(),
                                      this.cryptoKey,
                                      autoDiscovery.clientId,
                                      autoDiscovery.clientSecret,
                                      this.applicationName.createApplicationName(),
                                      this.appVersion,
                                      this.businessGroupId,
                                      this.appProperties,
                                      this.otherCloudHubProperties)
    }

}
