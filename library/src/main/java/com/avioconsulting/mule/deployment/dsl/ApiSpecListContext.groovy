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
            def projectNames = specs.collect { s -> s.name }
            def branchNames = specs.collect { s -> s.designCenterBranchName }
            if (projectNames.size() != projectNames.unique().size() && branchNames.size() != branchNames.unique().size()) {
                throw new Exception('You either need separate design center project names for v1 and v2 OR you need different designCenterBranchNames')
            }
            def autoDiscNames = specs.collect {s -> s.autoDiscoveryPropertyName}
            if (autoDiscNames.size() != autoDiscNames.unique().size()) {
                throw new Exception("If you have multiple API specs, you must specify a unique `autoDiscoveryPropertyName` for all of them!")
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
