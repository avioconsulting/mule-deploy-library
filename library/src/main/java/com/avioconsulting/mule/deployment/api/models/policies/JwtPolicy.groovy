package com.avioconsulting.mule.deployment.api.models.policies

class JwtPolicy extends MulesoftPolicy {
    JwtPolicy(String jwksUrl,
              String expectedAudience,
              String expectedIssuer,
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
                        expectedIssuer,
                        skipClientIdEnforcement,
                        clientIdExpression ?: '#[vars.claimSet.client_id]',
                        customClaimValidations,
                        jwksCachingTtlInMinutes ?: 60),
              policyPathApplications)
    }

    private static Map<String, Object> getConfig(String jwksUrl,
                                                 String expectedAudience,
                                                 String expectedIssuer,
                                                 boolean skipClientIdEnforcement,
                                                 String clientIdExpression,
                                                 Map<String, String> customClaimValidations,
                                                 int jwksCachingTtlInMinutes) {
        def map = [
                jwtOrigin             : 'httpBearerAuthenticationHeader',
                jwtExpression         : "#[attributes.headers['jwt']]",
                signingMethod         : 'rsa',
                // we have to include this our API Manager rejects, even though we are not using text keys
                textKey               : 'your-(256|384|512)-bit-secret',
                signingKeyLength      : 256,
                jwtKeyOrigin          : 'jwks',
                jwksUrl               : jwksUrl,
                jwksServiceTimeToLive : jwksCachingTtlInMinutes,
                skipClientIdValidation: skipClientIdEnforcement,
                clientIdExpression    : clientIdExpression,
                validateAudClaim      : true,
                mandatoryAudClaim     : true,
                supportedAudiences    : expectedAudience,
                mandatoryExpClaim     : true,
                mandatoryNbfClaim     : true,
                validateCustomClaim   : false
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

    @Override
    Map<String, Object> getPolicyConfigForEquals() {
        // existing policies don't return this field but we have to supply it when we create
        def configCopy = new HashMap<String, Object>(this.policyConfiguration)
        assert this.policyConfiguration.jwtKeyOrigin == 'jwks'
        configCopy.remove('textKey')
        return configCopy
    }
}
