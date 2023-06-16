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

class RuntimeFabricDeployer extends BaseDeployer implements IRuntimeFabricDeployer {

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

    protected def getTargets(String businessGroupId) {
        logger.println('Fetching all available targets')
        def groupId = businessGroupId ?: clientWrapper.anypointOrganizationId
        def request = new HttpGet("${clientWrapper.baseUrl}/runtimefabric/api/organizations/${groupId}/targets")
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                    "Unable to retrieve targets. Please ensure your org ID (${groupId}) is correct and the credentials you are using have the right permissions.)")
            return result
        }
        finally {
            response.close()
        }
    }

    protected def getTargetId(String targetName, String businessGroupId) {
        def targets = getTargets(businessGroupId).findAll { env -> env.type == RUNTIME_FABRIC_TARGET_TYPE }
                .collectEntries { target -> [target.name, target.id] }
        def target = targets[targetName]
        if (!target) {
            def valids = targets.keySet()
            throw new Exception("Unable to find a valid runtime fabric target '${targetName}'. Valid targets are ${valids}")
        }
        return target
    }

    private def doDeployment(RuntimeFabricDeploymentRequest deploymentRequest, String envId, DeploymentItem appInfo) {
        def groupId = deploymentRequest.groupId

        if (appInfo == null) {
            logger.println "Starting new deployment for application '${deploymentRequest.appName}'."
            createApp(deploymentRequest, envId, groupId)
        } else {
            logger.println "The name of the application '${deploymentRequest.appName}' is already used. Updating the application."
            updateApp(deploymentRequest, appInfo, envId, groupId)
        }
    }

    private def createApp(RuntimeFabricDeploymentRequest deploymentRequest,
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

    private def updateApp(RuntimeFabricDeploymentRequest deploymentRequest,
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

    def waitForAppToStart(String envId,
                          String groupId,
                          String appName,
                          AppStatusPackage baselineStatus) {
        logger.println 'Now will wait for application to start...'
        def tries = 0
        def deployed = false
        def failed = false
        def hasBaselineStatusChanged = false
        def sleep = {
            logger.println "Sleeping for ${this.retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(this.retryIntervalInMs)
        }
        while (!deployed && tries < this.maxTries) {
            tries++
            logger.println "*** Try ${tries} ***"
            DeploymentItem appInfo
            AppStatusPackage status
            try {
                appInfo = getAppInfo(envId,
                        groupId,
                        appName)
                status = getAppStatus(appInfo)
            } catch (e) {
                logger.println("Caught exception ${e.message} while checking app status, will ignore and retry")
                failed = true
                break
            }
            logger.println "Current status is '${status}'"
            if (status == baselineStatus && !hasBaselineStatusChanged) {
                logger.println "We have not seen the baseline status change from '${baselineStatus}' so will keep checking"
                sleep()
                continue
            } else if (status != baselineStatus && !hasBaselineStatusChanged) {
                hasBaselineStatusChanged = true
            }
            if (status.appStatus == AppStatus.Applied && status.deploymentUpdateStatus == DeploymentUpdateStatus.Running) {
                logger.println 'App started successfully!'
                deployed = true
                break
            }
            if (status.appStatus == AppStatus.Failed || status.appStatus == AppStatus.DeploymentFailed) {
                failed = true
                logger.println 'Deployment FAILED on 1 more nodes!'
                break
            }
            logger.println 'Have not seen Started state yet'
            sleep()
        }
        if (!deployed && failed) {
            throw new Exception('Deployment failed on 1 or more workers. Please see logs and messages as to why app did not start')
        }
        if (!deployed) {
            throw new Exception("Deployment has not failed but app has not started after ${tries} tries!")
        }
    }

    DeploymentItem getAppInfo(String envId,
                              String groupId,
                              String appName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/amc/application-manager/api/v2/organizations/${groupId}/environments/${envId}/deployments")
        def response = clientWrapper.execute(request)
        def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                "Error retrieving applications. Chek if the groupId (${groupId}) and environment (${envId}) provided are correct")

        def apps = result.items.collectEntries { app -> [
            app.name,
            new DeploymentItem(app.id,
                               app.name,
                               app.status,
                               app.application.status,
                               app.target.id)
        ]}
        def app = apps[appName]
        try {
            return app
        }
        finally {
            response.close()
        }
    }

    AppStatusPackage getAppStatus(DeploymentItem app) {
        if (app == null) {
            return new AppStatusPackage(AppStatus.NotFound, null)
        }
        def mapper = new AppStatusMapper()
        return mapper.parseAppStatus([status: app.getDeploymentStatus(), deploymentUpdateStatus: app.getAppStatus()])
    }

    @Override
    boolean isMule4Request(RuntimeFabricDeploymentRequest deploymentRequest) {
        // TODO: How are we going to support Mule 3 for RTF and CHv2?
        true
    }

}
