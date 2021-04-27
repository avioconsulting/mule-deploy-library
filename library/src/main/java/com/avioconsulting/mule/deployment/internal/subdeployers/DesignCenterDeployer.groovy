package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.internal.http.EnvironmentLocator
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import groovy.json.JsonOutput
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class DesignCenterDeployer implements DesignCenterHttpFunctionality, IDesignCenterDeployer, ApiManagerFunctionality {
    private final HttpClientWrapper clientWrapper
    private final ILogger logger
    private final DryRunMode dryRunMode
    private final EnvironmentLocator environmentLocator

    DesignCenterDeployer(HttpClientWrapper clientWrapper,
                         ILogger logger,
                         DryRunMode dryRunMode,
                         EnvironmentLocator environmentLocator) {
        this.environmentLocator = environmentLocator
        this.dryRunMode = dryRunMode
        this.logger = logger
        this.clientWrapper = clientWrapper
    }

    EnvironmentLocator getEnvironmentLocator() {
        return environmentLocator
    }

    HttpClientWrapper getClientWrapper() {
        return clientWrapper
    }

    ILogger getLogger() {
        return logger
    }

    String getDesignCenterProjectId(String projectName) {
        logger.println "Looking up ID for Design Center project '${projectName}'"
        def request = new HttpGet("${clientWrapper.baseUrl}/designcenter/api-designer/projects")
        def failureContext = "fetch design center project ID for '${projectName}'"
        executeDesignCenterRequest(request,
                                   failureContext) { results ->
            def id = results.find { result ->
                result.name == projectName
            }?.id
            if (id) {
                logger.println "Identified Design Center project '${projectName}' as ID ${id}"
            } else {
                throw new Exception("Unable to find ID for Design Center project '${projectName}'")
            }
            return id
        }
    }

    def deleteDesignCenterFiles(String projectId,
                                List<RamlFile> files) {
        files.each { ramlFile ->
            def file = ramlFile.fileName
            logger.println("Removing unused file from Design Center: ${file}")
            // subdirectories in filename do not go in RESTful URL
            def urlEncodedFileName = URLEncoder.encode(file)
            executeDesignCenterRequest(new HttpDelete("${getFilesUrl(projectId)}/${urlEncodedFileName}"),
                                       "Removing file ${file}")
        }
    }

    def uploadDesignCenterFiles(String projectId,
                                List<RamlFile> files) {
        logger.println("Uploading files: ${files.collect { f -> f.fileName }} to Design Center")
        def requestPayload = files.collect { file ->
            [
                    path   : file.fileName,
                    content: file.contents
            ]
        }
        def request = new HttpPost("${getMasterUrl(projectId)}/save?commit=true&message=fromAPI").with {
            setEntity(new StringEntity(JsonOutput.toJson(requestPayload),
                                       ContentType.APPLICATION_JSON))
            it
        }
        executeDesignCenterRequest(request,
                                   'Uploading design center files')
    }

    def getMasterUrl(String projectId) {
        getMasterUrl(clientWrapper,
                     projectId)
    }

    private def getFilesUrl(String projectId) {
        "${getMasterUrl(projectId)}/files"
    }

    def executeDesignCenterRequest(HttpUriRequest request,
                                   String failureContext,
                                   Closure resultHandler = null) {
        executeDesignCenterRequest(clientWrapper,
                                   request,
                                   failureContext,
                                   resultHandler)
    }

    List<RamlFile> getExistingDesignCenterFiles(String projectId) {
        logger.println('Fetching list of existing Design Center RAML files')
        def url = getFilesUrl(projectId)
        def request = new HttpGet(url)
        executeDesignCenterRequest(request,
                                   'Fetching project files') { List<Map> results ->
            def filesWeCareAbout = results.findAll { result ->
                def asFile = new File(result.path)
                result.type != 'FOLDER' && !FileBasedAppDeploymentRequest.isIgnored(asFile.toPath())
            }
            return filesWeCareAbout.collect { result ->
                def filePath = result.path
                logger.println("Fetching file ${filePath}")
                def escapedForUrl = URLEncoder.encode(filePath)
                executeDesignCenterRequest(new HttpGet("${url}/${escapedForUrl}"),
                                           "Fetching file ${filePath}") { String contents ->
                    new RamlFile(filePath,
                                 contents)
                }
            }
        }
    }

    def pushToExchange(ApiSpecification apiSpec,
                       String projectId,
                       String appVersion) {
        def requestPayload = [
                main      : apiSpec.mainRamlFile,
                apiVersion: apiSpec.apiMajorVersion,
                version   : appVersion,
                assetId   : apiSpec.exchangeAssetId,
                name      : apiSpec.name,
                groupId   : clientWrapper.anypointOrganizationId,
                classifier: 'raml'
        ]
        def requestJson = JsonOutput.toJson(requestPayload)
        logger.println "Pushing to Exchange with payload ${JsonOutput.prettyPrint(requestJson)}"
        def request = new HttpPost("${getMasterUrl(projectId)}/publish/exchange").with {
            setEntity(new StringEntity(requestJson,
                                       ContentType.APPLICATION_JSON))
            it
        }
        executeDesignCenterRequest(request,
                                   'Publishing to Exchange')
    }

    def synchronizeDesignCenterFromApp(ApiSpecification apiSpec,
                                       FileBasedAppDeploymentRequest appFileInfo) {
        synchronizeDesignCenter(apiSpec,
                                appFileInfo.ramlFilesFromApp,
                                appFileInfo.appVersion)
    }

    def synchronizeDesignCenter(ApiSpecification apiSpec,
                                List<RamlFile> ramlFiles,
                                String appVersion) {
        def projectId = getDesignCenterProjectId(apiSpec.name)
        def withLock = { Closure closure ->
            new DesignCenterLock(clientWrapper,
                                 logger,
                                 projectId).withCloseable {
                closure()
            }
        }
        def existingFiles = getExistingDesignCenterFiles(projectId)
        def changes = ramlFiles - existingFiles
        if (changes.empty) {
            logger.println('New RAML contents match the old contents, will not update Design Center')
            def assets = getExchangeAssets(apiSpec.exchangeAssetId)
            if (assets.empty) {
                if (dryRunMode != DryRunMode.Run) {
                    logger.println('No exchange asset was found so we WOULD have pushed to Exchange, but in dry-run mode')
                    return
                }
                logger.println 'RAMLs have not changed but asset does not exist in Exchange so we have to push'
                withLock {
                    pushToExchange(apiSpec,
                                   projectId,
                                   appVersion)
                }
            } else {
                logger.println 'Exchange asset exists, no need for push'
            }
        } else {
            if (dryRunMode != DryRunMode.Run) {
                logger.println('RAML quantity/contents have changed, WOULD update Design Center but in dry-run mode')
                return
            }
            logger.println('RAML quantity/contents have changed, will update Design Center')
            def noLongerExist = existingFiles.findAll { file ->
                !ramlFiles.any { toBeFile -> file.fileName == toBeFile.fileName }
            }
            withLock {
                if (noLongerExist.any()) {
                    deleteDesignCenterFiles(projectId,
                                            noLongerExist)
                } else {
                    logger.println('No existing files to delete')
                }
                uploadDesignCenterFiles(projectId,
                                        changes)
                pushToExchange(apiSpec,
                               projectId,
                               appVersion)
            }
        }
    }
}
