package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubV2DeploymentRequest

class CloudhubV2Context extends RuntimeFabricContext {

    CloudhubV2DeploymentRequest createDeploymentRequest() {
        validateContext()
        new CloudhubV2DeploymentRequest(this.environment,
                                      workerSpecs.createRequest(),
                                      new File(this.file),
                                      this.cryptoKey,
                                      autoDiscovery.clientId,
                                      autoDiscovery.clientSecret,
                                      this.cloudHubAppPrefix,
                                      this.applicationName,
                                      this.appVersion,
                                      this.businessGroupId,
                                      this.appProperties,
                                      this.otherCloudHubProperties)
    }

}
