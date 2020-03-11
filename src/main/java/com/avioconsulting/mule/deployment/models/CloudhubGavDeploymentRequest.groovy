package com.avioconsulting.mule.deployment.models

import com.avioconsulting.mule.deployment.MuleUtil
import groovy.json.JsonOutput
import org.apache.http.HttpEntity
import org.apache.http.entity.ContentType
import org.apache.http.entity.StringEntity

class CloudhubGavDeploymentRequest extends BaseCloudhubDeploymentRequest {
    /**
     * Used to retrieve ZIP/JAR from Exchange
     */
    final String groupId
    /**
     * Used to retrieve ZIP/JAR from Exchange
     */
    final String version

    CloudhubGavDeploymentRequest(String groupId,
                                 String version,
                                 String environment,
                                 String appName,
                                 CloudhubWorkerSpecRequest workerSpecRequest,
                                 String cryptoKey,
                                 String anypointClientId,
                                 String anypointClientSecret,
                                 String cloudHubAppPrefix,
                                 Map<String, String> appProperties = [:],
                                 Map<String, String> otherCloudHubProperties = [:]) {
        super(environment,
              appName,
              workerSpecRequest,
              MuleUtil.getFileName(appName,
                                   version,
                                   workerSpecRequest.muleVersion),
              cryptoKey,
              anypointClientId,
              anypointClientSecret,
              cloudHubAppPrefix,
              appProperties,
              otherCloudHubProperties)
        this.groupId = groupId
        this.version = version
    }

    CloudhubGavDeploymentRequest(String groupId,
                                 String version,
                                 String environment,
                                 String appName,
                                 CloudhubWorkerSpecRequest workerSpecRequest,
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
              MuleUtil.getFileName(appName,
                                   version,
                                   workerSpecRequest.muleVersion),
              cryptoKey,
              anypointClientId,
              anypointClientSecret,
              cloudHubAppPrefix,
              appProperties,
              overrideByChangingFileInZip,
              otherCloudHubProperties)
        this.groupId = groupId
        this.version = version
    }

    @Override
    Map<String, String> getCloudhubAppInfo() {
        return super.getCloudhubAppInfo() + [
                fileName: fileName
        ]
    }

    @Override
    HttpEntity getHttpPayload() {
        def appSource = [
                groupId   : groupId,
                artifactId: appName,
                version   : version,
                source    : 'EXCHANGE'
        ]
        def payload = [
                applicationSource: appSource,
                applicationInfo  : cloudhubAppInfo,
                autoStart        : true
        ]
        def payloadJson = JsonOutput.toJson(payload)
        new StringEntity(payloadJson,
                         ContentType.APPLICATION_JSON)
    }
}
