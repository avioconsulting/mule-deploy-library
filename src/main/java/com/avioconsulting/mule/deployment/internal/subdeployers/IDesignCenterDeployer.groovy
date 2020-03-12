package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.models.ApiSpecification
import com.avioconsulting.mule.deployment.models.FileBasedAppDeploymentRequest

interface IDesignCenterDeployer {
    def synchronizeDesignCenterFromApp(ApiSpecification apiSpec,
                                       FileBasedAppDeploymentRequest appFileInfo,
                                       String appVersion)
}
