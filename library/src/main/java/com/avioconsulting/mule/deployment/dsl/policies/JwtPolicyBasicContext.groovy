package com.avioconsulting.mule.deployment.dsl.policies


import com.avioconsulting.mule.deployment.api.models.policies.JwtPolicy
import com.avioconsulting.mule.deployment.dsl.BaseContext

class JwtPolicyBasicContext extends BaseContext {
    String version, jwksUrl, expectedAudience, expectedIssuer, clientIdExpression
    private PathsContext paths = new PathsContext()
    private boolean pathsCalled = false
    private Map<String, String> customClaims = [:]
    private boolean skipClientIdEnforcement = false
    Integer jwksCachingTtlInMinutes

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
                      this.expectedAudience,
                      this.expectedIssuer,
                      pathListing,
                      customClaims,
                      clientIdExpression,
                      skipClientIdEnforcement,
                      jwksCachingTtlInMinutes,
                      version)
    }

    def methodMissing(String name, def args) {
        if (name == 'paths') {
            pathsCalled = true
        }
        super.methodMissing(name,
                            args)
    }

    def skipClientIdEnforcement() {
        this.skipClientIdEnforcement = true
    }

    def validateClaim(String claim,
                      String expression) {
        customClaims[claim] = expression
    }

    @Override
    List<String> findOptionalProperties() {
        ['version', 'clientIdExpression', 'jwksCachingTtlInMinutes']
    }
}
