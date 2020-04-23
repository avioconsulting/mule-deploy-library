package com.avioconsulting.mule.deployment.api.models.policies

import groovy.transform.ToString

@ToString
class Policy {
    final String groupId
    final String assetId
    final String version
    final Map<String, Object> policyConfiguration
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
           Map<String, Object> policyConfiguration,
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

    boolean isPolicyConfigTheSame(Policy other) {
        this.policyConfigForEquals == other.policyConfigForEquals
    }

    Map<String, Object> getPolicyConfigForEquals() {
        this.policyConfiguration
    }

    boolean equals(o) {
        if (this.is(o)) return true
        // we don't care about subclasses here since the subclasses do not have fields of there own
        if (!(o instanceof Policy)) return false

        Policy policy = (Policy) o

        if (assetId != policy.assetId) return false
        if (groupId != policy.groupId) return false
        if (!isPolicyConfigTheSame(o)) return false
        if (policyPathApplications != policy.policyPathApplications) return false
        if (version != policy.version) return false

        return true
    }

    int hashCode() {
        int result
        result = groupId.hashCode()
        result = 31 * result + assetId.hashCode()
        result = 31 * result + version.hashCode()
        result = 31 * result + policyConfiguration.hashCode()
        result = 31 * result + policyPathApplications.hashCode()
        return result
    }
}
