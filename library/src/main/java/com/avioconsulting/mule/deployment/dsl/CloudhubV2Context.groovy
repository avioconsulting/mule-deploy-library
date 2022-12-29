package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubV2DeploymentRequest

class CloudhubV2Context extends BaseContext {
    String environment, applicationName, appVersion, file, cryptoKey, cloudHubAppPrefix, businessGroupId
    // make API visualizer, etc. more easy by default
    private WorkerV2SpecContext workerSpecs = new WorkerV2SpecContext()
    private AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()
    Map<String, String> appProperties = [:]
    Map<String, String> otherCloudHubProperties = [:]

    CloudhubV2DeploymentRequest createDeploymentRequest() {
        def errors = findErrors()
        def specs = workerSpecs
        errors += specs.findErrors('workerSpecs')
        def autoDiscovery = this.autoDiscovery
        errors += autoDiscovery.findErrors('autoDiscovery')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
        new CloudhubV2DeploymentRequest(this.environment,
                                      specs.createRequest(),
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

    @Override
    List<String> findOptionalProperties() {
        ['appVersion', 'applicationName', 'businessGroupId']
    }
}
