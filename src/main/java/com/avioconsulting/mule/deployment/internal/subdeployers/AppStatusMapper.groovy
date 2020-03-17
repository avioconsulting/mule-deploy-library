package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus

class AppStatusMapper {
    static final Map<String, AppStatus> AppStatusMappings = [
            STARTED      : AppStatus.Started,
            DEPLOY_FAILED: AppStatus.Failed,
            UNDEPLOYED   : AppStatus.Undeployed,
            DELETED      : AppStatus.Deleted,
            UNDEPLOYING  : AppStatus.Undeploying,
            DEPLOYING    : AppStatus.Deploying
    ]

    static final Map<String, DeploymentUpdateStatus> DeployUpdateStatusMappings = [
            DEPLOY_FAILED: DeploymentUpdateStatus.Failed,
            DEPLOYING    : DeploymentUpdateStatus.Deploying
    ]

    AppStatusPackage parseAppStatus(Map muleStatusResponse) {
        def parsedAppStatus = AppStatusMappings[muleStatusResponse.status]
        def parsedDeployUpdateStatus = DeployUpdateStatusMappings[muleStatusResponse.deploymentUpdateStatus]
        new AppStatusPackage(parsedAppStatus,
                             parsedDeployUpdateStatus)
    }
}
