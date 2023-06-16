package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubDeploymentRequest

interface ICloudHubDeployer extends ISubDeployer<CloudhubDeploymentRequest> {
    def deploy(CloudhubDeploymentRequest deploymentRequest)
}
