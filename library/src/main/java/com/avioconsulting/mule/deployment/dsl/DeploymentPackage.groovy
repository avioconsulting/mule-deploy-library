package com.avioconsulting.mule.deployment.dsl


import com.avioconsulting.mule.deployment.api.models.ApiSpecificationList
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy
import groovy.transform.Canonical

@Canonical
class DeploymentPackage {
    FileBasedAppDeploymentRequest deploymentRequest
    ApiSpecificationList apiSpecifications
    List<Policy> desiredPolicies
    List<Features> enabledFeatures
}
