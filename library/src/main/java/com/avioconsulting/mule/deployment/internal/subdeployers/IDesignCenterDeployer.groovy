package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.deployment.FileBasedAppDeploymentRequest

interface IDesignCenterDeployer {
    def synchronizeDesignCenterFromApp(ApiSpecification apiSpec,
                                       FileBasedAppDeploymentRequest appFileInfo)
}
