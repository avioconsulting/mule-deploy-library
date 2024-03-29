package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.deployment.ApplicationName
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubDeploymentRequest

class CloudhubContext extends BaseContext {
    String environment, appVersion, file, cryptoKey
    // make API visualizer, etc. more easy by default
    boolean analyticsAgentEnabled = true
    private WorkerSpecContext workerSpecs = new WorkerSpecContext()
    private AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()
    Map<String, String> appProperties = [:]
    Map<String, String> otherCloudHubProperties = [:]
    private ApplicationNameContext applicationName = new ApplicationNameContext()
    CloudhubDeploymentRequest createDeploymentRequest() {
        validateContext()
        new CloudhubDeploymentRequest(this.environment,
                                      workerSpecs.createRequest(),
                                      new File(this.file),
                                      this.cryptoKey,
                                      autoDiscovery.clientId,
                                      autoDiscovery.clientSecret,
                                      this.applicationName.createApplicationName(),
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
