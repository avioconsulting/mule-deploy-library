package com.avioconsulting.mule.deployment.api.models

abstract class AppDeploymentRequest {

    protected final Map<String, String> autoDiscoveries = [:]

    /**
     * environment name (e.g. DEV, not GUID)
     */
    protected String environment
    /**
     * Actual name of your application WITHOUT any kind of customer/environment prefix or suffix. Spaces in the name are not allowed and will be rejected.
     * This parameter is optional. If you don't supply it, the <artifactId> from your app's POM will be used.
     */
    protected String appName
    /**
     * Version of the app you are deploying (e.g. <version> from the POM). This parameter is optional and if it's not supplied
     * then it will be derived from the <version> parameter in the project's POM based on the JAR/ZIP
     */
    protected String appVersion

    AppDeploymentRequest(String appName, String appVersion, String environment) {
        this.appName = appName
        this.appVersion = appVersion
        this.environment = environment
    }

    def setAutoDiscoveryId(String propertyName,
                           String autoDiscoveryId) {
        this.autoDiscoveries[propertyName] = autoDiscoveryId
    }

    protected void setEnvironment(String environment) {
        this.environment = environment
    }

    protected void setAppName(String appName) {
        this.appName = appName
    }

    protected void setAppVersion(String appVersion) {
        this.appVersion = appVersion
    }

    String getEnvironment() {
        return environment
    }

    String getAppName() {
        return appName
    }

    String getAppVersion() {
        return appVersion
    }
}
