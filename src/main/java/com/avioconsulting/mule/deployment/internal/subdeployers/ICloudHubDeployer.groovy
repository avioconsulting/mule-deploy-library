package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest

interface ICloudHubDeployer extends IDeployer<CloudhubDeploymentRequest> {
    def deploy(CloudhubDeploymentRequest deploymentRequest)
}
