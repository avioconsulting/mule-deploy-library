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

    Policy(String groupId,
           String assetId,
           String version,
           Map<String, String> policyConfiguration,
           List<PolicyPathApplication> policyPathApplications) {
        this.groupId = groupId
        this.assetId = assetId
        this.version = version
        this.policyConfiguration = policyConfiguration
        this.policyPathApplications = policyPathApplications
    }
}
