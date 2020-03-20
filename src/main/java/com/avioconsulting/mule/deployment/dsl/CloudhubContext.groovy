package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest

class CloudhubContext extends BaseContext {
    String environment
    String applicationName
    String appVersion
    String file
    String cryptoKey
    String cloudHubAppPrefix
    private WorkerSpecContext workerSpecs = new WorkerSpecContext()
    private AutodiscoveryContext autoDiscovery = new AutodiscoveryContext()

    CloudhubDeploymentRequest createDeploymentRequest() {
        def errors = findErrors()
        def autoDiscovery = this.autoDiscovery
        errors += autoDiscovery.findErrors('autoDiscovery')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
        new CloudhubDeploymentRequest(this.environment,
                                      this.applicationName,
                                      this.appVersion,
                                      workerSpecs.request,
                                      new File(this.file),
                                      this.cryptoKey,
                                      autoDiscovery.clientId,
                                      autoDiscovery.clientSecret,
                                      this.cloudHubAppPrefix)
    }
}
