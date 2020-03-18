package com.avioconsulting.mule.deployment.internal.models

import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(callSuper = true)
@ToString(includeSuper = true)
class ExistingPolicy extends Policy {
    final String id

    ExistingPolicy(String assetId,
                   String version,
                   Map<String, Object> policyConfiguration,
                   List<PolicyPathApplication> policyPathApplications,
                   String id) {
        super(assetId,
              version,
              policyConfiguration,
              policyPathApplications)
        this.id = id
    }
}
