package com.avioconsulting.jenkins.mule.impl.models


import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.mime.HttpMultipartMode
import org.apache.http.entity.mime.MultipartEntityBuilder

class CloudhubFileDeploymentRequest extends BaseCloudhubDeploymentRequest implements FileBasedAppDeploymentRequest {
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
        def appFileInfo = new AppFileInfo(fileName,
                                          app)
        this.app = overrideByChangingFileInZip ? getPropertyModifiedStream(overrideByChangingFileInZip,
                                                                           appProperties,
                                                                           appFileInfo).app : app
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
        def appFileInfo = new AppFileInfo(fileName,
                                          app)
        this.app = overrideByChangingFileInZip ? getPropertyModifiedStream(overrideByChangingFileInZip,
                                                                           appProperties,
                                                                           appFileInfo).app : app
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
                               this.app,
                               ContentType.APPLICATION_OCTET_STREAM,
                               fileName)
                .build()
    }
}
