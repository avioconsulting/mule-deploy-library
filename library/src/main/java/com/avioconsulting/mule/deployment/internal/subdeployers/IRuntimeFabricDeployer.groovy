package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest

interface IRuntimeFabricDeployer extends ISubDeployer<RuntimeFabricDeploymentRequest> {
    def deploy(RuntimeFabricDeploymentRequest deploymentRequest)
}
