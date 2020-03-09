package com.avioconsulting.jenkins.mule.impl.models

import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.IOUtils

trait MuleFileUtils {
    abstract String getFileName()
    abstract InputStream getApp()
    abstract String getOverrideByChangingFileInZip()
    abstract Map<String, String> getAppProperties()

    boolean isMule4Request() {
        getFileName().endsWith('.jar')
    }

    InputStream modifyZipFileWithNewProperties() {
        def propertiesToAdd = getAppProperties()
        if (propertiesToAdd.isEmpty()) {
            return getApp()
        }
        def propertiesFileToAddTo = getOverrideByChangingFileInZip()
        def isMule4 = isMule4Request()
        // Mule 4 props files live at the root of the JAR. Mule 3's are in a classes subdirectory
        propertiesFileToAddTo = isMule4 ? propertiesFileToAddTo : "classes/${propertiesFileToAddTo}"
        def factory = new ArchiveStreamFactory()
        // small semantic difference between JAR and ZIP and on-prem/Mule 4 Runtime Manager will
        // complain if it's not set right
        def format = isMule4 ? ArchiveStreamFactory.JAR : ArchiveStreamFactory.ZIP
        def archiveIn = factory.createArchiveInputStream(format,
                                                         getApp())
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
                    try {
                        if (!inputEntry.isDirectory()) {
                            if (inputEntry.name.endsWith('.properties')) {
                                propertiesFilesFound << inputEntry.name
                            }
                            if (inputEntry.name == propertiesFileToAddTo) {
                                found = true
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
                    }
                    finally {
                        archiveOut.closeArchiveEntry()
                    }
                }
            }
            finally {
                archiveIn.close()
                archiveOut.finish()
                archiveOut.close()
            }
            if (!found) {
                throw new Exception("ERROR: Expected to find the properties file you wanted to modify, ${propertiesFileToAddTo}, in the ZIP archive, but did not! Only files seen were ${propertiesFilesFound}.")
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
        def bos = new ByteArrayOutputStream()
        props.store(bos,
                    'Modified by deployment process')
        new ByteArrayInputStream(bos.toByteArray())
    }
}
