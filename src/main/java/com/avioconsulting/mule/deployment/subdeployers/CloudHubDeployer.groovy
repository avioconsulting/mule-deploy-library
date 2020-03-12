package com.avioconsulting.mule.deployment.subdeployers

import com.avioconsulting.mule.deployment.httpapi.EnvironmentLocator
import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import com.avioconsulting.mule.deployment.models.AppStatus
import com.avioconsulting.mule.deployment.models.CloudhubDeploymentRequest
import com.avioconsulting.mule.deployment.models.DeploymentStatus
import groovy.json.JsonOutput
import org.apache.http.client.methods.*

class CloudHubDeployer extends BaseDeployer implements ICloudHubDeployer {
    static final Map<String, AppStatus> AppStatusMappings = [
            STARTED      : AppStatus.Started,
            DEPLOY_FAILED: AppStatus.Failed,
            UNDEPLOYED   : AppStatus.Undeployed,
            DELETED      : AppStatus.Deleted
    ]

    CloudHubDeployer(HttpClientWrapper clientWrapper,
                     EnvironmentLocator environmentLocator,
                     PrintStream logger) {
        this(clientWrapper,
             environmentLocator,
             60000,
             // for CloudHub, the deploy cycle is longer so we wait longer
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
        waitForAppToStart(deploymentRequest.environment,
                          deploymentRequest.normalizedAppName)
    }

    private HttpEntityEnclosingRequestBase getDeploymentHttpRequest(AppStatus existingAppStatus,
                                                                    CloudhubDeploymentRequest deploymentRequest) {
        def appName = deploymentRequest.normalizedAppName
        def fileName = deploymentRequest.file.name
        HttpEntityEnclosingRequestBase request
        if (existingAppStatus == AppStatus.NotFound) {
            logger.println "Deploying '${appName}', ${fileName} as a NEW application"
            request = new HttpPost("${clientWrapper.baseUrl}/cloudhub/api/v2/applications")
        } else if ([AppStatus.Undeployed, AppStatus.Failed].contains(existingAppStatus)) {
            // If you try and PUT a new version of an app over an existing failed deployment, CloudHub will reject it
            // so we delete first to clear out the failed deployment
            logger.println "Existing deployment of '${appName}' is in status '${existingAppStatus}' and is not currently running. Will remove first and then deploy a new copy"
            deleteApp(deploymentRequest.environment,
                      appName)
            waitForAppDeletion(deploymentRequest.environment,
                               appName)
            logger.println "App deleted, now deploying '${appName}', ${fileName} as a NEW application"
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

    def waitForAppDeletion(String environment,
                           String appName) {
        def tries = 0
        def deleted = false
        def failed = false
        logger.println 'Now checking to see if app has been deleted'
        while (!deleted && tries < this.maxTries) {
            tries++
            logger.println "*** Try ${tries} ***"
            AppStatus status = getAppStatus(environment,
                                            appName)
            logger.println "Received status of ${status}"
            if (status == AppStatus.NotFound) {
                logger.println 'App removed successfully!'
                deleted = true
                break
            }
            logger.println "Sleeping for ${this.retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(this.retryIntervalInMs)
        }
        if (!deleted && failed) {
            throw new Exception('Deletion failed on 1 or more nodes. Please see logs and messages as to why app did not start')
        }
        if (!deleted) {
            throw new Exception("Deletion has not completed after ${tries} tries!")
        }
    }

    def waitForAppToStart(String environment,
                          String appName) {
        logger.println 'Now will wait for application to start...'
        def tries = 0
        def deployed = false
        def failed = false
        while (!deployed && tries < this.maxTries) {
            tries++
            logger.println "*** Try ${tries} ***"
            Set<DeploymentStatus> status = getDeploymentStatus(environment,
                                                               appName)
            logger.println "Received statuses of ${status}"
            if (status.toList() == [DeploymentStatus.STARTED]) {
                logger.println 'App started successfully!'
                deployed = true
                break
            }
            if (status.contains(DeploymentStatus.FAILED)) {
                failed = true
                logger.println 'Deployment FAILED on 1 more nodes!'
                break
            }
            logger.println "Sleeping for ${this.retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(this.retryIntervalInMs)
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

    Set<DeploymentStatus> getDeploymentStatus(String environment,
                                              String appName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/cloudhub/api/v2/applications/${appName}/deployments?orderByDate=DESC").with {
            addStandardStuff(it,
                             environment)
            it
        } as HttpGet
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'deployment status')
            def data = result.data
            // we used order by desc date
            def lastChronologicalDeployment = data[0]
            def uniqueStatuses = lastChronologicalDeployment.instances.status.unique()
            def mapStatus = { String input ->
                switch (input) {
                    case 'STARTED':
                        return DeploymentStatus.STARTED
                    case 'TERMINATED':
                        return DeploymentStatus.FAILED
                    case 'DEPLOYING':
                        return DeploymentStatus.STARTING
                    default:
                        return DeploymentStatus.UNKNOWN
                }
            }
            return uniqueStatuses.collect { str -> mapStatus(str) }
        }
        finally {
            response.close()
        }
    }
}
