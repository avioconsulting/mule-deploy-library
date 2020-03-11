package com.avioconsulting.mule.deployment.subdeployers

import com.avioconsulting.mule.deployment.models.OnPremDeploymentStatus
import com.avioconsulting.mule.deployment.httpapi.EnvironmentLocator
import com.avioconsulting.mule.deployment.httpapi.HttpClientWrapper
import com.avioconsulting.mule.deployment.models.OnPremDeploymentRequest
import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost

class OnPremDeployer extends BaseDeployer {
    /***
     * Instantiate using default anypoint.mulesoft.com URL
     * @param anypointOrganizationId
     * @param username
     * @param password
     * @param logger - will be used for all your log messages
     */
    OnPremDeployer(HttpClientWrapper clientWrapper,
                   EnvironmentLocator environmentLocator,
                   PrintStream logger) {
        this(clientWrapper,
             environmentLocator,
             10000,
             30,
             logger)
    }

    OnPremDeployer(HttpClientWrapper clientWrapper,
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

    /***
     * Do an on-prem deployment
     * @param request - deployment request
     */
    def deploy(OnPremDeploymentRequest request) {
        def existingApp = locateApplication(request.environment,
                                            request.appName)
        def appId = existingApp ?
                updateDeployment(existingApp,
                                 request) :
                newDeployment(request)
        logger.println 'Now will wait for application to start...'
        def tries = 0
        def deployed = false
        def failed = false
        while (!deployed && tries < this.maxTries) {
            tries++
            logger.println "*** Try ${tries} ***"
            Set<OnPremDeploymentStatus> status = getAppStatus(request.environment,
                                                              appId,
                                                              request.fileName)
            logger.println "Received statuses of ${status}"
            if (status[0] == OnPremDeploymentStatus.RECEIVED) {
                logger.println 'Still waiting for Runtime Manager to send app to server'
            }
            if (status.toList() == [OnPremDeploymentStatus.STARTED]) {
                logger.println 'App started successfully!'
                deployed = true
                break
            }
            if (status.contains(OnPremDeploymentStatus.FAILED)) {
                failed = true
                logger.println 'Deployment FAILED on 1 more nodes!'
                break
            }
            logger.println "Sleeping for ${this.retryIntervalInMs / 1000} seconds and will recheck..."
            Thread.sleep(this.retryIntervalInMs)
        }
        if (!deployed && failed) {
            throw new Exception('Deployment failed on 1 or more nodes. Please see logs and messages as to why app did not start')
        }
        if (!deployed) {
            throw new Exception("Deployment has not failed but app has not started after ${tries} tries!")
        }
    }

    /***
     *
     * @param environmentId
     * @param appName
     * @param zipFile
     * @param fileName
     * @param targetServerOrClusterName
     * @return - the app ID
     */
    private String newDeployment(OnPremDeploymentRequest deploymentRequest) {
        def serverId = locateServer(deploymentRequest.environment,
                                    deploymentRequest.targetServerOrClusterName)
        def appName = deploymentRequest.appName
        def fileName = deploymentRequest.fileName
        logger.println "Deploying '${appName}', ${fileName} as a new application with additional properties ${deploymentRequest.appProperties}"
        def request = new HttpPost("${clientWrapper.baseUrl}/hybrid/api/v1/applications").with {
            addStandardStuff(it,
                             deploymentRequest.environment)
            setEntity(deploymentRequest.getHttpPayload(serverId))
            it
        } as HttpPost
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'deploy application')
            def appId = result.data.id
            logger.println("Application '${appName}'/ID ${appId} has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
            return appId.toString()
        }
        finally {
            response.close()
        }
    }

