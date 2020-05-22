package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.Policy

class PolicyListContext {
    private List<Policy> policies = []

    List<Policy> createPolicyList() {
        policies
    }

    def policy(Closure closure) {
        def policyContext = new PolicyContext(null)
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

    def mulesoftPolicy(Closure closure) {
        def policyContext = new PolicyContext(Policy.getMulesoftGroupId())
        closure.delegate = policyContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        policies << policyContext.createPolicyModel()
    }

    def jwtPolicy(Closure closure) {
        def policyContext = new JwtPolicyBasicContext()
        closure.delegate = policyContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        policies << policyContext.createPolicyModel()
    }

    def azureAdJwtPolicy(Closure closure) {
        def policyContext = new AzureAdJwtPolicyBasicContext()
        closure.delegate = policyContext
        closure.resolveStrategy = Closure.DELEGATE_FIRST
        closure.call()
        policies << policyContext.createPolicyModel()
    }
}
