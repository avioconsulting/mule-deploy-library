package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubDeploymentRequest

class CloudhubContext extends BaseContext {
    String environment, applicationName, appVersion, file, cryptoKey, cloudHubAppPrefix
    // make API visualizer, etc. more easy by default
    boolean analyticsAgentEnabled = true
    private WorkerSpecContext workerSpecs = new WorkerSpecContext()
    private AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()
    Map<String, String> appProperties = [:]
    Map<String, String> otherCloudHubProperties = [:]

    CloudhubDeploymentRequest createDeploymentRequest() {
        validateContext()
        new CloudhubDeploymentRequest(this.environment,
                                      workerSpecs.createRequest(),
                                      new File(this.file),
                                      this.cryptoKey,
                                      autoDiscovery.clientId,
                                      autoDiscovery.clientSecret,
                                      this.cloudHubAppPrefix,
                                      this.applicationName,
                                      this.appVersion,
                                      this.appProperties,
                                      this.otherCloudHubProperties,
                                      this.analyticsAgentEnabled)
    }

    /**
     * Besides of validating attributes of the class itself, validates attributes from workSpecs and autoDiscovery objects
     */
    private def validateContext() {
        validateBaseContext(["workerSpecs": workerSpecs, "autoDiscovery": autoDiscovery])
    }

    @Override
    List<String> findOptionalProperties() {
        ['appVersion', 'applicationName', 'workerSpecs', 'analyticsAgentEnabled']
    }
}
