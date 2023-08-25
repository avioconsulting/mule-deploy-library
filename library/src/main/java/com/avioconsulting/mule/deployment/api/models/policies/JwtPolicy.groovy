package com.avioconsulting.mule.deployment.api.models.policies

class JwtPolicy extends MulesoftPolicy {
    /**
     * Instantiate a JWT policy
     * @param jwksUrl Most endpoints use this to get keys
     * @param expectedAudience OIDC says you should always check this
     * @param expectedIssuer OIDC says you should always check this
     * @param policyPathApplications
     * @param customClaimValidations optional
     * @param clientIdExpression optional, #[vars.claimSet.client_id] by default
     * @param skipClientIdEnforcement optional, false by default
     * @param jwksCachingTtlInMinutes optional
     * @param version optional, uses 1.1.2 by default
     */
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
              version ?: '1.3.1',
              getConfig(jwksUrl,
                        expectedAudience,
                        expectedIssuer,
                        skipClientIdEnforcement,
                        clientIdExpression ?: '#[vars.claimSet.client_id]',
                        customClaimValidations,
                        // 24 hours recommended by Microsoft at https://docs.microsoft.com/en-us/azure/active-directory/develop/active-directory-signing-key-rollover
                        // https://developer.okta.com/docs/concepts/key-rotation/ - Okta rotates 4 times a year so this is probably reasonable for that system too
                        jwksCachingTtlInMinutes ?: 1440),
              policyPathApplications)
    }

    private static Map<String, Object> getConfig(String jwksUrl,
                                                 String expectedAudience,
                                                 String expectedIssuer,
                                                 boolean skipClientIdEnforcement,
                                                 String clientIdExpression,
                                                 Map<String, String> customClaimValidations,
                                                 int jwksCachingTtlInMinutes) {
        customClaimValidations = [
                // Mulesoft does not have this as a default claim to validate but it should
                iss: expectedIssuer
        ] + (customClaimValidations ?: [:])
        return [
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
                validateCustomClaim   : true,
                mandatoryCustomClaims : customClaimValidations.collect { k, v ->
                    [
                            key  : k,
                            value: v
                    ]
                }
        ]
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
