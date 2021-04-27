package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest

class ApiSpecListContext {
    private List<ApiSpecification> apiSpecs = []

    List<ApiSpecification> createApiSpecList(FileBasedAppDeploymentRequest request) {
        apiSpecs
    }

    def apiSpecification(Closure closure) {
        def apiContext = new ApiSpecContext()
        closure.delegate = apiContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        apiSpecs << apiContext.createRequest()
    }
}
