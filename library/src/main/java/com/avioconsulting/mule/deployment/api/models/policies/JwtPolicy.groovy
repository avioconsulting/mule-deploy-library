package com.avioconsulting.mule.deployment.api.models.policies

class JwtPolicy extends MulesoftPolicy {
    JwtPolicy(String jwksUrl,
              String expectedAudience,
              List<PolicyPathApplication> policyPathApplications = null,
              Map<String, String> customClaimValidations = [:],
              String clientIdExpression = '#[vars.claimSet.client_id]',
              boolean skipClientIdEnforcement = false,
              int jwksCachingTtlInMinutes = 60,
              String version = '1.1.2') {
        super('jwt-validation',
              version,
              getConfig(jwksUrl,
                        expectedAudience,
                        skipClientIdEnforcement,
                        clientIdExpression,
                        customClaimValidations),
              policyPathApplications)
    }

    private static Map getConfig(String jwksUrl,
                                 String expectedAudience,
                                 boolean skipClientIdEnforcement,
                                 String clientIdExpression,
                                 Map<String, String> customClaimValidations) {
        def map = [
                jwtKeyOrigin          : 'jwks',
                jwksUrl               : jwksUrl,
                skipClientIdValidation: skipClientIdEnforcement,
                clientIdExpression    : clientIdExpression,
                validateAudClaim      : true,
                mandatoryAudClaim     : true,
                supportedAudiences    : expectedAudience,
                mandatoryExpClaim     : true,
                mandatoryNbfClaim     : true
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
