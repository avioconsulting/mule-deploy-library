package com.avioconsulting.mule.deployment.api.models.policies

/**
 * Equivalent to "Custom Expression" in the GUI
 */
class ClientEnforcementPolicyCustomAuth extends BaseClientEnforcementPolicy {
    /**
     * Build a new policy
     * @param policyPathApplications List of policy path apps. By default, will apply to all paths
     * @param clientIdExpression
     * @param clientSecretExpression
     * @param version will use BaseClientEnforcementPolicy version by default
     */
    ClientEnforcementPolicyCustomAuth(List<PolicyPathApplication> policyPathApplications = null,
                                      String version = null,
                                      String clientIdExpression = "#[attributes.headers['client_id']]",
                                      String clientSecretExpression = "#[attributes.headers['client_secret']]"
                                      ) {
        super('customExpression',
              clientIdExpression,
              clientSecretExpression,
              policyPathApplications,
              version)
    }
}
