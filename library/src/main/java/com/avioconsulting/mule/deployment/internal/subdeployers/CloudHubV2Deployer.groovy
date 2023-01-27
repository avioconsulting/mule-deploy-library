package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentItem
import groovy.json.JsonOutput
import org.apache.http.client.methods.*
import org.apache.http.entity.StringEntity

class CloudHubV2Deployer extends RuntimeFabricDeployer implements ICloudHubV2Deployer {

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

    private def doDeployment(CloudhubV2DeploymentRequest deploymentRequest, String envId, DeploymentItem appInfo) {
        def groupId = deploymentRequest.groupId

        if (appInfo == null) {
            logger.println "Starting new deployment for application '${deploymentRequest.appName}'."
            createApp(deploymentRequest, envId, groupId)
        } else {
            logger.println "The name of the application '${deploymentRequest.appName}' is already used. Updating the application."
            updateApp(deploymentRequest, appInfo, envId, groupId)
        }
    }

    private def createApp(CloudhubV2DeploymentRequest deploymentRequest,
                                  String envId,
                                  String groupId) {
        def request = new HttpPost("${clientWrapper.baseUrl}/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments")
        def prettyJson = JsonOutput.prettyPrint(deploymentRequest.cloudhubAppInfoAsJson)
        if (dryRunMode != DryRunMode.Run) {
            logger.println "WOULD deploy using settings but in dry-run mode: ${prettyJson}"
            return
        }
        logger.println "Deploying using settings: ${prettyJson}"
        request = request.with {
            setHeader('Content-Type', 'application/json')
            addStandardStuff(it, deploymentRequest.environment)
            setEntity(new StringEntity(prettyJson))
            it
        }
        def response = clientWrapper.execute(request)
        def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,'deploy application')
        logger.println("Application '${deploymentRequest.normalizedAppName}' has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
        response.close()
    }

    private def updateApp(CloudhubV2DeploymentRequest deploymentRequest,
                          DeploymentItem appInfo,
                          String envId,
                          String groupId) {
        def request = new HttpPatch("${clientWrapper.baseUrl}/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments/${appInfo.getId()}")
        def prettyJson = JsonOutput.prettyPrint(deploymentRequest.cloudhubAppInfoAsJson)
        if (dryRunMode != DryRunMode.Run) {
            logger.println "WOULD deploy using settings but in dry-run mode: ${prettyJson}"
            return
        }
        logger.println "Deploying using settings: ${prettyJson}"
        request = request.with {
            setHeader('Content-Type', 'application/json')
            addStandardStuff(it, deploymentRequest.environment)
            setEntity(new StringEntity(prettyJson))
            it
        }
        def response = clientWrapper.execute(request)
        def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,'deploy application')
        logger.println("Application '${deploymentRequest.normalizedAppName}' has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
        response.close()
    }

    @Override
    boolean isMule4Request(CloudhubV2DeploymentRequest deploymentRequest) {
        // TODO: How are we going to support Mule 3 for RTF and CHv2?
        true
    }

}
