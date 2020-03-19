package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest

class CloudhubContext {
    private String environmentName

    CloudhubDeploymentRequest getDeploymentRequest() {
        new CloudhubDeploymentRequest(this.environmentName,
                                      'foo',
                                      new CloudhubWorkerSpecRequest('4.2.2'),
                                      null,
                                      null,
                                      null,
                                      null,
                                      null)
    }

    def environment(String environmentName) {
        this.environmentName = environmentName
    }

    def methodMissing(String name, def args) {
        println "got call ${name}"
    }
}
