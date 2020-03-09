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

    boolean isMule4Request()

    /**
     * VERY rare. If you have a weird situation where you need to be able to say that you "froze" an app ZIP/JAR for config management purposes and you want to change the properties inside a ZIP file, set this to the filename you want to drop new properties in inside the ZIP (e.g. api.dev.properties)
     */
    String getOverrideByChangingFileInZip()
}
