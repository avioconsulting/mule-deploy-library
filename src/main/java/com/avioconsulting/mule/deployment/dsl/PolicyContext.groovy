package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication

class PathsContext {
    private List<PolicyPathApplication> models = []

    List<PolicyPathApplication> createModel() {
        models
    }

    def path(Closure closure) {
        def context = new PathContext()
        closure.delegate = context
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        models << context.createModel()
    }
}

class PathContext {
    private String regex
    private List<HttpMethod> methods = []

    PolicyPathApplication createModel() {
        new PolicyPathApplication(methods,
                                  regex)
    }

    def regex(String regex) {
        this.regex = regex
    }

    def method(HttpMethod httpMethod) {
        methods << httpMethod
    }

    def propertyMissing(String name) {
        HttpMethod.values().find { method ->
            method.name() == name
        }
    }
}

class PolicyContext extends BaseContext {
    String groupId, assetId, version
    Map<String, String> config
    private PathsContext paths = new PathsContext()

    Policy createPolicyModel() {
        def pathsList = paths.createModel()
        new Policy(this.groupId ?: Policy.mulesoftGroupId,
                   this.assetId,
                   this.version,
                   this.config,
                   pathsList.any() ? pathsList : null)
    }

    @Override
    List<String> findOptionalProperties() {
        return null
    }
}
