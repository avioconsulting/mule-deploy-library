package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.models.AppFileInfo
import com.avioconsulting.jenkins.mule.impl.models.RamlFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.IOUtils

import java.nio.charset.Charset

class DesignCenterDeployer {
    private final HttpClientWrapper clientWrapper
    private final PrintStream logger
    private static final List<String> IGNORE_DC_FILES = [
            'exchange_modules',
            '.gitignore',
            'exchange.json',
            '.designer.json'
    ]

    DesignCenterDeployer(HttpClientWrapper clientWrapper,
                         PrintStream logger) {

        this.logger = logger
        this.clientWrapper = clientWrapper
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
                if (!inputEntryFile.toPath().startsWith(apiDirectoryPath)) {
                    continue
                }
                def relativeToApiDirectory = apiDirectoryPath.relativize(inputEntryFile.toPath())
                inputEntryFile = relativeToApiDirectory.toFile()
                if (!IGNORE_DC_FILES.contains(inputEntryFile.name) &&
                        (!inputEntryFile.parentFile || !IGNORE_DC_FILES.contains(inputEntryFile.parentFile.name))) {
                    def nonWindowsPath = inputEntryFile.toString()
                            .replace(File.separator,
                                     '/') // Design center will always use this syntax
                    results << new RamlFile(nonWindowsPath,
                                            IOUtils.toString(archiveIn,
                                                             Charset.defaultCharset()))
                }
            }
            results
        } finally {
            archiveIn.close()
        }
    }
}
