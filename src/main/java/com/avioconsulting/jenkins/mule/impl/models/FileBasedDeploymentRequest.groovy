package com.avioconsulting.jenkins.mule.impl.models

interface FileBasedDeploymentRequest {
    /**
     * Stream of the ZIP/JAR containing the application to deploy
     */
    InputStream getApp()
}
