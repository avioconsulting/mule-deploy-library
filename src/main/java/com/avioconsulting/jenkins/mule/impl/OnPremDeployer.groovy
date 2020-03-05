package com.avioconsulting.jenkins.mule.impl

import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPatch
import org.apache.http.client.methods.HttpPost
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class OnPremDeployer extends BaseDeployer {
    OnPremDeployer(String anypointOrganizationId,
                   String username,
                   String password,
                   PrintStream logger) {
        this('https://anypoint.mulesoft.com/',
             anypointOrganizationId,
             username,
             password,
             10000,
             30,
             logger)
    }

    OnPremDeployer(String baseUrl,
                   String anypointOrganizationId,
                   String username,
                   String password,
                   int retryIntervalInMs,
                   int maxTries,
                   PrintStream logger) {
        super(baseUrl,
              anypointOrganizationId,
              username,
              password,
              retryIntervalInMs,
              maxTries,
              logger)
    }

    def deploy(String environment,
               String appName,
               InputStream zipFile,
               String fileName,
               String targetServerOrClusterName,
               String propertyOverrides,
               String overrideByChangingFileInZip) {
        if (appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${appName}'!")
        }
        authenticate()
        def environmentId = locateEnvironment(environment)
        def existingApp = locateApplication(environmentId,
                                            appName)
        Map propertyMap = propertyOverrides ? parseProperties(propertyOverrides) : [:]
        def appId = existingApp ?
                updateDeployment(environmentId,
                                 appName,
                                 existingApp,
                                 zipFile,
                                 fileName,
                                 propertyMap,
                                 overrideByChangingFileInZip) :
                newDeployment(environmentId,
                              appName,
                              zipFile,
                              fileName,
                              targetServerOrClusterName,
                              propertyMap,
                              overrideByChangingFileInZip)
        logger.println 'Now will wait for application to start...'
        def tries = 0
        def deployed = false
        def failed = false
        while (!deployed && tries < this.maxTries) {
            tries++
            logger.println "*** Try ${tries} ***"
            Set<OnPremDeploymentStatus> status = getAppStatus(environmentId,
                                                              appId,
                                                              fileName)
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
    private String newDeployment(String environmentId,
                                 String appName,
                                 InputStream zipFile,
                                 String fileName,
                                 String targetServerOrClusterName,
                                 Map propertyMap,
                                 String overrideByChangingFileInZip) {
        def serverId = locateServer(environmentId,
                                    targetServerOrClusterName)
        logger.println "Deploying '${appName}', ${fileName} as a new application with additional properties ${propertyMap}"
        def request = new HttpPost("${baseUrl}/hybrid/api/v1/applications").with {
            addStandardStuff(it,
                             environmentId)
            def configJson = getConfigJson(appName,
                                           overrideByChangingFileInZip ? [:] : propertyMap)
            if (overrideByChangingFileInZip) {
                zipFile = modifyZipFileWithNewProperties(zipFile,
                                                         fileName,
                                                         overrideByChangingFileInZip,
                                                         propertyMap)
            }
            def entity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addTextBody('targetId',
                                 serverId)
                    .addTextBody('artifactName',
                                 appName)
                    .addTextBody('configuration',
                                 configJson)
                    .addBinaryBody('file',
                                   zipFile,
                                   ContentType.APPLICATION_OCTET_STREAM,
                                   fileName)
                    .build()
            setEntity(entity)
            it
        } as HttpPost
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'deploy application')
            def appId = result.data.id
            logger.println("Application '${appName}'/ID ${appId} has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
            return appId.toString()
        }
        finally {
            response.close()
        }
    }

    private String updateDeployment(String environmentId,
                                    String appName,
                                    String appId,
                                    InputStream zipFile,
                                    String fileName,
                                    Map propertyMap,
                                    String overrideByChangingFileInZip) {
        logger.println "Deploying '${appName}', ${fileName} as an UPDATED application to existing app id ${appId} with additional properties ${propertyMap}"
        def request = new HttpPatch("${baseUrl}/hybrid/api/v1/applications/${appId}").with {
            addStandardStuff(it,
                             environmentId)
            def configJson = getConfigJson(appName,
                                           overrideByChangingFileInZip ? [:] : propertyMap)
            if (overrideByChangingFileInZip) {
                zipFile = modifyZipFileWithNewProperties(zipFile,
                                                         fileName,
                                                         overrideByChangingFileInZip,
                                                         propertyMap)
            }
            def entity = MultipartEntityBuilder.create()
                    .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
                    .addTextBody('configuration',
                                 configJson)
                    .addBinaryBody('file',
                                   zipFile,
                                   ContentType.APPLICATION_OCTET_STREAM,
                                   fileName)
                    .build()
            setEntity(entity)
            it
        } as HttpPatch
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'deploy application')
            logger.println("Application '${appName}' has been accepted by Runtime Manager for deployment, details returned: ${JsonOutput.prettyPrint(JsonOutput.toJson(result))}")
            return appId
        }
        finally {
            response.close()
        }
    }

    private static String getConfigJson(String appName,
                                        Map propertyMap) {
        def map = [
                'mule.agent.application.properties.service': [
                        applicationName: appName,
                        properties     : propertyMap
                ]
        ]
        JsonOutput.toJson(map)
    }

    Set<OnPremDeploymentStatus> getAppStatus(String environmentId,
                                             String appId,
                                             String fileName) {
        def request = new HttpGet("${baseUrl}/hybrid/api/v1/applications/${appId}").with {
            addStandardStuff(it,
                             environmentId)
            it
        } as HttpGet
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
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

    String locateServer(String environmentId,
                        String serverOrClusterName) {
        def request = new HttpGet("${baseUrl}/hybrid/api/v1/servers").with {
            addStandardStuff(it,
                             environmentId)
            it
        }
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
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

    String locateApplication(String environmentId,
                             String applicationName) {
        def request = new HttpGet("${baseUrl}/hybrid/api/v1/applications").with {
            addStandardStuff(it,
                             environmentId)
            it
        }
        def response = httpClient.execute(request)
        try {
            def result = assertSuccessfulResponseAndReturnJson(response,
                                                               'Retrieve applications')
            def listing = result.data
            def app = listing.find { app ->
                app.name == applicationName
            }
            app?.id
        }
        finally {
            response.close()
        }
    }
}
