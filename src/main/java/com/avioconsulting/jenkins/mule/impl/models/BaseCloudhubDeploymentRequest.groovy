package com.avioconsulting.jenkins.mule.impl.models

abstract class BaseCloudhubDeploymentRequest {
    /**
     * environment name (e.g. DEV, not GUID)
     */
    final String environment
    final String appName
    final CloudhubWorkerSpecRequest workerSpecRequest
    /**
     * The filename to display in the Runtime Manager app GUI. Often used as a version for a label
     */
    final String fileName
    /**
     * Will be set in the 'crypto.key' CloudHub property
     */
    final String cryptoKey
    /**
     * will be set in the anypoint.platform.client_id CloudHub property
     */
    final String anypointClientId
    /**
     * will be set in the anypoint.platform.client_secret CloudHub property
     */
    final String anypointClientSecret
    /**
     * Your "DNS prefix" for Cloudhub app uniqueness, usually a 3 letter customer ID
     */
    final String cloudHubAppPrefix
    /**
     * Mule app property overrides (the stuff in the properties tab)
     */
    final Map<String, String> appProperties
    /**
     * CloudHub level property overrides (e.g. region type stuff)
     */
    final Map<String, String> otherCloudHubProperties
    /**
     * VERY rare. If you have a weird situation where you need to be able to say that you "froze" an app ZIP/JAR for config management purposes and you want to change the properties inside a ZIP file, set this to the filename you want to drop new properties in inside the ZIP (e.g. api.dev.properties)
     */
    final String overrideByChangingFileInZip

    BaseCloudhubDeploymentRequest(String environment,
                                  String appName,
                                  CloudhubWorkerSpecRequest workerSpecRequest,
                                  String fileName,
                                  String cryptoKey,
                                  String anypointClientId,
                                  String anypointClientSecret,
                                  String cloudHubAppPrefix,
                                  Map<String, String> appProperties = [:],
                                  Map<String, String> otherCloudHubProperties = [:]) {
        this.environment = environment
        this.appName = appName
        this.workerSpecRequest = workerSpecRequest
        this.fileName = fileName
        this.cryptoKey = cryptoKey
        this.anypointClientId = anypointClientId
        this.anypointClientSecret = anypointClientSecret
        this.cloudHubAppPrefix = cloudHubAppPrefix
        this.appProperties = appProperties
        this.otherCloudHubProperties = otherCloudHubProperties
    }

    BaseCloudhubDeploymentRequest(String environment,
                                  String appName,
                                  CloudhubWorkerSpecRequest workerSpecRequest,
                                  String fileName,
                                  String cryptoKey,
                                  String anypointClientId,
                                  String anypointClientSecret,
                                  String cloudHubAppPrefix,
                                  Map<String, String> appProperties,
                                  String overrideByChangingFileInZip,
                                  Map<String, String> otherCloudHubProperties = [:]) {
        this(environment,
             appName,
             workerSpecRequest,
             fileName,
             cryptoKey,
             anypointClientId,
             anypointClientSecret,
             cloudHubAppPrefix,
             appProperties,
             otherCloudHubProperties)
        this.overrideByChangingFileInZip = overrideByChangingFileInZip
    }
}
