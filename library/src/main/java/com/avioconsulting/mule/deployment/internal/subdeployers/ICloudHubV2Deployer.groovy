package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.CloudhubV2DeploymentRequest

interface ICloudHubV2Deployer extends ISubDeployer<CloudhubV2DeploymentRequest> {
    def deploy(CloudhubV2DeploymentRequest deploymentRequest)
}