    private String updateDeployment(String appId,
                                    OnPremDeploymentRequest deploymentRequest) {
        def appName = deploymentRequest.appName
        def fileName = deploymentRequest.fileName
        logger.println "Deploying '${appName}', ${fileName} as an UPDATED application to existing app id ${appId} with additional properties ${deploymentRequest.appProperties}"
        def request = new HttpPatch("${clientWrapper.baseUrl}/hybrid/api/v1/applications/${appId}").with {
            addStandardStuff(it,
                             deploymentRequest.environment)
            it.setEntity(deploymentRequest.updateHttpPayload)
            it
        } as HttpPatch
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'deploy application')
            logger.println("Application '${appName}' has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
            return appId
        }
        finally {
            response.close()
        }
    }

    Set<OnPremDeploymentStatus> getAppStatus(String environmentName,
                                             String appId,
                                             String fileName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/hybrid/api/v1/applications/${appId}").with {
            addStandardStuff(it,
                             environmentName)
            it
        } as HttpGet
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'app status')
            // matching the fileName (which SHOULD contain a version) is the only easy way to know what
            // serverArtifact matches the deployment we should just did
            // we can't use server artifact IDs because they don't exist in the new app POST response
            def matchingArtifacts = result.data.serverArtifacts.findAll { a ->
                a.artifact.fileName == fileName
            }
            assert matchingArtifacts.any(): "Unable to find server artifact from our deployment in ${result.data}!"
            // if we don't have a desiredStatus of STARTED, then we're 'UPDATING', hasn't even begun yet
            if (matchingArtifacts[0].desiredStatus != 'STARTED') {
                return [OnPremDeploymentStatus.RECEIVED]
            }
            def uniqueStatuses = matchingArtifacts.collect { a -> a.lastReportedStatus as String }.unique()
            logger.println("Raw status list from API is ${uniqueStatuses}")
            // now we have a steady state and we know something is happening
            def mapStatus = { String input ->
                switch (input) {
                    case 'STARTED':
                        return OnPremDeploymentStatus.STARTED
                    case 'DEPLOYMENT_FAILED':
                        return OnPremDeploymentStatus.FAILED
                    case 'STOPPING':
                        return OnPremDeploymentStatus.STOPPING
                    case 'STARTING':
                        return OnPremDeploymentStatus.STARTING
                    default:
                        return OnPremDeploymentStatus.UNKNOWN
                }
            }
            return uniqueStatuses.collect { str -> mapStatus(str) }
        }
        finally {
            response.close()
        }
    }

    String locateServer(String environmentName,
                        String serverOrClusterName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/hybrid/api/v1/servers").with {
            addStandardStuff(it,
                             environmentName)
            it
        }
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'Retrieve servers')
            def listing = result.data
            def server = listing.find { server ->
                server.name == serverOrClusterName
            }
            if (server) {
                logger.println "Identified server '${serverOrClusterName}' as ID '${server.id}'"
                return server.id
            }
            def cluster = listing.find { s ->
                s.clusterName == serverOrClusterName
            }
            if (!cluster) {
                def valids = listing.collect { s2 ->
                    [s2.name, s2.clusterName]
                }.flatten().unique()
                valids.remove(null)
                throw new Exception("Unable to find server/cluster '${serverOrClusterName}'. Valid servers/clusters are ${valids}")
            }
            logger.println "Identified cluster '${serverOrClusterName}' as ID '${cluster.clusterId}'"
            cluster.clusterId
        }
        finally {
            response.close()
        }
    }

    String locateApplication(String environmentName,
                             String appName) {
        def request = new HttpGet("${clientWrapper.baseUrl}/hybrid/api/v1/applications").with {
            addStandardStuff(it,
                             environmentName)
            it
        }
        def response = clientWrapper.execute(request)
        try {
            def result = clientWrapper.assertSuccessfulResponseAndReturnJson(response,
                                                                             'Retrieve applications')
            def listing = result.data
            def app = listing.find { app ->
                app.name == appName
            }
            app?.id
        }
        finally {
            response.close()
        }
    }

    def deleteApp(String environmentName,
                  String appId) {
        def request = new HttpDelete("${clientWrapper.baseUrl}/hybrid/api/v1/applications/${appId}").with {
            addStandardStuff(it,
                             environmentName)
            it
        }
        def response = clientWrapper.execute(request)
        response.statusLine.statusCode
    }
}
