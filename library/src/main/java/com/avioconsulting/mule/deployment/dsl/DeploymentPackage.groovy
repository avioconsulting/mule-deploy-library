package com.avioconsulting.mule.deployment.dsl

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import groovy.transform.Canonical

@Canonical
class DeploymentPackage {
    FileBasedAppDeploymentRequest deploymentRequest
    ApiSpecification apiSpecification
    List<Policy> desiredPolicies
    List<Features> enabledFeatures
}
