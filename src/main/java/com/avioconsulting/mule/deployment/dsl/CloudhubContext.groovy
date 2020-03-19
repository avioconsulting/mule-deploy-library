package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest

class CloudhubContext {
    private String environmentName
    private String appName
    private String appVersion
    private WorkerSpecContext workerSpecContext = new WorkerSpecContext()

    CloudhubDeploymentRequest getDeploymentRequest() {
        new CloudhubDeploymentRequest(this.environmentName,
                                      this.appName,
                                      this.appVersion,
                                      workerSpecContext.request,
                                      null,
                                      null,
                                      null,
                                      null,
                                      null)
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

    def methodMissing(String name, def args) {
        println "got call ${name}"
    }
}
