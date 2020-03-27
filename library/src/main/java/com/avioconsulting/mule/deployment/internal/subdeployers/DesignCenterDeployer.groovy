package com.avioconsulting.mule.deployment.internal.subdeployers

import com.avioconsulting.mule.deployment.api.DryRunMode
import com.avioconsulting.mule.deployment.api.ILogger
import com.avioconsulting.mule.deployment.internal.http.HttpClientWrapper

import com.avioconsulting.mule.deployment.api.models.ApiSpecification
import com.avioconsulting.mule.deployment.api.models.FileBasedAppDeploymentRequest
import com.avioconsulting.mule.deployment.internal.models.RamlFile
import com.avioconsulting.mule.deployment.internal.subdeployers.DesignCenterHttpFunctionality
import com.avioconsulting.mule.deployment.internal.subdeployers.DesignCenterLock
import groovy.json.JsonOutput
import groovy.json.JsonSlurper
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpDelete
import org.apache.http.client.methods.HttpGet
import org.apache.http.client.methods.HttpPost
import org.apache.http.client.methods.HttpUriRequest
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

import java.nio.charset.Charset
import java.nio.file.FileSystems
import java.nio.file.Files

class DesignCenterDeployer implements DesignCenterHttpFunctionality, IDesignCenterDeployer {
    private final HttpClientWrapper clientWrapper
    private final ILogger logger
    private static final List<String> IGNORE_DC_FILES = [
            'exchange_modules', // we don't deal with Exchange dependencies
            '.gitignore',
            'exchange.json', // see above
            '.designer.json'
    ]
    private final DryRunMode dryRunMode

    DesignCenterDeployer(HttpClientWrapper clientWrapper,
                         ILogger logger,
                         DryRunMode dryRunMode) {
        this.dryRunMode = dryRunMode
        this.logger = logger
        this.clientWrapper = clientWrapper
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
            executeDesignCenterRequest(new HttpDelete("${getFilesUrl(projectId)}/${file}"),
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
        logger.println('Fetching existing Design Center RAML files')
        def url = getFilesUrl(projectId)
        def request = new HttpGet(url)
        executeDesignCenterRequest(request,
                                   'Fetching project files') { List<Map> results ->
            def filesWeCareAbout = results.findAll { result ->
                def asFile = new File(result.path)
                result.type != 'FOLDER' &&
                        !IGNORE_DC_FILES.contains(asFile.name) &&
                        !IGNORE_DC_FILES.contains(asFile.parentFile?.name)
            }
            return filesWeCareAbout.collect { result ->
                def filePath = result.path
                def escapedForUrl = URLEncoder.encode(filePath)
                executeDesignCenterRequest(new HttpGet("${url}/${escapedForUrl}"),
                                           "Fetching file ${filePath}") { String contents ->
                    new RamlFile(filePath,
                                 contents)
                }
            }
        }
    }

    List<RamlFile> getRamlFilesFromApp(FileBasedAppDeploymentRequest deploymentRequest) {
        return FileSystems.newFileSystem(deploymentRequest.file.toPath(),
                                         null).withCloseable { fs ->
            def apiPath = fs.getPath('/api')
            if (!Files.exists(apiPath)) {
                return []
            }
            Files.walk(apiPath).findAll { p ->
                def relativeToApiDirectory = apiPath.relativize(p)
                !Files.isDirectory(p) &&
                        !IGNORE_DC_FILES.contains(relativeToApiDirectory.toString()) &&
                        !IGNORE_DC_FILES.contains(relativeToApiDirectory.parent.toString())
            }.collect { p ->
                def relativeToApiDirectory = apiPath.relativize(p)
                new RamlFile(relativeToApiDirectory.toString(),
                             p.text)
            }
        }
    }

    private static String getMainRamlFile(ApiSpecification apiSpec,
                                          List<RamlFile> ramlFiles) {
        apiSpec.mainRamlFile ?: ramlFiles.find { ramlFile ->
            new File(ramlFile.fileName).parentFile == null
        }.fileName
    }

    def pushToExchange(ApiSpecification apiSpec,
                       String projectId,
                       List<RamlFile> ramlFiles,
                       String appVersion) {
        def mainRamlFile = getMainRamlFile(apiSpec,
                                           ramlFiles)
        def requestPayload = [
                main      : mainRamlFile,
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
        def ramlFiles = getRamlFilesFromApp(appFileInfo)
        synchronizeDesignCenter(apiSpec,
                                ramlFiles,
                                appFileInfo.appVersion)
    }

    def synchronizeDesignCenter(ApiSpecification apiSpec,
                                List<RamlFile> ramlFiles,
                                String appVersion) {
        if (ramlFiles.empty) {
            logger.println 'No RAML files in project, therefore nothing to sync'
            return
        }
        def mainRamlFile = getMainRamlFile(apiSpec,
                                           ramlFiles)
        if (!ramlFiles.any { file -> file.fileName == mainRamlFile }) {
            throw new Exception("You specified '${mainRamlFile}' as your main RAML file but it does not exist in your application!")
        }
        def projectId = getDesignCenterProjectId(apiSpec.name)
        new DesignCenterLock(clientWrapper,
                             logger,
                             projectId).withCloseable {
            def existingFiles = getExistingDesignCenterFiles(projectId)
            def changes = ramlFiles - existingFiles
            if (changes.empty) {
                logger.println('New RAML contents match the old contents, will not update Design Center')
            } else {
                if (dryRunMode != DryRunMode.Run) {
                    logger.println('RAML quantity/contents have changed, WOULD update Design Center but in dry-run mode')
                    return
                }
                logger.println('RAML quantity/contents have changed, will update Design Center')
                def noLongerExist = existingFiles.findAll { file ->
                    !ramlFiles.any { toBeFile -> file.fileName == toBeFile.fileName }
                }
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
                               ramlFiles,
                               appVersion)
            }
        }
    }
}
