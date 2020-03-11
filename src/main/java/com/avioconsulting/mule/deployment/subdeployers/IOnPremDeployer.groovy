package com.avioconsulting.mule.deployment.subdeployers

import com.avioconsulting.mule.deployment.models.OnPremDeploymentRequest

interface IOnPremDeployer {
    def deploy(OnPremDeploymentRequest deploymentRequest)
}
