package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.ClientEnforcementPolicyBasicAuth
import com.avioconsulting.mule.deployment.api.models.policies.Policy
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

class PathContext {
    private String regex
    private List<HttpMethod> methods = []

    PolicyPathApplication createModel() {
        if (methods.empty) {
            throw new Exception("'path' is missing a 'method' declaration")
        }
        if (!regex) {
            throw new Exception("'path' is missing a 'regex' declaration")
        }
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

class ClientEnforcementPolicyBasicContext extends BaseContext {
    String version
    private PathsContext paths = new PathsContext()
    private boolean pathsCalled = false

    ClientEnforcementPolicyBasicAuth createPolicyModel() {
        def pathListing = paths.createModel()
        if (pathsCalled && !pathListing.any()) {
            throw new Exception("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one.")
        }
        new ClientEnforcementPolicyBasicAuth(pathListing,
                                             this.version)
    }

    def methodMissing(String name, def args) {
        if (name == 'paths') {
            pathsCalled = true
        }
        super.methodMissing(name,
                            args)
    }

    @Override
    List<String> findOptionalProperties() {
        []
    }
}

class PolicyContext extends BaseContext {
    String groupId, assetId, version
    Map<String, String> config
    private PathsContext paths = new PathsContext()
    private boolean pathsCalled = false

    Policy createPolicyModel() {
        def pathListing = paths.createModel()
        if (pathsCalled && !pathListing.any()) {
            throw new Exception("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one.")
        }
        def errors = findErrors('policies.policy')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your policy spec is not complete. The following errors exist:\n${errorList}")
        }
        new Policy(this.groupId ?: Policy.mulesoftGroupId,
                   this.assetId,
                   this.version,
                   this.config,
                   pathListing)
    }

    def methodMissing(String name, def args) {
        if (name == 'paths') {
            pathsCalled = true
        }
        super.methodMissing(name,
                            args)
    }

    @Override
    List<String> findOptionalProperties() {
        // groupId will be Mulesoft's if not supplied
        ['groupId']
    }
}
