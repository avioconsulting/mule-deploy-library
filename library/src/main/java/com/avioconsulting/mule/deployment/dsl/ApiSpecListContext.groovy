package com.avioconsulting.mule.deployment.dsl


import com.avioconsulting.mule.deployment.api.models.ApiSpecificationList
import com.avioconsulting.mule.deployment.internal.models.RamlFile

class ApiSpecListContext {
    private List<ApiSpecContext> apiSpecContexts = []

    ApiSpecificationList createApiSpecList(List<RamlFile> ramlFiles) {
        def specs = apiSpecContexts.collect { ctx ->
            ctx.createRequest(ramlFiles)
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
