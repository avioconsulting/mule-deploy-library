package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest

class CloudhubContext {
    String environment
    String applicationName
    String appVersion
    String file
    String cryptoKey
    String cloudHubAppPrefix
    private WorkerSpecContext workerSpecs = new WorkerSpecContext()
    private AutodiscoveryContext autodiscovery = new AutodiscoveryContext()
    private List<String> fieldsThatHaveBeenSet = []

    CloudhubDeploymentRequest createDeploymentRequest() {
        def errors = this.getProperties().findAll { k, v ->
            k != 'class' && v == null
        }.collect { k, v ->
            k
        }.sort()
        if (errors.any()) {
            def errorList = errors.collect { error ->
                "- ${error} missing"
            }.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
        new CloudhubDeploymentRequest(this.environment,
                                      this.applicationName,
                                      this.appVersion,
                                      workerSpecs.request,
                                      new File(this.file),
                                      this.cryptoKey,
                                      this.autodiscovery.clientId,
                                      this.autodiscovery.clientSecret,
                                      this.cloudHubAppPrefix)
    }

    def methodMissing(String name, def args) {
        if (fieldsThatHaveBeenSet.contains(name)) {
            throw new Exception("Field '${name}' has already been set!")
        }
        fieldsThatHaveBeenSet << name
        def argument = args[0]
        if (argument instanceof Closure) {
            argument.delegate = this[name]
            argument.call()
        } else {
            this[name] = argument
        }
    }
}
