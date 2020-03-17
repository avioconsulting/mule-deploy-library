package com.avioconsulting.mule.deployment.api.models

import groovy.transform.EqualsAndHashCode

@EqualsAndHashCode
class Policy {
    final String assetId
    final String version
    final Map<String, String> policyConfiguration
    final List<PolicyPathApplication> policyPathApplications

    Policy(String assetId,
           String version,
           Map<String, String> policyConfiguration,
           List<PolicyPathApplication> policyPathApplications) {
        this.assetId = assetId
        this.version = version
        this.policyConfiguration = policyConfiguration
        this.policyPathApplications = policyPathApplications
    }
}
