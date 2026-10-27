package com.avioconsulting.mule.deployment.internal.models

import com.avioconsulting.mule.deployment.api.models.policies.Policy
import com.avioconsulting.mule.deployment.api.models.policies.PolicyPathApplication
import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode(callSuper = true)
@ToString(includeSuper = true)
class ExistingPolicy extends Policy {
    final String id
    final boolean disabled
    final Integer order

    ExistingPolicy(String assetId,
                   String version,
                   Map<String, Object> policyConfiguration,
                   String groupId,
                   List<PolicyPathApplication> policyPathApplications,
                   String id,
                   boolean disabled = false,
                   Integer order = null) {
        super(assetId,
              version,
              policyConfiguration,
              groupId,
              policyPathApplications)
        this.id = id
        this.disabled = disabled
        this.order = order
    }

    Policy getWithoutId() {
        new Policy(assetId,
                   version,
                   policyConfiguration,
                   groupId,
                   policyPathApplications)
    }
}
