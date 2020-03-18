package com.avioconsulting.mule.deployment.api.models.policies

abstract class BaseClientEnforcementPolicy extends Policy {
    BaseClientEnforcementPolicy(String credentialsOriginHasHttpBasicAuthenticationHeader,
                                String clientIdExpression,
                                String clientSecretExpression,
                                List<PolicyPathApplication> policyPathApplications,
                                String version) {
        // Mulesoft groupId
        super('68ef9520-24e9-4cf2-b2f5-620025690913',
              'client-id-enforcement',
              version ?: '1.2.1',
              getConfig(credentialsOriginHasHttpBasicAuthenticationHeader,
                        clientIdExpression ?: "#[attributes.headers['client_id']]",
                        clientSecretExpression),
              policyPathApplications)
    }

    private static Map<String, String> getConfig(String credentialsOriginHasHttpBasicAuthenticationHeader,
                                                 String clientIdExpression,
                                                 String clientSecretExpression) {
        def result = [
                credentialsOriginHasHttpBasicAuthenticationHeader: credentialsOriginHasHttpBasicAuthenticationHeader,
                clientIdExpression                               : clientIdExpression
        ]
        if (clientSecretExpression) {
            result['clientSecretExpression'] = clientSecretExpression
        }
        return result
    }
}
