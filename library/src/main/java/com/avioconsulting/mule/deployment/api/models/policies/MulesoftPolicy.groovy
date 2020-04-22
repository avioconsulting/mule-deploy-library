package com.avioconsulting.mule.deployment.api.models.policies

class MulesoftPolicy extends Policy {
    MulesoftPolicy(String assetId,
                   String version,
                   Map<String, Object> policyConfiguration,
                   List<PolicyPathApplication> policyPathApplications = null) {
        super(assetId,
              version,
              policyConfiguration,
              getMulesoftGroupId(),
              policyPathApplications)
    }
}
