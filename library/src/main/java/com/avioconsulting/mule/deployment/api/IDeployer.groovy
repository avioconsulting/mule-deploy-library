package com.avioconsulting.mule.deployment.api

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy

interface IDeployer {
    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest)

    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest,
                          List<ApiSpecification> apiSpecifications)

    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest,
                          List<ApiSpecification> apiSpecifications,
                          List<Policy> desiredPolicies)

    def deployApplication(FileBasedAppDeploymentRequest appDeploymentRequest,
                          List<ApiSpecification> apiSpecifications,
                          List<Policy> desiredPolicies,
                          List<Features> enabledFeatures)
}
