package com.avioconsulting.mule.deployment.api.models.policies

class ClientEnforcementPolicyCustom extends BaseClientEnforcementPolicy {
    ClientEnforcementPolicyCustom(List<PolicyPathApplication> policyPathApplications,
                                  String clientIdExpression = "#[attributes.headers['client_id']]",
                                  String clientSecretExpression = "#[attributes.headers['client_secret']]",
                                  String version = DEFAULT_VERSION) {
        super('customExpression',
              clientIdExpression,
              clientSecretExpression,
              policyPathApplications,
              version)
    }
}
