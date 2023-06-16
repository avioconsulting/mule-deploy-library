package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest

class RuntimeFabricContext extends BaseContext {
    String environment, applicationName, appVersion, cryptoKey, cloudHubAppPrefix, businessGroupId
    // make API visualizer, etc. more easy by default
    WorkerSpecV2Context workerSpecs = new WorkerSpecV2Context()
    AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()
    Map<String, String> appProperties = [:]
    Map<String, String> otherCloudHubProperties = [:]

    RuntimeFabricDeploymentRequest createDeploymentRequest() {
        validateContext()
        new RuntimeFabricDeploymentRequest(this.environment,
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

    /**
     * Besides of validating attributes of the class itself, validates attributes from workSpecs and autoDiscovery objects
     */
    protected def validateContext() {
        validateBaseContext(["workerSpecs": workerSpecs, "autoDiscovery": autoDiscovery])
    }

    @Override
    List<String> findOptionalProperties() {
        ['cloudHubAppPrefix']
    }
}
