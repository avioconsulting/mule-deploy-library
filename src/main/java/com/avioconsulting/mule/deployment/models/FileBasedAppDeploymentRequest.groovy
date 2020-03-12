package com.avioconsulting.mule.deployment.models

import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory
import org.apache.commons.compress.archivers.zip.ZipArchiveEntry
import org.apache.commons.io.IOUtils

abstract class FileBasedAppDeploymentRequest {
    boolean isMule4Request() {
        isMule4Request(file)
    }

    static boolean isMule4Request(File file) {
        file.name.endsWith('.jar')
    }

    abstract File getFile()

    ArchiveInputStream openArchiveStream() {
        openArchiveStream(archiveFormat,
                          file)
    }

    static ArchiveInputStream openArchiveStream(String archiveFormat,
                                                File file) {
        def factory = new ArchiveStreamFactory()
        factory.createArchiveInputStream(archiveFormat,
                                         new FileInputStream(file))
    }

    String getArchiveFormat() {
        getArchiveFormat(mule4Request)
    }

    static String getArchiveFormat(boolean mule4Request) {
        // small semantic difference between JAR and ZIP and on-prem/Mule 4 Runtime Manager will
        // complain if it's not set right
        mule4Request ? ArchiveStreamFactory.JAR : ArchiveStreamFactory.ZIP
    }

    static File modifyFileProps(String propertiesFileToAddTo,
                                Map<String, String> propertiesToAdd,
                                File file) {
        if (propertiesToAdd.isEmpty()) {
            return file
        }
        def isMule4 = isMule4Request(file)
        // Mule 4 props files live at the root of the JAR. Mule 3's are in a classes subdirectory
        propertiesFileToAddTo = isMule4 ? propertiesFileToAddTo : "classes/${propertiesFileToAddTo}"
        def archiveFormat = getArchiveFormat(isMule4)
        def archiveIn = openArchiveStream(archiveFormat,
                                          file)
        def newFile = new File(file.toString() + '.updated')
        def fos = new FileOutputStream(newFile)
        def factory = new ArchiveStreamFactory()
        def archiveOut = factory.createArchiveOutputStream(archiveFormat,
                                                           fos)
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
        return newFile
    }

    static InputStream modifyProperties(InputStream input,
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
