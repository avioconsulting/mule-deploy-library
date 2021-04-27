package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.internal.models.RamlFile

class ApiSpecListContext {
    private List<ApiSpecContext> apiSpecContexts = []

    List<ApiSpecification> createApiSpecList(List<RamlFile> ramlFiles) {
        def specs = apiSpecContexts.collect { ctx ->
            ctx.createRequest(ramlFiles)
        }
        if (specs.size() > 1) {
            specs.each { spec ->
                if (spec.autoDiscoveryPropertyName == 'auto-discovery.api-id') {
                    throw new Exception("If you have multiple API specs, you must specify `autoDiscoveryPropertyName` for all of them!")
                }
            }
        }
        specs
    }

    void apiSpecification(Closure closure) {
        def apiContext = new ApiSpecContext()
        closure.delegate = apiContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        apiSpecContexts << apiContext
    }
}
