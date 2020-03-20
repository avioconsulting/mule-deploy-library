package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest

class CloudhubContext {
    String environment
    String applicationName
    String appVersion
    File file
    String cryptoKey
    String cloudHubAppPrefix
    private WorkerSpecContext workerSpecContext = new WorkerSpecContext()
    private AutodiscoveryContext autodiscoveryContext = new AutodiscoveryContext()

    CloudhubDeploymentRequest createDeploymentRequest() {
        def errors = this.getProperties().findAll { k, v ->
            k != 'class' && v == null
        }.collect { k, v ->
            k
        }.sort()
        if (errors.any()) {
            def errorList = errors.collect { error ->
                "- ${error}"
            }.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
        new CloudhubDeploymentRequest(this.environment,
                                      this.applicationName,
                                      this.appVersion,
                                      workerSpecContext.request,
                                      this.file,
                                      this.cryptoKey,
                                      this.autodiscoveryContext.clientId,
                                      this.autodiscoveryContext.clientSecret,
                                      this.cloudHubAppPrefix)
    }

    def environment(String environment) {
        this.environment = environment
    }

    def applicationName(String applicationName) {
        this.applicationName = applicationName
    }

    def appVersion(String appVersion) {
        this.appVersion = appVersion
    }

    def workerSpecs(Closure closure) {
        closure.delegate = workerSpecContext
        closure.call()
    }

    def autodiscovery(Closure closure) {
        closure.delegate = autodiscoveryContext
        closure.call()
    }

    def file(String file) {
        this.file = new File(file)
    }

    def cryptoKey(String cryptoKey) {
        this.cryptoKey = cryptoKey
    }

    def cloudHubAppPrefix(String cloudHubAppPrefix) {
        this.cloudHubAppPrefix = cloudHubAppPrefix
    }
}
