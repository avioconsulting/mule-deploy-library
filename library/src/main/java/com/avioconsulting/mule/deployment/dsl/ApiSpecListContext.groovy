package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.internal.models.RamlFile

class ApiSpecListContext {
    private List<ApiSpecContext> apiSpecContexts = []

    List<ApiSpecification> createApiSpecList(List<RamlFile> ramlFiles) {
        apiSpecContexts.collect { ctx ->
            ctx.createRequest(ramlFiles)
        }
    }

    void apiSpecification(Closure closure) {
        def apiContext = new ApiSpecContext()
        closure.delegate = apiContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        apiSpecContexts << apiContext
    }
}
