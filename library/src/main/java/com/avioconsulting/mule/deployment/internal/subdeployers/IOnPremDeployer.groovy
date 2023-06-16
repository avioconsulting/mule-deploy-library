package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.deployment.OnPremDeploymentRequest

interface IOnPremDeployer extends ISubDeployer<OnPremDeploymentRequest> {
    def deploy(OnPremDeploymentRequest deploymentRequest)
}
