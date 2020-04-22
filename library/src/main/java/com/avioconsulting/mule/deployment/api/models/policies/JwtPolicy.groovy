package com.avioconsulting.mule.deployment.api.models.policies

class JwtPolicy extends MulesoftPolicy {
    JwtPolicy(String jwksUrl,
              String expectedAudience,
              List<PolicyPathApplication> policyPathApplications = null,
              Map<String, String> customClaimValidations = [:],
              String clientIdExpression = null,
              boolean skipClientIdEnforcement = false,
              Integer jwksCachingTtlInMinutes = null,
              String version = null) {
        super('jwt-validation',
              version ?: '1.1.2',
              getConfig(jwksUrl,
                        expectedAudience,
                        skipClientIdEnforcement,
                        clientIdExpression ?: '#[vars.claimSet.client_id]',
                        customClaimValidations,
                        jwksCachingTtlInMinutes ?: 60),
              policyPathApplications)
    }

    private static Map<String, Object> getConfig(String jwksUrl,
                                                 String expectedAudience,
                                                 boolean skipClientIdEnforcement,
                                                 String clientIdExpression,
                                                 Map<String, String> customClaimValidations,
                                                 int jwksCachingTtlInMinutes) {
        def map = [
                jwtKeyOrigin          : 'jwks',
                jwksUrl               : jwksUrl,
                skipClientIdValidation: skipClientIdEnforcement,
                clientIdExpression    : clientIdExpression,
                validateAudClaim      : true,
                mandatoryAudClaim     : true,
                supportedAudiences    : expectedAudience,
                mandatoryExpClaim     : true,
                mandatoryNbfClaim     : true,
                jwksServiceTimeToLive : jwksCachingTtlInMinutes
        ]
        if (customClaimValidations.any()) {
            map['validateCustomClaim'] = true
            map['mandatoryCustomClaims'] = customClaimValidations.collect { k, v ->
                [
                        key  : k,
                        value: v
                ]
            }
        }
        return map
    }
}
