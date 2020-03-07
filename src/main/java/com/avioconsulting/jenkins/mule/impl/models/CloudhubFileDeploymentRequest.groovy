package com.avioconsulting.jenkins.mule.impl.models


import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class CloudhubFileDeploymentRequest extends BaseCloudhubDeploymentRequest {
    /**
     * Stream of the ZIP/JAR containing the application to deploy
     */
    InputStream app

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

    @Override
    HttpEntity getHttpPayload() {
        MultipartEntityBuilder.create()
                .setMode(HttpMultipartMode.BROWSER_COMPATIBLE)
        // without autoStart, the app won't actually start after we push this request out
                .addTextBody('autoStart',
                             'true')
                .addTextBody('appInfoJson',
                             cloudhubAppInfoAsJson)
                .addBinaryBody('file',
                               app,
                               ContentType.APPLICATION_OCTET_STREAM,
                               fileName)
                .build()
    }
}
