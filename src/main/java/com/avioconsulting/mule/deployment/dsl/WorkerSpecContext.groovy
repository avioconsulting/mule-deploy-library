package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest

class WorkerSpecContext {
    private String muleVersion

    CloudhubWorkerSpecRequest getRequest() {
        new CloudhubWorkerSpecRequest(this.muleVersion)
    }

    def muleVersion(String muleVersion) {
        this.muleVersion = muleVersion
    }
}
