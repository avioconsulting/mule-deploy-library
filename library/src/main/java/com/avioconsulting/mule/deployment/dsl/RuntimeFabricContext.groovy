package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest

class RuntimeFabricContext extends BaseContext {
    String environment
    String environmentProperty
    String appVersion
    String cryptoKey
    String cryptoKeyProperty
    String businessGroupId
    WorkerSpecV2Context workerSpecs = new WorkerSpecV2Context()
    AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()
    ApplicationNameContext applicationName = new ApplicationNameContext()
    Map<String, String> appProperties = [:]
    Map<String, String> appSecureProperties = [:]
    Map<String, String> otherCloudHubProperties = [:]

    RuntimeFabricDeploymentRequest createDeploymentRequest() {
        validateContext()
        new RuntimeFabricDeploymentRequest(environment,
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

    /**
     * Besides of validating attributes of the class itself, validates attributes from workSpecs and autoDiscovery objects
     */
    protected def validateContext() {
        validateBaseContext(["workerSpecs": workerSpecs, "autoDiscovery": autoDiscovery])
    }

    @Override
    List<String> findOptionalProperties() {
        ['applicationName', 'environmentProperty', 'cryptoKeyProperty']
    }
}
