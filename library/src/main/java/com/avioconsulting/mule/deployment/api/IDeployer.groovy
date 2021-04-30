package com.avioconsulting.mule.deployment.api


import com.avioconsulting.mule.deployment.api.models.ApiSpecificationList
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy

interface IDeployer {
    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest)

    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest,
                          ApiSpecificationList apiSpecifications)

    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest,
                          ApiSpecificationList apiSpecifications,
                          List<Policy> desiredPolicies)

    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest,
                          ApiSpecificationList apiSpecifications,
                          List<Policy> desiredPolicies,
                          List<Features> enabledFeatures)
}
