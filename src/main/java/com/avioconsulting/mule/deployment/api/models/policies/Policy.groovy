package com.avioconsulting.mule.deployment.api.models.policies

import groovy.transform.EqualsAndHashCode
import groovy.transform.ToString

@EqualsAndHashCode
@ToString
class Policy {
    final String groupId
    final String assetId
    final String version
    final Map<String, String> policyConfiguration
    final List<PolicyPathApplication> policyPathApplications

    static String getMulesoftGroupId() {
        '68ef9520-24e9-4cf2-b2f5-620025690913'
    }

    /**
     * Create a policy
     * @param assetId name of the policy asset (might have to use APIs to find this)
     * @param version version
     * @param policyConfiguration additional policy configuration
     * @param groupId - Which group (goes with asset ID) is the policy from? If not supplied, it will be assumed to be your own group (e.g. a custom policy). You can also use the getMulesoftGroupId static method
     * @param policyPathApplications - Optional. If not supplied, will apply to every resource
     */
    Policy(String assetId,
           String version,
           Map<String, String> policyConfiguration,
           String groupId = null,
           List<PolicyPathApplication> policyPathApplications = null) {
        this.groupId = groupId
        this.assetId = assetId
        this.version = version
        this.policyConfiguration = policyConfiguration
        this.policyPathApplications = policyPathApplications
    }

    Policy withGroupId(String groupId) {
        new Policy(assetId,
                   version,
                   policyConfiguration,
                   groupId,
                   policyPathApplications)
    }
}
