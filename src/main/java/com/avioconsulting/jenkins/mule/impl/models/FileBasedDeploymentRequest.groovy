package com.avioconsulting.jenkins.mule.impl.models

interface FileBasedDeploymentRequest {
    /**
     * Stream of the ZIP/JAR containing the application to deploy
     */
    InputStream getApp()
    /**
     * The filename to display in the Runtime Manager app GUI. Often used as a version for a label
     */
    String getFileName()

    boolean isMule4File(String zipFileName)
}
