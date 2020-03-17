package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage

class AppStatusMapper {
    static final Map<String, AppStatus> AppStatusMappings = [
            STARTED      : AppStatus.Started,
            DEPLOY_FAILED: AppStatus.Failed,
            UNDEPLOYED   : AppStatus.Undeployed,
            DELETED      : AppStatus.Deleted,
            UNDEPLOYING  : AppStatus.Undeploying,
            DEPLOYING    : AppStatus.Deploying
    ]

    AppStatusPackage parseAppStatus(Map muleStatusResponse) {
        def parsedAppStatus = AppStatusMappings[muleStatusResponse.status]
        new AppStatusPackage(parsedAppStatus,
                             null)
    }
}
