package com.avioconsulting.jenkins.mule.impl.models

class OnPremDeploymentRequest {
    /**
     * environment name (e.g. DEV, not GUID)
     */
    final String environment
    final String appName, targetServerOrClusterName
    /**
     * The filename to display in the Runtime Manager app GUI. Often used as a version for a label
     */
    final String fileName
    /**
     * VERY rare. If you have a weird situation where you need to be able to say that you "froze" an app ZIP/JAR for config management purposes and you want to change the properties inside a ZIP file, set this to the filename you want to drop new properties in inside the ZIP (e.g. api.dev.properties)
     */
    final String overrideByChangingFileInZip
    /**
     * Mule app property overrides (the stuff in the properties tab)
     */
    final Map<String, String> appProperties
    /**
     * Stream of the ZIP/JAR containing the application to deploy
     */
    final InputStream app


    OnPremDeploymentRequest(String environment,
                            String appName,
                            String targetServerOrClusterName,
                            String fileName,
                            InputStream app,
                            Map<String, String> appProperties = [:]) {
        if (appName.contains(' ')) {
            throw new Exception("Runtime Manager does not like spaces in app names and you specified '${appName}'!")
        }
        this.environment = environment
        this.appName = appName
        this.targetServerOrClusterName = targetServerOrClusterName
        this.fileName = fileName
        this.appProperties = appProperties
        this.app = app
    }

    OnPremDeploymentRequest(String environment,
                            String appName,
                            String targetServerOrClusterName,
                            String fileName,
                            InputStream app,
                            Map<String, String> appProperties,
                            String overrideByChangingFileInZip) {
        this(environment,
             appName,
             targetServerOrClusterName,
             fileName,
             app,
             appProperties)
        this.overrideByChangingFileInZip = overrideByChangingFileInZip
    }
}
