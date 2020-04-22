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
              [
                      jwtKeyOrigin          : 'jwks',
                      jwksUrl               : jwksUrl,
                      skipClientIdValidation: skipClientIdEnforcement,
                      clientIdExpression    : clientIdExpression,
                      validateAudClaim      : true,
                      mandatoryAudClaim     : true,
                      supportedAudiences    : expectedAudience,
                      mandatoryExpClaim     : true,
                      mandatoryNbfClaim     : true
              ],
              policyPathApplications)
    }
}
