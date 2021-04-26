package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.HttpMethod
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import com.avioconsulting.mule.deployment.dsl.LowerCaseEnumWrapper

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

    def invokeMethod(String name, def args) {
        switch (name) {
            case HttpMethod.simpleName:
                return new LowerCaseEnumWrapper(HttpMethod)
            default:
                return null
        }
    }
}
