package com.avioconsulting.jenkins.mule.impl.models

class CloudhubFileDeploymentRequest extends BaseCloudhubDeploymentRequest {
    /**
     * Stream of the ZIP/JAR containing the application to deploy
     */
    final InputStream app

    CloudhubFileDeploymentRequest(InputStream app,
                                  String environment,
                                  String appName,
                                  CloudhubWorkerSpecRequest workerSpecRequest,
                                  String fileName,
                                  String cryptoKey,
                                  String anypointClientId,
                                  String anypointClientSecret,
                                  String cloudHubAppPrefix,
                                  Map<String, String> appProperties = [:],
                                  Map<String, String> otherCloudHubProperties = [:]) {
        super(environment,
              appName,
              workerSpecRequest,
              fileName,
              cryptoKey,
              anypointClientId,
              anypointClientSecret,
              cloudHubAppPrefix,
              appProperties,
              otherCloudHubProperties)
        this.app = app
    }

    CloudhubFileDeploymentRequest(InputStream app,
                                  String environment,
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
        super(environment,
              appName,
              workerSpecRequest,
              fileName,
              cryptoKey,
              anypointClientId,
              anypointClientSecret,
              cloudHubAppPrefix,
              appProperties,
              overrideByChangingFileInZip,
              otherCloudHubProperties)
        this.app = app
    }
}
