package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import com.avioconsulting.jenkins.mule.impl.models.AppFileInfo
import com.avioconsulting.jenkins.mule.impl.models.RamlFile
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry

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
        def apiDirectory = new File('api').toPath()
        try {
            ZipArchiveEntry inputEntry
            List<RamlFile> results = []
            while ((inputEntry = archiveIn.nextEntry as ZipArchiveEntry) != null) {
                def inputEntryFile = new File(inputEntry.name)
                def relativeToApiDirectory = apiDirectory.relativize(inputEntryFile.toPath())
                inputEntryFile = relativeToApiDirectory.toFile()
                if (!inputEntry.directory &&
                        !IGNORE_DC_FILES.contains(inputEntryFile.name) &&
                        (!inputEntryFile.parentFile || !IGNORE_DC_FILES.contains(inputEntryFile.parentFile.name))) {
                    def nonWindowsPath = inputEntryFile.toString()
                            .replace(File.separator,
                                     '/') // Design center will always use this syntax
                    results << new RamlFile(nonWindowsPath,
                                            'foobar')
                }
            }
            results
        } finally {
            archiveIn.close()
        }
    }
}
