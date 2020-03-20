package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.CloudhubWorkerSpecRequest

class WorkerSpecContext extends BaseContext {
    String muleVersion

    CloudhubWorkerSpecRequest createRequest() {
        new CloudhubWorkerSpecRequest(this.muleVersion)
    }
}
