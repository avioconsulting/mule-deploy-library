package com.avioconsulting.mule.deployment.api

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.OnPremDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.policies.Policy

interface IDeployer {
    def deployApplication(CloudhubDeploymentRequest appDeploymentRequest,
                          ApiSpecification apiSpecification,
                          List<Policy> desiredPolicies,
                          List<Features> enabledFeatures)

    def deployApplication(OnPremDeploymentRequest appDeploymentRequest,
                          ApiSpecification apiSpecification,
                          List<Policy> desiredPolicies,
                          List<Features> enabledFeatures)
}
