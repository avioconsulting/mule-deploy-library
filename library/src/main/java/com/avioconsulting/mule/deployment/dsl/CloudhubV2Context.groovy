package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.subdeployers.BaseDeployer

class CloudhubV2Context extends BaseDeployer {

    CloudhubV2Context(int retryIntervalInMs, int maxTries, ILogger logger, HttpClientWrapper clientWrapper, EnvironmentLocator environmentLocator, DryRunMode dryRunMode) {
        super(retryIntervalInMs, maxTries, logger, clientWrapper, environmentLocator, dryRunMode)
    }

    CloudhubV2DeploymentRequest createDeploymentRequest() {
        validateContext()
        new CloudhubV2DeploymentRequest(this.environment,
                                      workerSpecs.createRequest(),
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
