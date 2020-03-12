package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.models.CloudhubDeploymentRequest

interface ICloudHubDeployer {
    def deploy(CloudhubDeploymentRequest deploymentRequest)
}
