package com.avioconsulting.mule.deployment.api.models.policies

class ClientEnforcementPolicyBasicAuth extends BaseClientEnforcementPolicy {
    /**
     * Build a new client enforcement policy using "basic auth"
     * @param policyPathApplications List of policy path apps. By default, will apply to all paths
     * @param version will use BaseClientEnforcementPolicy version by default
     */
    ClientEnforcementPolicyBasicAuth(List<PolicyPathApplication> policyPathApplications = null,
                                     String version = null) {
        super('httpBasicAuthenticationHeader',
              null,
              null,
              policyPathApplications,
              version)
    }
}
