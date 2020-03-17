package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.AppStatus
import groovy.json.JsonOutput
import org.apache.http.client.methods.*
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class CloudHubDeployer extends BaseDeployer implements ICloudHubDeployer {
    static final Map<String, AppStatus> AppStatusMappings = [
            STARTED      : AppStatus.Started,
            DEPLOY_FAILED: AppStatus.Failed,
            UNDEPLOYED   : AppStatus.Undeployed,
            DELETED      : AppStatus.Deleted,
            UNDEPLOYING  : AppStatus.Undeploying,
            DEPLOYING    : AppStatus.Deploying
    ]

    CloudHubDeployer(HttpClientWrapper clientWrapper,
                     EnvironmentLocator environmentLocator,
                     PrintStream logger) {
        this(clientWrapper,
             environmentLocator,
             // for CloudHub, the deploy cycle is longer so we wait longer
             10000,
             30,
             logger)
    }

    CloudHubDeployer(HttpClientWrapper clientWrapper,
                     EnvironmentLocator environmentLocator,
                     int retryIntervalInMs,
                     int maxTries,
                     PrintStream logger) {
        super(retryIntervalInMs,
              maxTries,
              logger,
              clientWrapper,
              environmentLocator)
    }

    def deploy(CloudhubDeploymentRequest deploymentRequest) {
        def existingAppStatus = getAppStatus(deploymentRequest.environment,
                                             deploymentRequest.normalizedAppName)
        def request = getDeploymentHttpRequest(existingAppStatus,
                                               deploymentRequest)
        doDeployment(request,
                     deploymentRequest)
        if ([AppStatus.Undeployed, AppStatus.Failed].contains(existingAppStatus)) {
            logger.println "Since existing app was in '${existingAppStatus}' status before the deployment we just did, we will now try and start the app manually"
            startApplication(deploymentRequest.environment,
                             deploymentRequest.normalizedAppName)
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
        logger.println "Deploying using settings: ${JsonOutput.prettyPrint(deploymentRequest.cloudhubAppInfoAsJson)}"
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
                          AppStatus baselineStatus) {
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
            def status = getAppStatus(environment,
                                      appName)
            logger.println "Current status is '${status}'"
            if (status == baselineStatus && !hasBaselineStatusChanged) {
                logger.println "We have not seen the baseline status change from '${baselineStatus}' so will keep checking"
                sleep()
                continue
            } else if (status != baselineStatus && !hasBaselineStatusChanged) {
                hasBaselineStatusChanged = true
            }
            if (status == AppStatus.Started) {
                logger.println 'App started successfully!'
                deployed = true
                break
            }
            if (status == AppStatus.Failed) {
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

    AppStatus getAppStatus(String environmentName,
                           String appName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/cloudhub/api/v2/applications/${appName}").with {
            addStandardStuff(it,
                             environmentName)
            it
        } as HttpGet
        def response = clientWrapper.execute(request)
        try {
            if (response.statusLine.statusCode == 404) {
                return AppStatus.NotFound
            }
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'app status')
            def input = result.status
            def mappedStatus = AppStatusMappings[input]
            if (!mappedStatus) {
                throw new Exception("Unknown status value of ${input} detected from CloudHub!")
            }
            return mappedStatus
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
