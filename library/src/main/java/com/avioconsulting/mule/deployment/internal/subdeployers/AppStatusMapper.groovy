package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus

class AppStatusMapper {
    static final Map<String, AppStatus> AppStatusMappings = [
            STARTED           : AppStatus.Started,
            DEPLOY_FAILED     : AppStatus.Failed,
            UNDEPLOYED        : AppStatus.Undeployed,
            DELETED           : AppStatus.Deleted,
            UNDEPLOYING       : AppStatus.Undeploying,
            DEPLOYING         : AppStatus.Deploying,
            PARTIALLY_STARTED : AppStatus.PartiallyStarted,
            DEPLOYMENT_FAILED : AppStatus.DeploymentFailed,
            STARTING          : AppStatus.Starting,
            STOPPING          : AppStatus.Stopping,
            STOPPED           : AppStatus.Stopped,
            UPDATED           : AppStatus.Updated,
            APPLIED           : AppStatus.Applied,
            APPLYING          : AppStatus.Applying,
    ]

    static final Map<String, DeploymentUpdateStatus> DeployUpdateStatusMappings = [
            DEPLOY_FAILED: DeploymentUpdateStatus.Failed,
            DEPLOYING    : DeploymentUpdateStatus.Deploying,
            RUNNING      : DeploymentUpdateStatus.Running,
            NOT_RUNNING  : DeploymentUpdateStatus.NotRunning
    ]

    AppStatusPackage parseAppStatus(Map muleStatusResponse) {
        def input = muleStatusResponse.status
        def parsedAppStatus = AppStatusMappings[input]
        if (parsedAppStatus == null) {
            throw new Exception("Unknown status value of ${input} detected from CloudHub!")
        }
        input = muleStatusResponse.deploymentUpdateStatus
        def parsedDeployUpdateStatus = DeployUpdateStatusMappings[input]
        if (input != null && parsedDeployUpdateStatus == null) {
            throw new Exception("Unknown parsedDeployUpdateStatus value of ${input} detected from CloudHub!")
        }
        new AppStatusPackage(parsedAppStatus,
                             parsedDeployUpdateStatus)
    }
}
