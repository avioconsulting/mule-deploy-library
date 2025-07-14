package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.deployment.RuntimeFabricDeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentItem
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.StringEntity

class RuntimeFabricDeployer extends ExchangeBasedDeployer<RuntimeFabricDeploymentRequest> implements IRuntimeFabricDeployer {

    RuntimeFabricDeployer(HttpClientWrapper clientWrapper,
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

    RuntimeFabricDeployer(HttpClientWrapper clientWrapper,
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

    RuntimeFabricDeployer(int retryIntervalInMs,
                          int maxTries,
                          ILogger logger,
                          HttpClientWrapper clientWrapper,
                          EnvironmentLocator environmentLocator,
                          DryRunMode dryRunMode) {
        super(retryIntervalInMs,
              maxTries,
              logger,
              clientWrapper,
              environmentLocator,
              dryRunMode)
    }

    @Override
    def deploy(RuntimeFabricDeploymentRequest deploymentRequest) {
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
    @Override
    boolean isMule4Request(RuntimeFabricDeploymentRequest deploymentRequest) {
        // TODO: How are we going to support Mule 3 for RTF and CHv2?
        true
    }

}
