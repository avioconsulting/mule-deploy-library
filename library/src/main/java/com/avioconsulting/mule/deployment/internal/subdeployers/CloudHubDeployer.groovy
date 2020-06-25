package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import com.avioconsulting.mule.deployment.internal.models.AppStatusPackage
import com.avioconsulting.mule.deployment.internal.models.DeploymentUpdateStatus
import groovy.json.JsonOutput
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class CloudHubDeployer extends BaseDeployer implements ICloudHubDeployer {
    CloudHubDeployer(HttpClientWrapper clientWrapper,
                     EnvironmentLocator environmentLocator,
                     ILogger logger,
                     DryRunMode dryRunMode) {
        this(clientWrapper,
             environmentLocator,
             // for CloudHub, the deploy cycle is longer so we wait longer
             10000,
             50,
             logger,
             dryRunMode)
    }

    CloudHubDeployer(HttpClientWrapper clientWrapper,
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

    def deploy(CloudhubDeploymentRequest deploymentRequest) {
        def existingAppStatus = getAppStatus(deploymentRequest.environment,
                                             deploymentRequest.normalizedAppName)
        def request = getDeploymentHttpRequest(existingAppStatus.getAppStatus(),
                                               deploymentRequest)
        doDeployment(request,
                     deploymentRequest)
        if ([AppStatus.Undeployed, AppStatus.Failed].contains(existingAppStatus.appStatus)) {
            if (dryRunMode != DryRunMode.Run) {
                logger.println "Since existing app was in '${existingAppStatus}' status before the deployment we just did, we WOULD start the app manually but we're in dry run mode"
            } else {
                logger.println "Since existing app was in '${existingAppStatus}' status before the deployment we just did, we will now try and start the app manually"
                startApplication(deploymentRequest.environment,
                                 deploymentRequest.normalizedAppName)
            }
        }
        if (dryRunMode != DryRunMode.Run) {
            return
        }
        waitForAppToStart(deploymentRequest.environment,
                          deploymentRequest.normalizedAppName,
                          existingAppStatus)
    }

    private HttpEntityEnclosingRequestBase getDeploymentHttpRequest(AppStatus existingAppStatus,
                                                                    CloudhubDeploymentRequest deploymentRequest) {
        def appName = deploymentRequest.normalizedAppName
        def fileName = deploymentRequest.file.name
        HttpEntityEnclosingRequestBase request
        if (existingAppStatus == AppStatus.NotFound) {
            logger.println "Deploying '${appName}', ${fileName} as a NEW application"
            request = new HttpPost("${clientWrapper.baseUrl}/cloudhub/api/v2/applications")
        } else {
            logger.println "Deploying '${appName}', ${fileName} as an UPDATED application"
            request = new HttpPut("${clientWrapper.baseUrl}/cloudhub/api/v2/applications/${appName}")
        }
        request
    }

    private def doDeployment(HttpEntityEnclosingRequestBase request,
                             CloudhubDeploymentRequest deploymentRequest) {
        def prettyJson = JsonOutput.prettyPrint(deploymentRequest.cloudhubAppInfoAsJson)
        if (dryRunMode != DryRunMode.Run) {
            logger.println "WOULD deploy using settings but in dry-run mode: ${prettyJson}"
            return
        }
        logger.println "Deploying using settings: ${prettyJson}"
        request = request.with {
            addStandardStuff(it,
                             deploymentRequest.environment)
            def entity = deploymentRequest.getHttpPayload()
            setEntity(entity)
            it
        }
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'deploy application')
            logger.println("Application '${deploymentRequest.normalizedAppName}' has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
        }
        finally {
            response.close()
        }
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

    def waitForAppToStart(String environment,
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
            AppStatusPackage status
            try {
                status = getAppStatus(environment,
                                      appName)
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
            if (status.appStatus == AppStatus.Started && status.deploymentUpdateStatus == null) {
                logger.println 'App started successfully!'
                deployed = true
                break
            }
            if (status.appStatus == AppStatus.Failed || status.deploymentUpdateStatus == DeploymentUpdateStatus.Failed) {
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

    AppStatusPackage getAppStatus(String environmentName,
                                  String appName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/cloudhub/api/v2/applications/${appName}").with {
            addStandardStuff(it,
                             environmentName)
            it
        } as HttpGet
        def response = clientWrapper.execute(request)
        try {
            if (response.statusLine.statusCode == 404) {
                return new AppStatusPackage(AppStatus.NotFound,
                                            null)
            }
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'check app status')
            def mapper = new AppStatusMapper()
            return mapper.parseAppStatus(result)
        }
        finally {
            response.close()
        }
    }

    @Override
    boolean isMule4Request(CloudhubDeploymentRequest deploymentRequest) {
        // TODO: Is using this (which leans on JAR vs. ZIP) ok?
        deploymentRequest.isMule4Request()
    }

    def startApplication(String environment,
                         String appName) {
        def request = new HttpPost("${clientWrapper.baseUrl}/cloudhub/api/applications/${appName}/status").with {
            def payload = [
                    status: 'start'
            ]
            setHeader('X-ANYPNT-ENV-ID',
                      environmentLocator.getEnvironmentId(environment))
            setEntity(new StringEntity(JsonOutput.toJson(payload),
                                       ContentType.APPLICATION_JSON))
            it
        }
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
                                                             'Start application')
    }
}
