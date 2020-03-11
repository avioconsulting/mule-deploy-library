package com.avioconsulting.mule.deployment.models

import groovy.transform.Canonical
import org.apache.commons.compress.archivers.ArchiveInputStream
import org.apache.commons.compress.archivers.ArchiveStreamFactory

@Canonical
class AppFileInfo {
    /**
     * The filename to display in the Runtime Manager app GUI. Often used as a version for a label
     */
    final String fileName
    /**
     * Stream of the ZIP/JAR containing the application to deploy
     */
    final InputStream app

    boolean isMule4Request() {
        fileName.endsWith('.jar')
    }

    String getArchiveFormat() {
        // small semantic difference between JAR and ZIP and on-prem/Mule 4 Runtime Manager will
        // complain if it's not set right
        mule4Request ? ArchiveStreamFactory.JAR : ArchiveStreamFactory.ZIP
    }

    ArchiveInputStream openArchiveStream() {
        def factory = new ArchiveStreamFactory()
        factory.createArchiveInputStream(archiveFormat,
                                         app)
    }
}
