package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest

interface ISubDeployer<T extends FileBasedAppDeploymentRequest> {
    boolean isMule4Request(T deploymentRequest)
}
