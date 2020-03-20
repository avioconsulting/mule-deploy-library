package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.Policy

class PolicyListContext {
    private List<Policy> policies = []

    List<Policy> createPolicyList() {
        policies
    }

    def policy(Closure closure) {
        def policyContext = new PolicyContext()
        closure.delegate = policyContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        policies << policyContext.createPolicyModel()
    }

    def clientEnforcementPolicyBasic(Closure closure = {}) {
        def policyContext = new ClientEnforcementPolicyBasicContext()
        closure.delegate = policyContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        policies << policyContext.createPolicyModel()
    }
}
