package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.ClientEnforcementPolicyBasicAuth
import com.avioconsulting.mule.deployment.api.models.policies.JwtPolicy
import com.avioconsulting.mule.deployment.dsl.BaseContext

class JwtPolicyBasicContext extends BaseContext {
    String version, jwksUrl, expectedAudience
    private PathsContext paths = new PathsContext()
    private boolean pathsCalled = false

    JwtPolicy createPolicyModel() {
        def pathListing = paths.createModel()
        if (pathsCalled && !pathListing.any()) {
            throw new Exception("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one.")
        }
        def errors = findErrors('policies.policy.jwtPolicy')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your policy spec is not complete. The following errors exist:\n${errorList}")
        }
        new JwtPolicy(this.jwksUrl,
                      this.expectedAudience)
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
        ['version']
    }
}
