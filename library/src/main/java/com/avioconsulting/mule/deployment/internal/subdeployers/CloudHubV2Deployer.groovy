package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.CloudhubV2DeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentItem
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import groovy.json.JsonOutput
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class CloudHubV2Deployer extends BaseDeployer implements ICloudHubV2Deployer {

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
        def envId = environmentLocator.getEnvironmentId(deploymentRequest.environment)
        def groupId = deploymentRequest.groupId
        def appName = deploymentRequest.appName
        def targetId = getTargetId(deploymentRequest.target)
        deploymentRequest.setTargetId(targetId)

        DeploymentItem appInfo = getAppInfo(envId, groupId, appName)
        AppStatusPackage appStatus = getAppStatus(appInfo)

        doDeployment(deploymentRequest, envId, appInfo)

        if (dryRunMode != DryRunMode.Run) {
            return
        }
        waitForAppToStart(envId, groupId, appName, appStatus)
    }

    def getTargetId(String targetName) {
        logger.println('Fetching all available targets')
        def groupId = clientWrapper.anypointOrganizationId
        def request = new HttpGet("${clientWrapper.baseUrl}/runtimefabric/api/organizations/${groupId}/targets")
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                    "Unable to retrieve targets. Please ensure your org ID (${groupId}) is correct and the credentials you are using have the right permissions.)")
            def targets = result.collectEntries { target ->
                [target.name, target.id]
            }
            def target = targets[targetName]
            if (!target) {
                def valids = targets.keySet()
                throw new Exception("Unable to find target '${targetName}'. Valid targets are ${valids}")
            }
            return target
        }
        finally {
            response.close()
        }
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

    def deleteApp(String environment,
                  String appName,
                  String defaultFailReason = 'remove failed app deployment') {
        def request = new HttpDelete("${clientWrapper.baseUrl}/cloudhub/api/v2/applications/${appName}").with {
            addStandardStuff(it,
                             environment)
            it
        } as HttpDelete
        def response = clientWrapper.execute(request)
        try {
            clientWrapper.assertSuccessfulResponse(response,
                                                   defaultFailReason)
        }
        finally {
            response.close()
        }
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
                sleep()
                continue
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
                               app.target.id,
                               app.target.provider)
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
    boolean isMule4Request(CloudhubV2DeploymentRequest deploymentRequest) {
        // TODO: Is using this (which leans on JAR vs. ZIP) ok?
        deploymentRequest.isMule4Request()
    }

    def startApplication(String envId,
                         String appName) {
        def request = new HttpPost("${clientWrapper.baseUrl}/cloudhub/api/applications/${appName}/status").with {
            def payload = [
                    status: 'start'
            ]
            setHeader('X-ANYPNT-ENV-ID', envId)
            setEntity(new StringEntity(JsonOutput.toJson(payload),
                                       ContentType.APPLICATION_JSON))
            it
        }
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'Start application')
    }
}
