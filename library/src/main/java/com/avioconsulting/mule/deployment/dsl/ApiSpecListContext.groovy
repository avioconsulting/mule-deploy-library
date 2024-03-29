package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecificationList
import com.avioconsulting.mule.deployment.api.models.deployment.AppDeploymentRequest

class ApiSpecListContext {
    private List<ApiSpecContext> apiSpecContexts = []

    ApiSpecificationList createApiSpecList(AppDeploymentRequest request) {
        def specs = apiSpecContexts.collect { ctx ->
            ctx.createRequest(request)
        }
        new ApiSpecificationList(specs)
    }

    void apiSpecification(Closure closure) {
        def apiContext = new ApiSpecContext()
        closure.delegate = apiContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        apiSpecContexts << apiContext
    }
}
