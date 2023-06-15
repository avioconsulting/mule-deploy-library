package com.avioconsulting.mule.deployment.api


import com.avioconsulting.mule.deployment.api.models.ApiSpecificationList
import com.avioconsulting.mule.deployment.api.models.deployment.AppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.api.models.policies.Policy

interface IDeployer {
    def deployApplication(AppDeploymentRequest appDeploymentRequest)

    def deployApplication(AppDeploymentRequest appDeploymentRequest,
                          ApiSpecificationList apiSpecifications)

    def deployApplication(AppDeploymentRequest appDeploymentRequest,
                          ApiSpecificationList apiSpecifications,
                          List<Policy> desiredPolicies)

    def deployApplication(AppDeploymentRequest appDeploymentRequest,
                          ApiSpecificationList apiSpecifications,
                          List<Policy> desiredPolicies,
                          List<Features> enabledFeatures)
}
