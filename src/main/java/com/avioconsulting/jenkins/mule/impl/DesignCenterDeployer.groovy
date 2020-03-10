package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.httpapi.LazyHeader
import com.avioconsulting.jenkins.mule.impl.models.AppFileInfo
import com.avioconsulting.jenkins.mule.impl.models.RamlFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpGet

import java.nio.charset.Charset

class DesignCenterDeployer {
    private final HttpClientWrapper clientWrapper
    private final PrintStream logger
    private static final List<String> IGNORE_DC_FILES = [
            'exchange_modules', // we don't deal with Exchange dependencies
            '.gitignore',
            'exchange.json', // see above
            '.designer.json'
    ]

    DesignCenterDeployer(HttpClientWrapper clientWrapper,
                         PrintStream logger) {

        this.logger = logger
        this.clientWrapper = clientWrapper
    }

    String getDesignCenterProjectId(String projectName) {
        logger.println "Looking up ID for Design Center project '${projectName}'"
        def request = new HttpGet("${clientWrapper.baseUrl}/designcenter/api-designer/projects").with {
            setHeader('X-ORGANIZATION-ID',
                      clientWrapper.anypointOrganizationId)
            setHeader('cache-control',
                      'no-cache')
            // At the time we run this, we might not yet have the ownerGuid value since that happens during authentication
            setHeader(new LazyHeader('X-OWNER-ID',
                                     {
                                         clientWrapper.ownerGuid
                                     }))
            it
        }
        def failureContext = "fetch design center project ID for '${projectName}'"
        clientWrapper.executeWithSuccessfulCloseableResponse(request,
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

    List<RamlFile> getRamlFilesFromApp(AppFileInfo deploymentRequest) {
        def archiveIn = deploymentRequest.openArchiveStream()
        def apiDirectoryPath = new File('api').toPath()
        try {
            ZipArchiveEntry inputEntry
            List<RamlFile> results = []
            while ((inputEntry = archiveIn.nextEntry as ZipArchiveEntry) != null) {
                if (inputEntry.directory) {
                    continue
                }
                def inputEntryFile = new File(inputEntry.name)
                def inputEntryPath = inputEntryFile.toPath()
                if (!inputEntryPath.startsWith(apiDirectoryPath)) {
                    continue
                }
                // design center won't care about API directories
                def relativeToApiDirectory = apiDirectoryPath.relativize(inputEntryPath)
                inputEntryFile = relativeToApiDirectory.toFile()
                def parentFile = inputEntryFile.parentFile
                if (!IGNORE_DC_FILES.contains(inputEntryFile.name) && !IGNORE_DC_FILES.contains(parentFile?.name)) {
                    def nonWindowsPath = inputEntryFile
                            .toString()
                            .replace(File.separator,
                                     '/')
                    // Design center will always use this syntax even if we're running this code on Windows
                    results << new RamlFile(nonWindowsPath,
                                            IOUtils.toString(archiveIn,
                                                             Charset.defaultCharset()))
                }
            }
            return results
        } finally {
            archiveIn.close()
        }
    }
}
