package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.AppDeploymentRequest

interface ISubDeployer<T extends AppDeploymentRequest> {
    boolean isMule4Request(T deploymentRequest)
}
