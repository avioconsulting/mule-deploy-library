package com.avioconsulting.mule.deployment.api.models.deployment

import com.avioconsulting.mule.deployment.api.models.Features
import com.avioconsulting.mule.deployment.internal.models.RamlFile

abstract class AppDeploymentRequest {

    protected final Map<String, String> autoDiscoveries = [:]

    /**
     * environment name (e.g. DEV, not GUID)
     */
    protected String environment
    /**
     * TODO change this
     * Actual name of your application WITHOUT any kind of customer/environment prefix or suffix. Spaces in the name are not allowed and will be rejected.
     * This parameter is optional. If you don't supply it, the <artifactId> from your app's POM will be used.
     */
    protected ApplicationName applicationName
    /**
     * Version of the app you are deploying (e.g. <version> from the POM). This parameter is optional and if it's not supplied
     * then it will be derived from the <version> parameter in the project's POM based on the JAR/ZIP
     */
    protected String appVersion

    AppDeploymentRequest(ApplicationName applicationName, String appVersion, String environment) {
        this.applicationName = applicationName
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

    protected void setAppName(ApplicationName appName) {
        this.applicationName = appName
    }

    protected void setAppVersion(String appVersion) {
        this.appVersion = appVersion
    }

    String getEnvironment() {
        return environment
    }

    ApplicationName getAppName() {
        return this.applicationName
    }

    String getAppVersion() {
        return appVersion
    }

    /**
     * This method returns the list of features that is still not supported for the deployment type. This list
     * will excluded from execution in {@link com.avioconsulting.mule.deployment.api.Deployer} class
     * @return List of {@link Features}
     */
    List<Features> getUnsupportedFeatures() {
        return Collections.emptyList()
    }

    abstract List<RamlFile> getRamlFilesFromApp(String rootRamlDirectory, boolean ignoreExchange)
}
