package com.avioconsulting.mule.deployment.dsl.policies

import com.avioconsulting.mule.deployment.api.models.policies.AzureAdJwtPolicy
import com.avioconsulting.mule.deployment.dsl.BaseContext

class AzureAdJwtPolicyBasicContext extends BaseContext {
    String version, azureAdTenantId, expectedAudience
    private PathsContext paths = new PathsContext()
    private boolean pathsCalled = false
    private Map<String, String> customClaims = [:]
    private List<String> rolesRequired = []
    private boolean skipClientIdEnforcement = false
    Integer jwksCachingTtlInMinutes

    AzureAdJwtPolicy createPolicyModel() {
        def pathListing = paths.createModel()
        if (pathsCalled && !pathListing.any()) {
            throw new Exception("You specified 'paths' but did not supply any 'path' declarations inside it. Either remove the paths declaration (policy applies to all resources) or declare one.")
        }
        def errors = findErrors('policies.policy.azureAdJwtPolicy')
        if (errors.any()) {
            def errorList = errors.join('\n')
            throw new Exception("Your policy spec is not complete. The following errors exist:\n${errorList}")
        }
        return new AzureAdJwtPolicy(this.azureAdTenantId,
                                    this.expectedAudience,
                                    this.rolesRequired,
                                    this.customClaims,
                                    pathListing,
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
        return null
    }

    def requireRole(String role) {
        rolesRequired << role
        return null
    }

    @Override
    List<String> findOptionalProperties() {
        ['version', 'jwksCachingTtlInMinutes']
    }
}
