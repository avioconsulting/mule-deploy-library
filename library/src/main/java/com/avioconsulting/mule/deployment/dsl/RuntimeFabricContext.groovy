package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.RuntimeFabricDeploymentRequest

class RuntimeFabricContext extends BaseContext {
    String environment, applicationName, appVersion, file, cryptoKey, cloudHubAppPrefix, businessGroupId
    // make API visualizer, etc. more easy by default
    WorkerSpecV2Context workerSpecs = new WorkerSpecV2Context()
    AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()
    Map<String, String> appProperties = [:]
    Map<String, String> otherCloudHubProperties = [:]

    RuntimeFabricDeploymentRequest createDeploymentRequest() {
        validateContext()
        new RuntimeFabricDeploymentRequest(this.environment,
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

    protected List validateContext() {
        def errors = findErrors()
        def specs = workerSpecs
        errors += specs.findErrors('workerSpecs')
        def autoDiscovery = this.autoDiscovery
        errors += autoDiscovery.findErrors('autoDiscovery')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
    }

    @Override
    List<String> findOptionalProperties() {
        ['appVersion', 'applicationName', 'businessGroupId']
    }
}
