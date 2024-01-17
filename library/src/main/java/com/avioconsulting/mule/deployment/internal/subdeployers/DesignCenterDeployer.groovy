package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.deployment.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.api.models.Version
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
                                String branch,
                                List<RamlFile> files) {
        files.each { ramlFile ->
            def file = ramlFile.fileName
            logger.println("Removing unused file from Design Center: ${file}")
            // subdirectories in filename do not go in RESTful URL
            def urlEncodedFileName = URLEncoder.encode(file)
            executeDesignCenterRequest(new HttpDelete("${getFilesUrl(projectId, branch)}/${urlEncodedFileName}"),
                                       "Removing file ${file}")
        }
    }

    def uploadDesignCenterFiles(String projectId,
                                String branch,
                                List<RamlFile> files) {
        logger.println("Uploading files: ${files.collect { f -> f.fileName }} to Design Center")
        def requestPayload = files.collect { file ->
            [
                    path   : file.fileName,
                    content: file.contents
            ]
        }
        def request = new HttpPost("${getBranchUrl(projectId, branch)}/save?commit=true&message=fromAPI").with {
            setEntity(new StringEntity(JsonOutput.toJson(requestPayload),
                                       ContentType.APPLICATION_JSON))
            it
        }
        executeDesignCenterRequest(request,
                                   'Uploading design center files')
    }

    def getBranchUrl(String projectId,
                     String branch) {
        getBranchUrl(clientWrapper,
                     projectId,
                     branch)
    }

    private def getFilesUrl(String projectId,
                            String branch) {
        "${getBranchUrl(projectId, branch)}/files"
    }

    def executeDesignCenterRequest(HttpUriRequest request,
                                   String failureContext,
                                   Closure resultHandler = null) {
        executeDesignCenterRequest(clientWrapper,
                                   request,
                                   failureContext,
                                   resultHandler)
    }

    List<RamlFile> getExistingDesignCenterFiles(String projectId,
                                                String branch) {
        logger.println('Fetching list of existing Design Center RAML files')
        def url = getFilesUrl(projectId,
                              branch)
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
        def apiMajorVersion = apiSpec.apiMajorVersion
        def majorVersionNumber = getMajorVersionNumber(apiMajorVersion)
        def parsedAppVersion = parseVersion(appVersion)
        if (parsedAppVersion.majorVersion != majorVersionNumber) {
            def newAppVersion = new Version(majorVersionNumber,
                                            parsedAppVersion.minorVersion,
                                            parsedAppVersion.patchLevel,
                    parsedAppVersion.qualifier, null, null).toString()
            logger.println "Since app is supporting multiple API specs and this push is for ${apiMajorVersion}, using ${newAppVersion} instead of ${appVersion} because Exchange will reject otherwise."
            appVersion = newAppVersion
        }
        def branchName = apiSpec.designCenterBranchName
        def requestPayload = [
                main      : apiSpec.mainRamlFile,
                apiVersion: apiMajorVersion,
                version   : appVersion,
                assetId   : apiSpec.exchangeAssetId,
                name      : apiSpec.name,
                groupId   : clientWrapper.anypointOrganizationId,
                classifier: 'raml',
                metadata  : [
                        branchId : branchName,
                        projectId: projectId
                ]
        ]
        def requestJson = JsonOutput.toJson(requestPayload)
        logger.println "Pushing to Exchange with payload ${JsonOutput.prettyPrint(requestJson)}"
        def request = new HttpPost("${getBranchUrl(projectId, branchName)}/publish/exchange").with {
            setEntity(new StringEntity(requestJson,
                                       ContentType.APPLICATION_JSON))
            it
        }
        executeDesignCenterRequest(request,
                                   'Publishing to Exchange')
    }

    def synchronizeDesignCenterFromApp(ApiSpecification apiSpec,
                                       FileBasedAppDeploymentRequest deploymentRequest) {
        // For now this library ignores Exchange modules/does not try and maintain or sync them
        // both the app and the remote/DC comparison should therefore exclude them
        def ramlFilesFromApp = deploymentRequest.getRamlFilesFromApp(apiSpec.sourceDirectory,
                                                               true)
        synchronizeDesignCenter(apiSpec,
                                ramlFilesFromApp,
                                deploymentRequest.appVersion)
    }

    def synchronizeDesignCenter(ApiSpecification apiSpec,
                                List<RamlFile> ramlFiles,
                                String appVersion) {
        logger.println "Using directory ${apiSpec.sourceDirectory} inside app JAR"
        if (ramlFiles.empty) {
            logger.println 'No RAML files in project, therefore nothing to sync'
            return
        }
        def projectId = getDesignCenterProjectId(apiSpec.name)
        def branchName = apiSpec.designCenterBranchName
        def withLock = { Closure closure ->
            new DesignCenterLock(clientWrapper,
                                 logger,
                                 projectId,
                                 branchName).withCloseable {
                closure()
            }
        }
        def existingFiles = getExistingDesignCenterFiles(projectId,
                                                         branchName)
        def changes = ramlFiles - existingFiles
        def noLongerExist = existingFiles.findAll { file ->
            !ramlFiles.any { toBeFile -> file.fileName == toBeFile.fileName }
        }
        if (changes.empty && noLongerExist.empty) {
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
            withLock {
                if (noLongerExist.any()) {
                    deleteDesignCenterFiles(projectId,
                                            branchName,
                                            noLongerExist)
                } else {
                    logger.println('No existing files to delete')
                }
                if (changes.any()) {
                    uploadDesignCenterFiles(projectId,
                                            branchName,
                                            changes)
                }
                pushToExchange(apiSpec,
                               projectId,
                               appVersion)
            }
        }
    }
}
