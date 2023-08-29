package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest

class CloudhubContext extends BaseContext {
    String environment, applicationName, appVersion, file, cryptoKey, cloudHubAppPrefix, cloudHubAppSuffix
    // make API visualizer, etc. more easy by default
    boolean analyticsAgentEnabled = true
    private WorkerSpecContext workerSpecs = new WorkerSpecContext()
    private AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()
    Map<String, String> appProperties = [:]
    Map<String, String> otherCloudHubProperties = [:]

    CloudhubDeploymentRequest createDeploymentRequest() {
        def errors = findErrors()
        def specs = workerSpecs
        errors += specs.findErrors('workerSpecs')
        def autoDiscovery = this.autoDiscovery
        errors += autoDiscovery.findErrors('autoDiscovery')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
        new CloudhubDeploymentRequest(this.environment,
                                      specs.createRequest(),
                                      new File(this.file),
                                      this.cryptoKey,
                                      autoDiscovery.clientId,
                                      autoDiscovery.clientSecret,
                                      this.cloudHubAppPrefix,
                                      this.cloudHubAppSuffix,
                                      this.applicationName,
                                      this.appVersion,
                                      this.appProperties,
                                      this.otherCloudHubProperties,
                                      this.analyticsAgentEnabled)
    }

    @Override
    List<String> findOptionalProperties() {
        ['cloudHubAppSuffix', 'appVersion', 'applicationName', 'workerSpecs', 'analyticsAgentEnabled']
    }
}
