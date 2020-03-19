package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest

class CloudhubContext {
    private String environmentName
    private String appName
    private String appVersion
    private WorkerSpecContext workerSpecContext = new WorkerSpecContext()
    private AutodiscoveryContext autodiscoveryContext = new AutodiscoveryContext()
    private File file
    private String key
    private String prefix

    CloudhubDeploymentRequest getDeploymentRequest() {
        def errors = []
        if (!this.environmentName) {
            errors << 'environmentName'
        }
        if (errors.any()) {
            def errorList = errors.collect { error ->
                "- ${error}"
            }.join('\n')
            throw new Exception("Your deployment request is not complete. The following errors exist:\n${errorList}")
        }
        new CloudhubDeploymentRequest(this.environmentName,
                                      this.appName,
                                      this.appVersion,
                                      workerSpecContext.request,
                                      this.file,
                                      this.key,
                                      this.autodiscoveryContext.clientId,
                                      this.autodiscoveryContext.clientSecret,
                                      this.prefix)
    }

    def environment(String environmentName) {
        this.environmentName = environmentName
    }

    def applicationName(String appName) {
        this.appName = appName
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

    def cryptoKey(String key) {
        this.key = key
    }

    def cloudHubAppPrefix(String prefix) {
        this.prefix = prefix
    }
}
