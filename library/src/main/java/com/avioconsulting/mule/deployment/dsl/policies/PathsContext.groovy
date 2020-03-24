package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication

class PathsContext {
    private List<PolicyPathApplication> models = null

    List<PolicyPathApplication> createModel() {
        models
    }

    def path(Closure closure) {
        def context = new PathContext()
        closure.delegate = context
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        if (!models) {
            models = []
        }
        models << context.createModel()
    }
}
