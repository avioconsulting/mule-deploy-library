package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.deployment.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentItem
import org.apache.http.client.methods.HttpDelete

class CloudHubV2Deployer extends ExchangeBasedDeployer<CloudhubV2DeploymentRequest> implements ICloudHubV2Deployer {

    CloudHubV2Deployer(HttpClientWrapper clientWrapper,
                       EnvironmentLocator environmentLocator,
                       ILogger logger,
                       DryRunMode dryRunMode) {
        this(clientWrapper,
             environmentLocator,
             // for CloudHub, the deploy cycle is longer so we wait longer
             10000,
             100,
             logger,
             dryRunMode)
    }

    CloudHubV2Deployer(HttpClientWrapper clientWrapper,
                       EnvironmentLocator environmentLocator,
                       int retryIntervalInMs,
                       int maxTries,
                       ILogger logger,
                       DryRunMode dryRunMode) {
        super(retryIntervalInMs,
              maxTries,
              logger,
              clientWrapper,
              environmentLocator,
              dryRunMode)
    }

    def deploy(CloudhubV2DeploymentRequest deploymentRequest) {
        def groupId = deploymentRequest.groupId
        def envId = environmentLocator.getEnvironmentId(deploymentRequest.environment, groupId)
        def appName = deploymentRequest.normalizedAppName
        def targetId = getTargetId(deploymentRequest.target, groupId)
        deploymentRequest.setTargetId(targetId)

        DeploymentItem appInfo = getAppInfo(envId, groupId, appName)
        AppStatusPackage appStatus = getAppStatus(appInfo)

        doDeployment(deploymentRequest, envId, appInfo)

        if (dryRunMode != DryRunMode.Run) {
            return
        }
        waitForAppToStart(envId, groupId, appName, appStatus)
    }

    def getTargetId(String targetName, String businessGroupId) {
        def targets = getTargets(businessGroupId).findAll {
            env -> env.type == PRIVATE_SPACE_TARGET_TYPE || env.type == SHARED_SPACE_TARGET_TYPE
        }.collectEntries { target -> [target.name, target.id] }
        def target = targets[targetName]
        if (!target) {
            def valids = targets.keySet()
            throw new Exception("Unable to find valid cloudhub v2 target '${targetName}'. Valid targets are ${valids}")
        }
        return target
    }

    @Override
    boolean isMule4Request(CloudhubV2DeploymentRequest deploymentRequest) {
        // TODO: How are we going to support Mule 3 for RTF and CHv2?
        true
    }

    def deleteApp(String groupId,
                  String envId,
                  String normalizedAppName,
                  String failReason = 'remove failed app deployment') {

        def appName = normalizedAppName
        DeploymentItem appInfo = getAppInfo(envId, groupId, appName)

        if(appInfo != null) {
            def request = new HttpDelete("${clientWrapper.baseUrl}/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments/${appInfo.getId()}")
            def response = clientWrapper.execute(request)
            try {
                clientWrapper.assertSuccessfulResponse(response,
                        failReason)
            }
            finally {
                response.close()
            }
        }else {
            throw new Exception("No application available for deletion: ${appName}")
        }

    }

}
