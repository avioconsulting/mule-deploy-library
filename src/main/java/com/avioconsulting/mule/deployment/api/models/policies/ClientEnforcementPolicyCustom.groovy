package com.avioconsulting.mule.deployment.api.models.policies

/**
 * Equivalent to "Custom Expression" in the GUI
 */
class ClientEnforcementPolicyCustom extends BaseClientEnforcementPolicy {
    /**
     * Build a new policy
     * @param policyPathApplications List of policy path apps. By default, will apply to all paths
     * @param clientIdExpression
     * @param clientSecretExpression
     * @param version will use BaseClientEnforcementPolicy version by default
     */
    ClientEnforcementPolicyCustom(List<PolicyPathApplication> policyPathApplications = null,
                                  String clientIdExpression = "#[attributes.headers['client_id']]",
                                  String clientSecretExpression = "#[attributes.headers['client_secret']]",
                                  String version = null) {
        super('customExpression',
              clientIdExpression,
              clientSecretExpression,
              policyPathApplications,
              version)
    }
}
