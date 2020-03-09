package com.avioconsulting.jenkins.mule.impl.models

import groovy.transform.Canonical

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
}
