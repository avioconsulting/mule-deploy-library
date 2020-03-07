package com.avioconsulting.jenkins.mule.impl

import com.avioconsulting.jenkins.mule.impl.httpapi.EnvironmentLocator
import com.avioconsulting.jenkins.mule.impl.httpapi.HttpClientWrapper
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.IOUtils
import org.apache.http.client.methods.HttpUriRequest

abstract class BaseDeployer {
    protected final EnvironmentLocator environmentLocator
    protected final PrintStream logger
    protected final int retryIntervalInMs
    protected final int maxTries
    protected final HttpClientWrapper clientWrapper

    BaseDeployer(int retryIntervalInMs,
                 int maxTries,
                 PrintStream logger,
                 HttpClientWrapper clientWrapper,
                 EnvironmentLocator environmentLocator) {
        this.clientWrapper = clientWrapper
        this.logger = logger
        this.maxTries = maxTries
        this.retryIntervalInMs = retryIntervalInMs
        this.environmentLocator = environmentLocator
    }

    def addStandardStuff(HttpUriRequest request,
                         String environmentName) {
        def environmentId = environmentLocator.getEnvironmentId(environmentName)
        request.addHeader('X-ANYPNT-ENV-ID',
                          environmentId)
        request.addHeader('X-ANYPNT-ORG-ID',
                          clientWrapper.anypointOrganizationId)
    }

    InputStream modifyZipFileWithNewProperties(InputStream inputZipFile,
                                               String zipFileName,
                                               String propertiesFileToAddTo,
                                               Map propertiesToAdd) {
        if (propertiesToAdd.isEmpty()) {
            return inputZipFile
        }
        def isMule4 = zipFileName.endsWith('.jar')
        // Mule 4 props files live at the root of the JAR. Mule 3's are in a classes subdirectory
        propertiesFileToAddTo = isMule4 ? propertiesFileToAddTo : "classes/${propertiesFileToAddTo}"
        def factory = new ArchiveStreamFactory()
        // small semantic difference between JAR and ZIP and on-prem/Mule 4 Runtime Manager will
        // complain if it's not set right
        def format = isMule4 ? ArchiveStreamFactory.JAR : ArchiveStreamFactory.ZIP
        def archiveIn = factory.createArchiveInputStream(format,
                                                         inputZipFile)
        def pos = new PipedOutputStream()
        def archiveOut = factory.createArchiveOutputStream(format,
                                                           pos)
        Thread.start {
            ZipArchiveEntry inputEntry
            def found = false
            List<String> propertiesFilesFound = []
            try {
                while ((inputEntry = archiveIn.nextEntry as ZipArchiveEntry) != null) {
                    assert archiveIn.canReadEntryData(inputEntry)
                    archiveOut.putArchiveEntry(inputEntry)
                    if (!inputEntry.isDirectory()) {
                        if (inputEntry.name.endsWith('.properties')) {
                            propertiesFilesFound << inputEntry.name
                        }
                        if (inputEntry.name == propertiesFileToAddTo) {
                            found = true
                            logger.println "Modifying ${isMule4 ? 'Mule 4' : 'Mule 3'} properties file '${inputEntry.name}'"
                            def modifiedStream = modifyProperties(archiveIn,
                                                                  propertiesFileToAddTo,
                                                                  propertiesToAdd)
                            IOUtils.copy(modifiedStream,
                                         archiveOut)
                        } else {
                            IOUtils.copy(archiveIn,
                                         archiveOut)
                        }
                    }
                    archiveOut.closeArchiveEntry()
                }
            }
            finally {
                archiveIn.close()
                archiveOut.finish()
                archiveOut.close()
            }
            if (!found) {
                logger.println "ERROR: Expected to find the properties file you wanted to modify, ${propertiesFileToAddTo}, in the ZIP archive, but did not! Only files seen were ${propertiesFilesFound}."
            }
        }
        new PipedInputStream(pos)
    }

    InputStream modifyProperties(InputStream input,
                                 String propertiesFileToAddTo,
                                 Map propertiesToAdd) {
        def props = new Properties()
        props.load(input)
        props.putAll(propertiesToAdd)
        logger.println "Merged properties from ${propertiesFileToAddTo}: ${props}"
        def bos = new ByteArrayOutputStream()
        props.store(bos,
                    'Modified by deployment process')
        new ByteArrayInputStream(bos.toByteArray())
    }
}
